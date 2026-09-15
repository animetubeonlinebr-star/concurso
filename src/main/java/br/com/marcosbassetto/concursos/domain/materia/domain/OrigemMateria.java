package br.com.marcosbassetto.concursos.domain.materia.domain;

public enum OrigemMateria {
    EDITAL("Identificada a partir do edital"),
    USUARIO("Cadastrada manualmente pelo usuário"),
    IA("Sugerida pela Inteligência Artificial"),
    IMPORTACAO("Importada de fonte externa");

    private final String descricao;

    OrigemMateria(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
