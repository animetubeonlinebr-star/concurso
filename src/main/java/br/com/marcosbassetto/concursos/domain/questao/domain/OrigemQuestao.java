package br.com.marcosbassetto.concursos.domain.questao.domain;

import lombok.Getter;

@Getter
public enum OrigemQuestao {
    BANCA("Questão oficial de banca"),
    IA("Questão gerada por Inteligência Artificial"),
    USUARIO("Questão cadastrada pelo usuário"),
    IMPORTADA("Questão importada de fonte externa");

    private final String descricao;

    OrigemQuestao(String descricao) {
        this.descricao = descricao;
    }
}
