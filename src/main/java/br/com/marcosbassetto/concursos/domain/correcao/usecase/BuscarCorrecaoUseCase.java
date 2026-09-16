package br.com.marcosbassetto.concursos.domain.correcao.usecase;

import br.com.marcosbassetto.concursos.domain.correcao.dto.CorrecaoResponse;
import br.com.marcosbassetto.concursos.domain.correcao.dto.ResultadoSimuladoResponse;
import br.com.marcosbassetto.concursos.domain.correcao.service.CorrecaoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Caso de uso: consultar correções já realizadas.
 */
@Component
@RequiredArgsConstructor
public class BuscarCorrecaoUseCase {

    private final CorrecaoService correcaoService;

    public CorrecaoResponse porQuestao(Long usuarioId, Long simuladoId, Long simuladoQuestaoId) {
        return correcaoService.buscarPorQuestao(usuarioId, simuladoId, simuladoQuestaoId);
    }

    public List<CorrecaoResponse> porSimulado(Long usuarioId, Long simuladoId) {
        return correcaoService.listarPorSimulado(usuarioId, simuladoId);
    }

    public ResultadoSimuladoResponse resultado(Long usuarioId, Long simuladoId) {
        return correcaoService.consultarResultado(usuarioId, simuladoId);
    }
}