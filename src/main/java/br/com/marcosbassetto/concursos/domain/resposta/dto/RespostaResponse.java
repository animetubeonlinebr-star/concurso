package br.com.marcosbassetto.concursos.domain.resposta.dto;

import java.time.LocalDateTime;

/**
 * Resposta exposta ao usuário. Não contém gabarito, resultado nem pontuação.
 */
public record RespostaResponse(
        Long id,
        Long simuladoId,
        Long simuladoQuestaoId,
        String alternativaSelecionada,
        String respostaTexto,
        LocalDateTime respondidaEm,
        LocalDateTime atualizadaEm
) {
}