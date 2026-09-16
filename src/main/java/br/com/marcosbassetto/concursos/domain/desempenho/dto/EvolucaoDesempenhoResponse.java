package br.com.marcosbassetto.concursos.domain.desempenho.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Série temporal de aproveitamento, agregada por dia de correção.
 */
public record EvolucaoDesempenhoResponse(
        List<PontoEvolucao> evolucao
) {

    public record PontoEvolucao(
            LocalDate data,
            long totalQuestoes,
            long questoesCorretas,
            double percentualAcerto
    ) {
    }
}