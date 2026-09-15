package br.com.marcosbassetto.concursos.domain.questao.domain;

import lombok.Getter;

@Getter
public enum DificuldadeQuestao {
    FACIL("Fácil"),
    MEDIO("Médio"),
    DIFICIL("Difícil"),
    MUITO_DIFICIL("Muito Difícil");

    private final String descricao;

    DificuldadeQuestao(String descricao) {
        this.descricao = descricao;
    }
}
