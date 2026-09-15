package br.com.marcosbassetto.concursos.domain.questao.domain;

import lombok.Getter;

@Getter
public enum TipoQuestao {
    MULTIPLA_ESCOLHA("Múltipla Escolha"),
    CERTO_ERRADO("Certo ou Errado"),
    DISCURSIVA("Discursiva");

    private final String descricao;

    TipoQuestao(String descricao) {
        this.descricao = descricao;
    }
}
