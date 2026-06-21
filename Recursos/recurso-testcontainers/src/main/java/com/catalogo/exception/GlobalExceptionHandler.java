package com.catalogo.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.Instant;

/**
 * Manipulador global de exceções para capturar erros e responder em formato JSON padronizado.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Captura a exceção de produto não encontrado e retorna 404 (Not Found).
     */
    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<StandardError> handleProductNotFound(ProductNotFoundException e, HttpServletRequest request) {
        HttpStatus status = HttpStatus.NOT_FOUND;
        StandardError error = StandardError.builder()
                .timestamp(Instant.now())
                .status(status.value())
                .error("Recurso não encontrado")
                .message(e.getMessage())
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.status(status).body(error);
    }

    /**
     * Captura falhas de @Valid nos argumentos de requisições REST e retorna 422 (Unprocessable Entity).
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationError> handleValidation(MethodArgumentNotValidException e, HttpServletRequest request) {
        HttpStatus status = HttpStatus.valueOf(422);
        ValidationError error = new ValidationError(
                Instant.now(),
                status.value(),
                "Falha na validação de campos",
                "Existem dados incorretos no corpo da requisição.",
                request.getRequestURI()
        );

        for (FieldError fieldError : e.getBindingResult().getFieldErrors()) {
            error.addError(fieldError.getField(), fieldError.getDefaultMessage());
        }

        return ResponseEntity.status(status).body(error);
    }

    /**
     * Captura outras exceções genéricas inesperadas e retorna 500 (Internal Server Error).
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<StandardError> handleGenericException(Exception e, HttpServletRequest request) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        StandardError error = StandardError.builder()
                .timestamp(Instant.now())
                .status(status.value())
                .error("Erro interno do servidor")
                .message("Ocorreu um erro inesperado no sistema. Por favor, tente novamente mais tarde.")
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.status(status).body(error);
    }
}
