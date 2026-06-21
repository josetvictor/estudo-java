package com.catalogo.exception;

/**
 * Exceção lançada quando um produto não é encontrado pelo ID informado.
 */
public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(String message) {
        super(message);
    }
}
