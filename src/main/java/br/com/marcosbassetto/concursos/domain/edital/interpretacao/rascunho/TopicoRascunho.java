package br.com.marcosbassetto.concursos.domain.edital.interpretacao.rascunho;

import br.com.marcosbassetto.concursos.domain.edital.interpretacao.classificacao.TipoUnidadeEdital;

/**
 * Tópico em construção, antes de virar o DTO de resposta da API.
 *
 * Carrega o papel classificado junto do texto para que a auditoria consiga
 * explicar de onde cada tópico veio, sem reclassificar nada depois.
 */
public record TopicoRascunho(
        String nome,
        String codigo,
        TipoUnidadeEdital origem
) {

    public TopicoRascunho {
        if (origem == null) origem = TipoUnidadeEdital.TOPICO;
    }

    public static TopicoRascunho de(String nome) {
        return new TopicoRascunho(nome, null, TipoUnidadeEdital.TOPICO);
    }
}