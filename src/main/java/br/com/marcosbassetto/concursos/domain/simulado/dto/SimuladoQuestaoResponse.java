package br.com.marcosbassetto.concursos.domain.simulado.dto;

import br.com.marcosbassetto.concursos.domain.questao.domain.Alternativa;
import br.com.marcosbassetto.concursos.domain.questao.domain.TipoQuestao;

import java.util.List;

/**
 * Questão apresentada durante a resolução do simulado.
 * O campo respostaCorreta nunca é incluído.
 */
public record SimuladoQuestaoResponse(
        Long id,
        Integer ordem,
        Long questaoId,
        String enunciado,
        TipoQuestao tipo,
        List<Alternativa> alternativas,
        String banca,
        Integer ano,
        String dificuldade,
        boolean respondida,
        String alternativaSelecionada,
        String respostaTexto
) {
}