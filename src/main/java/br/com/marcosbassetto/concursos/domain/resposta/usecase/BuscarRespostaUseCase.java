package br.com.marcosbassetto.concursos.domain.resposta.usecase;

import br.com.marcosbassetto.concursos.domain.resposta.dto.RespostaResponse;
import br.com.marcosbassetto.concursos.domain.resposta.service.RespostaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Caso de uso: consultar a resposta atual de uma questão do simulado.
 * Retorna vazio quando o usuário ainda não respondeu.
 */
@Component
@RequiredArgsConstructor
public class BuscarRespostaUseCase {

    private final RespostaService respostaService;

    public Optional<RespostaResponse> execute(Long usuarioId, Long simuladoId, Long simuladoQuestaoId) {
        return respostaService.buscar(usuarioId, simuladoId, simuladoQuestaoId);
    }
}