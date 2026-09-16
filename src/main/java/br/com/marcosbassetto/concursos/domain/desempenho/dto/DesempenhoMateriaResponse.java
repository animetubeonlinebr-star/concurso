package br.com.marcosbassetto.concursos.domain.desempenho.dto;

/**
 * Desempenho agregado por matéria, usado na quebra do dashboard por matéria.
 */
public record DesempenhoMateriaResponse(
        Long materiaId,
        String materiaNome,
        int totalQuestoes,
        int questoesCorretas,
        int questoesIncorretas,
        double percentualAcerto
) {
}