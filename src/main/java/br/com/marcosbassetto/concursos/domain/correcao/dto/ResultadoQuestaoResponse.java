package br.com.marcosbassetto.concursos.domain.correcao.dto;

import br.com.marcosbassetto.concursos.domain.correcao.domain.ResultadoCorrecao;

/**
 * Resultado de uma questão durante a resolução do simulado.
 * O gabarito não é exposto enquanto o simulado não estiver finalizado.
 */
public record ResultadoQuestaoResponse(
        Long simuladoQuestaoId,
        String respostaUsuario,
        ResultadoCorrecao resultado
) {
}