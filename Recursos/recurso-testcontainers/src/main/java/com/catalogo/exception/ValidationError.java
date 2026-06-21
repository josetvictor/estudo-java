package com.catalogo.exception;

import lombok.Getter;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Representação de erro que estende StandardError para incluir erros de validação de campos.
 */
@Getter
public class ValidationError extends StandardError {

    private final List<FieldMessage> errors = new ArrayList<>();

    public ValidationError(Instant timestamp, Integer status, String error, String message, String path) {
        super(timestamp, status, error, message, path);
    }

    public void addError(String fieldName, String message) {
        errors.add(new FieldMessage(fieldName, message));
    }
}
