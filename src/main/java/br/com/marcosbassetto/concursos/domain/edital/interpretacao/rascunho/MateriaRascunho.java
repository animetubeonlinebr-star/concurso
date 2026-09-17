package br.com.marcosbassetto.concursos.domain.edital.interpretacao.rascunho;

import java.util.List;

/**
 * Disciplina em construção.
 *
 * Diferente do DTO, guarda o caminho hierárquico completo (ex.:
 * {@code ["Técnico Judiciário", "Conhecimentos Específicos"]}) para que a
 * origem da matéria sobreviva até a gravação — o DTO achatado perderia essa
 * informação e impediria auditar de qual cargo a matéria veio.
 */
public record MateriaRascunho(
        String nome,
        List<String> caminho,
        List<TopicoRascunho> topicos
) {

    public MateriaRascunho {
        if (caminho == null) caminho = List.of();
        if (topicos == null) topicos = List.of();
        caminho = List.copyOf(caminho);
        topicos = List.copyOf(topicos);
    }

    public MateriaRascunho(String nome, List<TopicoRascunho> topicos) {
        this(nome, List.of(), topicos);
    }

    public boolean semTopicos() {
        return topicos.isEmpty();
    }
}