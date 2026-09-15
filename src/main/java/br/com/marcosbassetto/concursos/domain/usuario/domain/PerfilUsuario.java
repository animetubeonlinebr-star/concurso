package br.com.marcosbassetto.concursos.domain.usuario.domain;

public enum PerfilUsuario {
    USER("Usuário Comum"),
    ADMIN("Administrador");

    private final String descricao;

    PerfilUsuario(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
