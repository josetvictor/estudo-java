package com.catalogo.exception;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * DTO para representação padrão de respostas de erro da API.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StandardError {

    private Instant timestamp;
    private Integer status;
    private String error;
    private String message;
    private String path;
}
