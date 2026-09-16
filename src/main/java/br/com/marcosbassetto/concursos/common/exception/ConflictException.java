package br.com.marcosbassetto.concursos.common.exception;

/**
 * Conflito com o estado atual do recurso (HTTP 409).
 * Usada, por exemplo, ao tentar alterar dados de um simulado já finalizado.
 */
public class ConflictException extends BusinessException {

    public ConflictException(String code, String message) {
        super(code, message);
    }
}