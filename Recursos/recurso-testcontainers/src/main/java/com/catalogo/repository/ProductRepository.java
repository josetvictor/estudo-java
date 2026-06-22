package com.catalogo.repository;

import com.catalogo.model.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

/**
 * Repositório JPA para acesso aos dados da entidade Product.
 */
public interface ProductRepository extends JpaRepository<Product, UUID> {

    /**
     * Busca um produto pelo ID aplicando bloqueio pessimista (PESSIMISTIC_WRITE).
     * Utilizado em operações de modificação concorrente de estoque para evitar
     * race conditions — garante que apenas uma transação por vez leia e modifique
     * o registro enquanto outras transações aguardam a liberação do lock.
     *
     * @param id UUID do produto a ser buscado com lock.
     * @return Optional contendo o produto, se encontrado.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdWithLock(@Param("id") UUID id);
}


