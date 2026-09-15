package br.com.marcosbassetto.concursos.security.dto;

public record LoginResponse(
        String token,
        String tokenType,
        String nome,
        Long expiresIn
) {}