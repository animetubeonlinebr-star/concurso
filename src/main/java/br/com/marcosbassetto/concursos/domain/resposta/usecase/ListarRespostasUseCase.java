package br.com.marcosbassetto.concursos.domain.resposta.usecase;

import br.com.marcosbassetto.concursos.domain.resposta.dto.RespostaResponse;
import br.com.marcosbassetto.concursos.domain.resposta.service.RespostaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Caso de uso: listar as respostas de um simulado do usuário autenticado.
 */
@Component
@RequiredArgsConstructor
public class ListarRespostasUseCase {

    private final RespostaService respostaService;

    public List<RespostaResponse> execute(Long usuarioId, Long simuladoId) {
        return respostaService.listarPorSimulado(usuarioId, simuladoId);
    }
}