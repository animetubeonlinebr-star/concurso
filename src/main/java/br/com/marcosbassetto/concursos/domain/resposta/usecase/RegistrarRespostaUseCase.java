package br.com.marcosbassetto.concursos.domain.resposta.usecase;

import br.com.marcosbassetto.concursos.domain.resposta.dto.RegistrarRespostaRequest;
import br.com.marcosbassetto.concursos.domain.resposta.dto.RespostaResponse;
import br.com.marcosbassetto.concursos.domain.resposta.service.RespostaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Caso de uso: registrar a resposta do usuário para uma questão do simulado.
 */
@Component
@RequiredArgsConstructor
public class RegistrarRespostaUseCase {

    private final RespostaService respostaService;

    public RespostaResponse execute(Long usuarioId, Long simuladoId, Long simuladoQuestaoId,
                                    RegistrarRespostaRequest request) {
        return respostaService.registrar(usuarioId, simuladoId, simuladoQuestaoId, request);
    }
}