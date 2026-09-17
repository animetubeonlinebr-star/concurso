package br.com.marcosbassetto.concursos.domain.edital.interpretacao.perfil;

import br.com.marcosbassetto.concursos.domain.edital.interpretacao.classificacao.TipoUnidadeEdital;

import java.util.List;

/**
 * Como um edital organiza seus níveis. Editais diferentes usam níveis
 * diferentes, então o sistema não pode obrigar todos à mesma forma.
 *
 * <pre>
 * curso    -> MATERIA -> TOPICO
 * cargo    -> CONHECIMENTOS_GERAIS -> MATERIA -> TOPICO
 * area     -> MATERIA -> TOPICO
 * </pre>
 */
public record HierarchyDefinition(List<TipoUnidadeEdital> niveis) {

    public HierarchyDefinition {
        if (niveis == null || niveis.isEmpty()) {
            niveis = List.of(TipoUnidadeEdital.MATERIA, TipoUnidadeEdital.TOPICO);
        }
        niveis = List.copyOf(niveis);
    }

    /** Hierarquia mais comum: matéria contendo tópicos, sem agrupamento. */
    public static HierarchyDefinition simples() {
        return new HierarchyDefinition(
                List.of(TipoUnidadeEdital.MATERIA, TipoUnidadeEdital.TOPICO));
    }

    /** Edital com cargo/curso/área acima da matéria. */
    public static HierarchyDefinition comUnidade(TipoUnidadeEdital unidade) {
        return new HierarchyDefinition(List.of(
                unidade,
                TipoUnidadeEdital.MATERIA,
                TipoUnidadeEdital.TOPICO));
    }

    /** Edital que separa conhecimentos gerais e específicos. */
    public static HierarchyDefinition comConhecimentos() {
        return new HierarchyDefinition(List.of(
                TipoUnidadeEdital.CONHECIMENTOS_GERAIS,
                TipoUnidadeEdital.CONHECIMENTOS_ESPECIFICOS,
                TipoUnidadeEdital.MATERIA,
                TipoUnidadeEdital.TOPICO));
    }

    public boolean possuiNivel(TipoUnidadeEdital tipo) {
        return niveis.contains(tipo);
    }

    public TipoUnidadeEdital nivel(int indice) {
        return indice >= 0 && indice < niveis.size() ? niveis.get(indice) : null;
    }

    public int profundidade() {
        return niveis.size();
    }
}