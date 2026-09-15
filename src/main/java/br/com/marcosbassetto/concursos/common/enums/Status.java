package br.com.marcosbassetto.concursos.common.enums;

public enum Status {
    ATIVO("Ativo"),
    INATIVO("Inativo"),
    EXCLUIDO("Excluído");

    private final String descricao;

    Status(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}