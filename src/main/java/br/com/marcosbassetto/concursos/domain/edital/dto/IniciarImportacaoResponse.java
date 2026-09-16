package br.com.marcosbassetto.concursos.domain.edital.dto;

import br.com.marcosbassetto.concursos.domain.edital.domain.StatusProcessamento;

/**
 * Resposta do upload de edital. {@code concursoId} é o que permite ao
 * frontend compor as rotas {@code /concursos/{id}/...}.
 */
public record IniciarImportacaoResponse(
        Long concursoId,
        Long importacaoId,
        StatusProcessamento status,
        boolean jaExistia
) {
}