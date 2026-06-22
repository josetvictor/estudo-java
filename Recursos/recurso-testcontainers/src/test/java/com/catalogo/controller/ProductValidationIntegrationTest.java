package com.catalogo.controller;

import com.catalogo.BaseIntegrationTest;
import com.catalogo.dto.ProductRequestDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cenário 2 — Comportamento sob Dados Inválidos (Validação e Restrições).
 *
 * <p>Valida que o {@code GlobalExceptionHandler} e as anotações de Bean Validation
 * ({@code @NotBlank}, {@code @DecimalMin}, {@code @Min}) estão configurados corretamente
 * para rejeitar payloads inválidos com {@code 422 Unprocessable Entity} e um corpo JSON
 * estruturado apontando o campo e a mensagem de erro exata.
 *
 * <p>Cada campo inválido é testado individualmente para garantir que a validação
 * identifica corretamente o campo problemático.
 */
@DisplayName("Cenário 2 - Validação de Dados Inválidos")
class ProductValidationIntegrationTest extends BaseIntegrationTest {

    private static final String PRODUCTS_URL = "/api/products";

    // -----------------------------------------------------------------------
    // Campo: name
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Deve retornar 422 quando nome for nulo")
    void shouldReturn422WhenNameIsNull() {
        ProductRequestDTO invalidDto = ProductRequestDTO.builder()
                .name(null)
                .description("Descrição qualquer")
                .price(new BigDecimal("99.90"))
                .stockQuantity(10)
                .build();

        ResponseEntity<Map> response = restTemplate.postForEntity(PRODUCTS_URL, invalidDto, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertValidationError(response.getBody(), "name", "O nome do produto não pode ser vazio.");
        assertThat(productRepository.count()).isZero();
    }

    @Test
    @DisplayName("Deve retornar 422 quando nome for vazio")
    void shouldReturn422WhenNameIsBlank() {
        ProductRequestDTO invalidDto = ProductRequestDTO.builder()
                .name("   ")
                .description("Descrição qualquer")
                .price(new BigDecimal("99.90"))
                .stockQuantity(10)
                .build();

        ResponseEntity<Map> response = restTemplate.postForEntity(PRODUCTS_URL, invalidDto, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertValidationError(response.getBody(), "name", "O nome do produto não pode ser vazio.");
        assertThat(productRepository.count()).isZero();
    }

    // -----------------------------------------------------------------------
    // Campo: price
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Deve retornar 422 quando preço for zero")
    void shouldReturn422WhenPriceIsZero() {
        ProductRequestDTO invalidDto = ProductRequestDTO.builder()
                .name("Teclado Mecânico")
                .price(BigDecimal.ZERO)
                .stockQuantity(15)
                .build();

        ResponseEntity<Map> response = restTemplate.postForEntity(PRODUCTS_URL, invalidDto, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertValidationError(response.getBody(), "price", "O preço do produto deve ser maior que zero.");
        assertThat(productRepository.count()).isZero();
    }

    @Test
    @DisplayName("Deve retornar 422 quando preço for negativo")
    void shouldReturn422WhenPriceIsNegative() {
        ProductRequestDTO invalidDto = ProductRequestDTO.builder()
                .name("Teclado Mecânico")
                .price(new BigDecimal("-10.00"))
                .stockQuantity(15)
                .build();

        ResponseEntity<Map> response = restTemplate.postForEntity(PRODUCTS_URL, invalidDto, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertValidationError(response.getBody(), "price", "O preço do produto deve ser maior que zero.");
        assertThat(productRepository.count()).isZero();
    }

    @Test
    @DisplayName("Deve retornar 422 quando preço for nulo")
    void shouldReturn422WhenPriceIsNull() {
        ProductRequestDTO invalidDto = ProductRequestDTO.builder()
                .name("Teclado Mecânico")
                .price(null)
                .stockQuantity(15)
                .build();

        ResponseEntity<Map> response = restTemplate.postForEntity(PRODUCTS_URL, invalidDto, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertValidationError(response.getBody(), "price", "O preço do produto é obrigatório.");
        assertThat(productRepository.count()).isZero();
    }

    // -----------------------------------------------------------------------
    // Campo: stockQuantity
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Deve retornar 422 quando estoque for negativo")
    void shouldReturn422WhenStockQuantityIsNegative() {
        ProductRequestDTO invalidDto = ProductRequestDTO.builder()
                .name("Teclado Mecânico")
                .price(new BigDecimal("99.90"))
                .stockQuantity(-5)
                .build();

        ResponseEntity<Map> response = restTemplate.postForEntity(PRODUCTS_URL, invalidDto, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertValidationError(response.getBody(), "stockQuantity", "A quantidade em estoque não pode ser negativa.");
        assertThat(productRepository.count()).isZero();
    }

    @Test
    @DisplayName("Deve retornar 422 quando estoque for nulo")
    void shouldReturn422WhenStockQuantityIsNull() {
        ProductRequestDTO invalidDto = ProductRequestDTO.builder()
                .name("Teclado Mecânico")
                .price(new BigDecimal("99.90"))
                .stockQuantity(null)
                .build();

        ResponseEntity<Map> response = restTemplate.postForEntity(PRODUCTS_URL, invalidDto, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertValidationError(response.getBody(), "stockQuantity", "A quantidade em estoque é obrigatória.");
        assertThat(productRepository.count()).isZero();
    }

    // -----------------------------------------------------------------------
    // Helper de asserção reutilizável
    // -----------------------------------------------------------------------

    /**
     * Verifica que o corpo de erro contém o campo e a mensagem de validação esperados.
     *
     * @param body      corpo da resposta deserializado como Map.
     * @param fieldName nome do campo que deve constar na lista de erros.
     * @param message   mensagem de validação esperada para o campo.
     */
    @SuppressWarnings("unchecked")
    private void assertValidationError(Map<String, Object> body, String fieldName, String message) {
        assertThat(body).isNotNull();
        assertThat(body.get("error")).isEqualTo("Falha na validação de campos");
        assertThat(body.get("status")).isEqualTo(422);

        List<Map<String, String>> errors = (List<Map<String, String>>) body.get("errors");
        assertThat(errors).isNotEmpty();

        boolean found = errors.stream()
                .anyMatch(e -> fieldName.equals(e.get("fieldName")) && message.equals(e.get("message")));
        assertThat(found)
                .as("Esperava erro de validação no campo '%s' com mensagem '%s', mas não encontrou. Erros: %s",
                        fieldName, message, errors)
                .isTrue();
    }
}
