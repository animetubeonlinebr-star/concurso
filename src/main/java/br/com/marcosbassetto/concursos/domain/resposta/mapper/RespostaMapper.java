package br.com.marcosbassetto.concursos.domain.resposta.mapper;

import br.com.marcosbassetto.concursos.domain.resposta.dto.RespostaResponse;
import br.com.marcosbassetto.concursos.domain.resposta.entity.RespostaEntity;
import org.springframework.stereotype.Component;

@Component
public class RespostaMapper {

    public RespostaResponse toResponse(RespostaEntity resposta) {
        if (resposta == null) {
            return null;
        }

        return new RespostaResponse(
                resposta.getId(),
                resposta.getSimuladoQuestao() != null
                        ? resposta.getSimuladoQuestao().getSimulado().getId()
                        : null,
                resposta.getSimuladoQuestaoId(),
                resposta.getAlternativaSelecionada(),
                resposta.getRespostaTexto(),
                resposta.getRespondidaEm(),
                resposta.getAtualizadaEm()
        );
    }
}