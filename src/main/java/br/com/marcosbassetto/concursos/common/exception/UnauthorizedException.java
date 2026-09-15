package br.com.marcosbassetto.concursos.common.exception;

import lombok.Getter;

@Getter
public class UnauthorizedException extends RuntimeException {

    private final String code;

     private final String message;

    public UnauthorizedException(String code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    public UnauthorizedException(String message) {
        this("UNAUTHORIZED", message);
    }
}
