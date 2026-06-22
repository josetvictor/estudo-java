package com.catalogo.controller;

import com.catalogo.BaseIntegrationTest;
import com.catalogo.dto.ProductRequestDTO;
import com.catalogo.dto.ProductResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cenário 1 — Persistência de Dados Válidos (Fluxo Feliz).
 *
 * <p>Valida que:
 * <ul>
 *   <li>Um POST com payload válido retorna 201 Created com o produto completo no corpo.</li>
 *   <li>O UUID e o campo {@code createdAt} são gerados automaticamente e não são nulos.</li>
 *   <li>O produto efetivamente existe no banco físico do contêiner (verificação direta no repositório).</li>
 *   <li>Um GET subsequente pelo ID gerado retorna os mesmos dados persistidos.</li>
 * </ul>
 */
@DisplayName("Cenário 1 - Persistência de Dados Válidos")
class ProductPersistenceIntegrationTest extends BaseIntegrationTest {

    @Test
    @DisplayName("Deve criar produto válido, retornar 201 e persistir no banco")
    void shouldCreateValidProductAndReturn201() {
        // Arrange
        ProductRequestDTO request = ProductRequestDTO.builder()
                .name("Teclado Mecânico")
                .description("Teclado RGB com switches Cherry MX Blue")
                .price(new BigDecimal("299.90"))
                .stockQuantity(50)
                .build();

        // Act — POST /api/products
        ResponseEntity<ProductResponseDTO> response = restTemplate.postForEntity(
                "/api/products", request, ProductResponseDTO.class);

        // Assert — status e corpo da resposta
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        ProductResponseDTO body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getId()).isNotNull().isInstanceOf(UUID.class);
        assertThat(body.getName()).isEqualTo("Teclado Mecânico");
        assertThat(body.getDescription()).isEqualTo("Teclado RGB com switches Cherry MX Blue");
        assertThat(body.getPrice()).isEqualByComparingTo("299.90");
        assertThat(body.getStockQuantity()).isEqualTo(50);
        assertThat(body.getCreatedAt()).isNotNull();

        UUID createdId = body.getId();

        // Assert — verificação direta no banco de dados do contêiner
        assertThat(productRepository.existsById(createdId)).isTrue();

        // Assert — GET /api/products/{id} retorna os mesmos dados
        ResponseEntity<ProductResponseDTO> getResponse = restTemplate.getForEntity(
                "/api/products/" + createdId, ProductResponseDTO.class);

        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        ProductResponseDTO fetched = getResponse.getBody();
        assertThat(fetched).isNotNull();
        assertThat(fetched.getId()).isEqualTo(createdId);
        assertThat(fetched.getName()).isEqualTo("Teclado Mecânico");
        assertThat(fetched.getPrice()).isEqualByComparingTo("299.90");
        assertThat(fetched.getCreatedAt()).isEqualTo(body.getCreatedAt());
    }

    @Test
    @DisplayName("Deve criar produto sem descrição e persistir corretamente")
    void shouldCreateProductWithoutDescriptionAndPersist() {
        // Arrange — descrição é opcional
        ProductRequestDTO request = ProductRequestDTO.builder()
                .name("Mouse Gamer")
                .price(new BigDecimal("149.00"))
                .stockQuantity(20)
                .build();

        // Act
        ResponseEntity<ProductResponseDTO> response = restTemplate.postForEntity(
                "/api/products", request, ProductResponseDTO.class);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getDescription()).isNull();
        assertThat(productRepository.count()).isEqualTo(1);
    }
}
