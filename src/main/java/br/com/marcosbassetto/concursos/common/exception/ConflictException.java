package br.com.marcosbassetto.concursos.common.exception;

import lombok.Getter;

import java.util.Map;

/**
 * Conflito com o estado atual do recurso (HTTP 409).
 * Usada, por exemplo, ao tentar alterar dados de um simulado já finalizado.
 */
@Getter
public class ConflictException extends BusinessException {

    private final Map<String, Object> details;

    public ConflictException(String code, String message) {
        this(code, message, Map.of());
    }

    public ConflictException(String code, String message, Map<String, Object> details) {
        super(code, message);
        this.details = details != null ? details : Map.of();
    }
}
