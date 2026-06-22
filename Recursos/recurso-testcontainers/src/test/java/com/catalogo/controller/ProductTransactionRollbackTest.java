package com.catalogo.controller;

import com.catalogo.BaseIntegrationTest;
import com.catalogo.model.Product;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Cenário 4 — Rollback de Transação ({@code @Transactional} Behavior).
 *
 * <p>Valida a integridade transacional do Spring Data JPA com o driver PostgreSQL nativo:
 * quando uma {@link RuntimeException} é lançada dentro do escopo de uma transação
 * ativa, o Spring garante o rollback automático de todos os {@code save()} realizados
 * até aquele ponto naquela transação.
 *
 * <p><b>Por que usar {@code TransactionTemplate} e não uma chamada HTTP?</b><br>
 * Com {@code RANDOM_PORT}, as requisições HTTP são processadas em threads separadas
 * gerenciadas pelo servidor Tomcat embutido. Isso impossibilita que o JUnit controle
 * ou observe diretamente a transação via {@code @Transactional} na camada de teste.
 * O {@code TransactionTemplate} permite que o teste controle explicitamente o ciclo
 * de vida da transação dentro da mesma thread do teste, tornando o cenário determinístico.
 */
@DisplayName("Cenário 4 - Rollback de Transação")
class ProductTransactionRollbackTest extends BaseIntegrationTest {

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    @DisplayName("Deve realizar rollback e não persistir produto quando RuntimeException ocorrer na transação")
    void shouldRollbackAndNotPersistWhenRuntimeExceptionOccurs() {
        // Arrange
        Product product = Product.builder()
                .name("Produto Rollback Test")
                .description("Este produto não deve ser persistido")
                .price(new BigDecimal("99.90"))
                .stockQuantity(10)
                .createdAt(Instant.now())
                .build();

        TransactionTemplate template = new TransactionTemplate(transactionManager);

        // Act — salva o produto dentro da transação e lança RuntimeException antes do commit
        assertThatThrownBy(() -> template.execute(status -> {
            productRepository.save(product);

            // Confirma que o produto existe dentro do escopo da transação (antes do rollback)
            assertThat(productRepository.count()).isEqualTo(1);

            // Força o rollback lançando RuntimeException
            throw new RuntimeException("Forçando rollback: simulando falha após save()");
        }))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Forçando rollback");

        // Assert — o banco deve estar vazio: o rollback foi aplicado com sucesso
        assertThat(productRepository.count())
                .as("O produto não deve ter sido persistido após o rollback da transação")
                .isZero();
    }

    @Test
    @DisplayName("Deve persistir produto normalmente quando nenhuma exceção ocorrer na transação")
    void shouldPersistWhenNoExceptionOccursInTransaction() {
        // Arrange — cenário de controle: transação sem erros deve persistir normalmente
        Product product = Product.builder()
                .name("Produto Commit Test")
                .price(new BigDecimal("50.00"))
                .stockQuantity(5)
                .createdAt(Instant.now())
                .build();

        TransactionTemplate template = new TransactionTemplate(transactionManager);

        // Act — transação sem exceção → deve realizar o commit
        template.execute(status -> productRepository.save(product));

        // Assert — o produto deve estar persistido
        assertThat(productRepository.count())
                .as("O produto deve ter sido persistido após o commit bem-sucedido da transação")
                .isEqualTo(1);
    }
}
