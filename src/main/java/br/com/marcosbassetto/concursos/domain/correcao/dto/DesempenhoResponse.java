package br.com.marcosbassetto.concursos.domain.correcao.dto;

/**
 * Agregado de aproveitamento usado pelo dashboard.
 */
public record DesempenhoResponse(
        int totalQuestoes,
        int questoesRespondidas,
        int questoesCorretas,
        int questoesIncorretas,
        int questoesNaoRespondidas,
        double percentualAcerto,
        double pontuacao
) {
    public static DesempenhoResponse vazio() {
        return new DesempenhoResponse(0, 0, 0, 0, 0, 0.0, 0.0);
    }
}