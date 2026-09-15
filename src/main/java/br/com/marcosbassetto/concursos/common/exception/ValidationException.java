package br.com.marcosbassetto.concursos.common.exception;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public class ValidationException extends RuntimeException {

    private final Map<String, String> errors;

    public ValidationException(Map<String, String> errors) {
        super("Erros de validação encontrados");
        this.errors = errors != null ? errors : new HashMap<>();
    }

    public ValidationException(String field, String message) {
        super("Erro de validação");
        this.errors = new HashMap<>();
        this.errors.put(field, message);
    }

    public ValidationException(String message, Map<String, String> errors) {
        super(message);
        this.errors = errors != null ? errors : new HashMap<>();
    }

    public void addError(String field, String message) {
        this.errors.put(field, message);
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }
}
