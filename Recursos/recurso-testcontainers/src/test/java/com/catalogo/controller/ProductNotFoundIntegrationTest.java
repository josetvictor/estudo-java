package com.catalogo.controller;

import com.catalogo.BaseIntegrationTest;
import com.catalogo.dto.ProductRequestDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cenário 3 — Tratamento de Exceção para Recursos Inexistentes.
 *
 * <p>Valida que os endpoints {@code GET}, {@code PUT} e {@code DELETE} retornam
 * {@code 404 Not Found} com o corpo estruturado da classe {@link com.catalogo.exception.StandardError}
 * quando um UUID inexistente é fornecido.
 *
 * <p>O UUID utilizado nos testes é gerado aleatoriamente, mas como o banco é limpo
 * antes de cada teste ({@code @BeforeEach cleanDatabase()}), a inexistência é garantida.
 */
@DisplayName("Cenário 3 - Tratamento de Recursos Inexistentes")
class ProductNotFoundIntegrationTest extends BaseIntegrationTest {

    @Test
    @DisplayName("GET /api/products/{id} deve retornar 404 para ID inexistente")
    void shouldReturn404OnGetWithUnknownId() {
        UUID unknownId = UUID.randomUUID();

        ResponseEntity<Map> response = restTemplate.getForEntity(
                "/api/products/" + unknownId, Map.class);

        assertNotFoundResponse(response, unknownId, "Produto não encontrado com o ID: " + unknownId);
    }

    @Test
    @DisplayName("PUT /api/products/{id} deve retornar 404 para ID inexistente")
    void shouldReturn404OnPutWithUnknownId() {
        UUID unknownId = UUID.randomUUID();
        ProductRequestDTO updateRequest = ProductRequestDTO.builder()
                .name("Produto Atualizado")
                .price(new BigDecimal("50.00"))
                .stockQuantity(5)
                .build();

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/products/" + unknownId,
                HttpMethod.PUT,
                new HttpEntity<>(updateRequest),
                Map.class);

        assertNotFoundResponse(response, unknownId, "Produto não encontrado com o ID: " + unknownId);
    }

    @Test
    @DisplayName("DELETE /api/products/{id} deve retornar 404 para ID inexistente")
    void shouldReturn404OnDeleteWithUnknownId() {
        UUID unknownId = UUID.randomUUID();

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/products/" + unknownId,
                HttpMethod.DELETE,
                HttpEntity.EMPTY,
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        Map<String, Object> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("status")).isEqualTo(404);
        assertThat(body.get("error")).isEqualTo("Recurso não encontrado");
        assertThat(body.get("message").toString()).contains(unknownId.toString());
    }

    // -----------------------------------------------------------------------
    // Helper compartilhado para GET e PUT
    // -----------------------------------------------------------------------

    /**
     * Verifica que a resposta é 404 com o corpo estruturado do {@code StandardError}.
     *
     * @param response       resposta HTTP recebida.
     * @param unknownId      UUID fictício utilizado na requisição.
     * @param expectedMessage mensagem esperada no campo {@code message}.
     */
    private void assertNotFoundResponse(ResponseEntity<Map> response, UUID unknownId, String expectedMessage) {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        Map<String, Object> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("status")).isEqualTo(404);
        assertThat(body.get("error")).isEqualTo("Recurso não encontrado");
        assertThat(body.get("message")).isEqualTo(expectedMessage);
        assertThat(body.get("path").toString()).contains(unknownId.toString());
        assertThat(body.get("timestamp")).isNotNull();
    }
}
