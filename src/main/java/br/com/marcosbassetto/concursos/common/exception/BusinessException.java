package br.com.marcosbassetto.concursos.common.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {



    private final String code;

    private final String message;

    public BusinessException(String code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

     public BusinessException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.message = message;
    }

    public BusinessException(String message) {
        this("BUSINESS_ERROR", message);
    }
}