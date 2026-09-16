package br.com.marcosbassetto.concursos.domain.questao.dto;

import br.com.marcosbassetto.concursos.domain.questao.domain.Alternativa;
import br.com.marcosbassetto.concursos.domain.questao.domain.TipoQuestao;

import java.util.List;

/**
 * Representação da questão durante a resolução do simulado.
 * O gabarito nunca é exposto aqui.
 */
public record QuestaoSimuladoResponse(
        Long id,
        Long materiaId,
        String materiaNome,
        Long topicoId,
        String topicoNome,
        String enunciado,
        TipoQuestao tipo,
        List<Alternativa> alternativas,
        String banca,
        Integer ano,
        String dificuldade
) {
}