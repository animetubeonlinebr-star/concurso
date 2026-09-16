package br.com.marcosbassetto.concursos.domain.resposta.usecase;

import br.com.marcosbassetto.concursos.domain.resposta.dto.AtualizarRespostaRequest;
import br.com.marcosbassetto.concursos.domain.resposta.dto.RespostaResponse;
import br.com.marcosbassetto.concursos.domain.resposta.service.RespostaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Caso de uso: alterar uma resposta enquanto o simulado está em andamento.
 */
@Component
@RequiredArgsConstructor
public class AtualizarRespostaUseCase {

    private final RespostaService respostaService;

    public RespostaResponse execute(Long usuarioId, Long simuladoId, Long simuladoQuestaoId,
                                    AtualizarRespostaRequest request) {
        return respostaService.atualizar(usuarioId, simuladoId, simuladoQuestaoId, request);
    }
}