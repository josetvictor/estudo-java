package com.catalogo.repository;

import com.catalogo.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Repositório JPA para acesso aos dados da entidade Product.
 */
public interface ProductRepository extends JpaRepository<Product, UUID> {
}

