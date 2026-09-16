package br.com.marcosbassetto.concursos.domain.edital.dto;

import br.com.marcosbassetto.concursos.domain.edital.domain.StatusProcessamento;

import java.util.List;

/**
 * Árvore consolidada do concurso, consumida pela tela
 * {@code /concursos/{id}/conteudo}.
 *
 * As contagens são calculadas no servidor: a tela só exibe, e um cliente
 * não precisa agregar listas inteiras para mostrar "12 matérias, 84 tópicos".
 */
public record ConteudoConcursoResponse(
        Long concursoId,
        String nome,
        StatusProcessamento statusProcessamento,
        ResumoConteudo resumo,
        List<MateriaConteudo> materias
) {

    public record ResumoConteudo(
            int totalMaterias,
            int totalTopicos,
            long totalQuestoes
    ) {
    }

    public record MateriaConteudo(
            Long id,
            String nome,
            Integer ordem,
            String origem,
            long totalTopicos,
            long totalQuestoes,
            List<TopicoConteudo> topicos
    ) {
    }

    public record TopicoConteudo(
            Long id,
            String nome,
            Integer ordem,
            Boolean ativo,
            long totalQuestoes
    ) {
    }
}
