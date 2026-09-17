package br.com.marcosbassetto.concursos.domain.edital.interpretacao.classificacao;

/**
 * Papel de um trecho do edital na hierarquia do documento.
 *
 * Existe porque uma linha em destaque no PDF não é necessariamente uma
 * matéria: cabeçalho de instituição, título de seção e nome de prova
 * aparecem com a mesma forma visual de uma disciplina. Sem este papel, o
 * extrator grava "UNIVERSIDADE ESTADUAL PAULISTA" como matéria.
 */
public enum TipoUnidadeEdital {

    /** Cabeçalho institucional repetido a cada página. */
    CABECALHO_DOCUMENTO,

    /** Título estrutural do documento ou de um bloco ("COMUNICADO", "ANEXO II"). */
    SECAO,

    /** Agrupamento de conteúdo ("CONHECIMENTOS GERAIS", "CONHECIMENTOS ESPECÍFICOS"). */
    CONHECIMENTOS_GERAIS,
    CONHECIMENTOS_ESPECIFICOS,

    /** Unidades de nível 1: cargo, curso, área, especialidade, eixo ou prova. */
    CARGO,
    CURSO,
    AREA,
    ESPECIALIDADE,
    EIXO,
    PROVA,

    /** Disciplina propriamente dita. É o único papel que vira matéria. */
    MATERIA,

    /** Item de conteúdo dentro de uma matéria. */
    TOPICO;

    /** Somente MATERIA é persistível como matéria. */
    public boolean ehMateria() {
        return this == MATERIA;
    }

    /** Papéis que descrevem o documento e não conteúdo programático. */
    public boolean ehEstrutural() {
        return this == CABECALHO_DOCUMENTO || this == SECAO;
    }

    public boolean ehAgrupamento() {
        return this == CONHECIMENTOS_GERAIS || this == CONHECIMENTOS_ESPECIFICOS;
    }

    public boolean ehUnidadeNivel1() {
        return switch (this) {
            case CARGO, CURSO, AREA, ESPECIALIDADE, EIXO, PROVA -> true;
            default -> false;
        };
    }
}