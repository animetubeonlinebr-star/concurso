package br.com.marcosbassetto.concursos.domain.edital.interpretacao.perfil;

import br.com.marcosbassetto.concursos.domain.edital.interpretacao.classificacao.TipoUnidadeEdital;

import java.util.List;

/**
 * Resposta à pergunta "que tipo de edital é este?".
 *
 * O perfil reúne as decisões que os extratores antes tomavam de forma
 * implícita e espalhada: que níveis o documento usa, como os blocos são
 * delimitados e como os itens de conteúdo são marcados.
 *
 * @param codigo           identificador estável do perfil, para log e auditoria
 * @param descricao        texto legível do perfil
 * @param hierarquia       níveis que o documento usa, do topo para a base
 * @param marcadoresSecao   marcadores de início/fim do bloco de conteúdo
 * @param marcadoresItem    marcadores de item de conteúdo na base
 * @param tratarCargoComoNivel se o nome do cargo introduz uma nova ramificação
 */
public record EditalProfile(
        String codigo,
        String descricao,
        HierarchyDefinition hierarquia,
        List<String> marcadoresSecao,
        List<String> marcadoresItem,
        boolean tratarCargoComoNivel
) {

    public EditalProfile {
        if (codigo == null || codigo.isBlank()) codigo = "GENERICO";
        if (descricao == null) descricao = "";
        if (hierarquia == null) hierarquia = HierarchyDefinition.simples();
        if (marcadoresSecao == null) marcadoresSecao = List.of();
        if (marcadoresItem == null) marcadoresItem = List.of();
        marcadoresSecao = List.copyOf(marcadoresSecao);
        marcadoresItem = List.copyOf(marcadoresItem);
    }

    /**
     * Edital organizado em blocos "CONTEÚDO PROGRAMÁTICO" com disciplinas em
     * destaque. É o formato mais comum e serve de base neutra.
     */
    public static EditalProfile generico() {
        return new EditalProfile(
                "GENERICO",
                "Documento organizado em blocos de conteúdo programático com disciplinas em destaque.",
                HierarchyDefinition.simples(),
                List.of("CONTEUDO PROGRAMATICO", "CONTEÚDO PROGRAMÁTICO", "ANEXO"),
                List.of("▪", "■", "◆", "•", "-", "I.", "II."),
                false);
    }

    /**
     * Documento que não é um edital de conteúdo programático: comunicado,
     * aviso, errata. Declarar isso evita inventar disciplinas a partir de
     * linhas soltas.
     */
    public static EditalProfile semConteudo() {
        return new EditalProfile(
                "SEM_CONTEUDO",
                "Documento sem seção de conteúdo programático.",
                HierarchyDefinition.simples(),
                List.of(),
                List.of(),
                false);
    }

    /** Edital cujo conteúdo é separado por cargo, curso ou área. */
    public static EditalProfile porUnidade(TipoUnidadeEdital unidade, String codigo) {
        return new EditalProfile(
                codigo,
                "Documento que separa o conteúdo por " + unidade + ".",
                HierarchyDefinition.comUnidade(unidade),
                List.of("CONTEUDO PROGRAMATICO", "CONTEÚDO PROGRAMÁTICO", "ANEXO"),
                List.of("▪", "■", "◆", "•", "-"),
                true);
    }

    /** Edital que divide o conteúdo em conhecimentos gerais e específicos. */
    public static EditalProfile porConhecimentos() {
        return new EditalProfile(
                "CONHECIMENTOS",
                "Documento que separa conhecimentos gerais de conhecimentos específicos.",
                HierarchyDefinition.comConhecimentos(),
                List.of("CONHECIMENTOS GERAIS", "CONHECIMENTOS ESPECÍFICOS", "CONTEÚDO PROGRAMÁTICO"),
                List.of("▪", "■", "◆", "•", "-"),
                false);
    }

    public boolean possuiBlocoConteudo() {
        return !marcadoresSecao.isEmpty();
    }
}