package com.catalogo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Entidade que representa um Produto no catálogo.
 */
@Entity
@Table(name = "tb_product")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank(message = "O nome do produto não pode ser vazio.")
    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @NotNull(message = "O preço do produto é obrigatório.")
    @DecimalMin(value = "0.01", message = "O preço do produto deve ser maior que zero.")
    @Column(nullable = false)
    private BigDecimal price;

    @NotNull(message = "A quantidade em estoque é obrigatória.")
    @Min(value = 0, message = "A quantidade em estoque não pode ser negativa.")
    @Column(nullable = false)
    private Integer stockQuantity;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
