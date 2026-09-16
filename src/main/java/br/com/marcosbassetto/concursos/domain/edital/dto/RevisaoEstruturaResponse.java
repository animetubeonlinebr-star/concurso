package br.com.marcosbassetto.concursos.domain.edital.dto;

import br.com.marcosbassetto.concursos.domain.edital.domain.StatusProcessamento;

import java.util.List;

/**
 * Estrutura extraída em revisão, com as flags que o usuário pode alterar
 * antes de confirmar.
 */
public record RevisaoEstruturaResponse(
        Long concursoId,
        Long importacaoId,
        StatusProcessamento status,
        DadosConcurso dadosConcurso,
        List<MateriaRevisaoResponse> materias,
        String mensagem
) {

    public record MateriaRevisaoResponse(
            Long id,
            String nome,
            Integer ordem,
            Boolean selecionada,
            Boolean possivelDuplicidade,
            Long similarAId,
            String similarA,
            Boolean jaExisteConfirmada,
            List<TopicoRevisaoResponse> topicos
    ) {
    }

    public record TopicoRevisaoResponse(
            Long id,
            String nome,
            Integer ordem,
            Boolean selecionado,
            Boolean possivelDuplicidade,
            Long similarAId,
            String similarA,
            Boolean jaExisteConfirmado
    ) {
    }
}