package br.com.marcosbassetto.concursos.domain.edital.domain;

/**
 * Ciclo de vida de uma importação de edital.
 *
 * Usado tanto em {@code edital_importacao} quanto em
 * {@code concurso.status_processamento} para que a tela
 * {@code /concursos/{id}/processando} possa acompanhar o andamento.
 */
public enum StatusProcessamento {

    RECEBIDO("Arquivo recebido, aguardando processamento"),
    PROCESSANDO("Extraindo matérias e tópicos do edital"),
    AGUARDANDO_REVISAO("Estrutura extraída, aguardando revisão do usuário"),
    CONFIRMADO("Estrutura confirmada e persistida"),
    ERRO("Falha no processamento");

    private final String descricao;

    StatusProcessamento(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}