package br.com.marcosbassetto.concursos.domain.correcao.dto;

import br.com.marcosbassetto.concursos.domain.correcao.domain.ResultadoCorrecao;

import java.time.LocalDateTime;

/**
 * Correção de uma questão. O gabarito é exposto aqui porque a consulta só é
 * permitida depois que o simulado está finalizado.
 */
public record CorrecaoResponse(
        Long id,
        Long simuladoId,
        Long simuladoQuestaoId,
        String respostaUsuario,
        String respostaCorreta,
        ResultadoCorrecao resultado,
        LocalDateTime corrigidaEm
) {
}