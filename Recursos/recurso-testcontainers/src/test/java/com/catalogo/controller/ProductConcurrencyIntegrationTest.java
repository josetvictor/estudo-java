package com.catalogo.controller;

import com.catalogo.BaseIntegrationTest;
import com.catalogo.dto.ProductRequestDTO;
import com.catalogo.dto.ProductResponseDTO;
import com.catalogo.service.ProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cenário 5 — Controle de Concorrência e Isolamento.
 *
 * <p>Valida o comportamento do banco de dados PostgreSQL real sob carga concorrente:
 * múltiplas threads tentam decrementar o estoque de um único produto simultaneamente.
 *
 * <p><b>Por que este teste só funciona com Testcontainers (PostgreSQL real)?</b><br>
 * Bancos em memória como H2 possuem comportamentos de locking simplificados que
 * mascaram race conditions. O PostgreSQL implementa MVCC (Multi-Version Concurrency
 * Control) com locking real, expondo o problema de corrida de forma fiel ao ambiente
 * de produção.
 *
 * <p><b>Pessimistic Locking ({@code PESSIMISTIC_WRITE})</b>:<br>
 * O método {@code decrementStock} do {@code ProductService} utiliza
 * {@code findByIdWithLock} do repositório, que aplica {@code SELECT FOR UPDATE}
 * no PostgreSQL. Isso garante que apenas UMA transação por vez leia e modifique
 * o estoque, serializando as operações e prevenindo o problema clássico de
 * <i>Lost Update</i> / <i>Race Condition</i>.
 *
 * <p><b>Como funcionaria SEM o lock</b>:<br>
 * Sem {@code PESSIMISTIC_WRITE}, múltiplas threads leem o valor atual do estoque
 * ao mesmo tempo (ex: todas leem 10), cada uma decrementa para 9 e salva 9.
 * O resultado final seria incorreto (ex: 9 em vez de 0), corrompendo o dado.
 */
@DisplayName("Cenário 5 - Controle de Concorrência com Pessimistic Locking")
class ProductConcurrencyIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private ProductService productService;

    private static final int THREAD_COUNT = 10;
    private static final int INITIAL_STOCK = 10;

    @Test
    @DisplayName("Deve decrementar estoque corretamente sob concorrência com Pessimistic Locking")
    void shouldDecrementStockCorrectlyUnderConcurrencyWithPessimisticLock() throws InterruptedException {
        // Arrange — persiste produto com 10 unidades de estoque
        ProductRequestDTO request = ProductRequestDTO.builder()
                .name("Produto Concorrência Test")
                .description("Produto para teste de race condition com estoque inicial de " + INITIAL_STOCK)
                .price(new BigDecimal("25.00"))
                .stockQuantity(INITIAL_STOCK)
                .build();

        ProductResponseDTO created = productService.createProduct(request);
        UUID productId = created.getId();

        // Arrange — latch para liberar todas as threads simultaneamente (simula pico de requisições)
        CountDownLatch startLatch = new CountDownLatch(1);
        // latch para aguardar todas as threads concluírem
        CountDownLatch doneLatch = new CountDownLatch(THREAD_COUNT);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        List<Future<?>> futures = new ArrayList<>();

        // Act — lança THREAD_COUNT threads que aguardam o sinal simultâneo
        for (int i = 0; i < THREAD_COUNT; i++) {
            futures.add(executor.submit(() -> {
                try {
                    // Todas as threads aguardam aqui, prontas para agir ao mesmo tempo
                    startLatch.await();
                    productService.decrementStock(productId, 1);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            }));
        }

        // Dispara todas as threads simultaneamente
        startLatch.countDown();

        // Aguarda todas as threads finalizarem (timeout de segurança: 30s)
        boolean allFinished = doneLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        // Assert — todas as threads finalizaram dentro do timeout
        assertThat(allFinished)
                .as("Todas as %d threads devem finalizar dentro do timeout", THREAD_COUNT)
                .isTrue();

        // Assert — todas as operações de decremento foram bem-sucedidas (sem erros)
        assertThat(errorCount.get())
                .as("Nenhuma thread deve ter lançado exceção com o lock pessimista ativo")
                .isZero();

        assertThat(successCount.get())
                .as("Todas as %d threads devem ter decrementado com sucesso", THREAD_COUNT)
                .isEqualTo(THREAD_COUNT);

        // Assert — estoque final deve ser exatamente 0: nenhum decremento foi perdido
        ProductResponseDTO finalProduct = productService.findProductById(productId);
        assertThat(finalProduct.getStockQuantity())
                .as("Estoque final deve ser 0: pessimistic lock preveniu race conditions. " +
                    "Sem lock, o resultado seria > 0 por Lost Update.")
                .isZero();
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar decrementar estoque além do disponível")
    void shouldThrowExceptionWhenDecrementingBeyondAvailableStock() {
        // Arrange — produto com estoque de 1
        ProductRequestDTO request = ProductRequestDTO.builder()
                .name("Produto Estoque Mínimo")
                .price(new BigDecimal("10.00"))
                .stockQuantity(1)
                .build();

        ProductResponseDTO created = productService.createProduct(request);
        UUID productId = created.getId();

        // Act — decrementa 1 (ok)
        productService.decrementStock(productId, 1);

        // Assert — tentar decrementar com estoque zerado deve lançar exceção
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalStateException.class,
                () -> productService.decrementStock(productId, 1),
                "Deve lançar IllegalStateException quando estoque é insuficiente"
        );

        // Assert — estoque permanece 0 (não ficou negativo)
        ProductResponseDTO finalProduct = productService.findProductById(productId);
        assertThat(finalProduct.getStockQuantity()).isZero();
    }
}
