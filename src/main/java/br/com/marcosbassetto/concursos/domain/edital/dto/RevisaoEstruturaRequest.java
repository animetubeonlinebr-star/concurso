package br.com.marcosbassetto.concursos.domain.edital.dto;

import java.util.List;

/**
 * Alterações da revisão enviadas pelo usuário. Ausência de um item na lista
 * {@code materias} significa remoção; {@code mesclarEmId} preenchido faz a
 * mesclagem ser explícita (nunca automática).
 */
public record RevisaoEstruturaRequest(
        List<MateriaRevisaoRequest> materias
) {

    public record MateriaRevisaoRequest(
            Long id,
            String nome,
            Boolean selecionada,
            Long mesclarEmId,
            Boolean remover,
            List<TopicoRevisaoRequest> topicos
    ) {
    }

    public record TopicoRevisaoRequest(
            Long id,
            String nome,
            Boolean selecionado,
            Long mesclarEmId,
            Boolean remover
    ) {
    }
}