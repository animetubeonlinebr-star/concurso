package br.com.marcosbassetto.concursos.domain.correcao.dto;

import java.time.LocalDateTime;

/**
 * Resultado consolidado de um simulado.
 *
 * Invariante: questoesCorretas + questoesIncorretas + questoesNaoRespondidas
 * deve sempre ser igual a totalQuestoes.
 */
public record ResultadoSimuladoResponse(
        Long simuladoId,
        int totalQuestoes,
        int questoesCorretas,
        int questoesIncorretas,
        int questoesNaoRespondidas,
        double percentualAcerto,
        double pontuacao,
        LocalDateTime corrigidoEm
) {
}