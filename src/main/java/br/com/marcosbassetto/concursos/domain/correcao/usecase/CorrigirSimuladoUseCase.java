package br.com.marcosbassetto.concursos.domain.correcao.usecase;

import br.com.marcosbassetto.concursos.domain.correcao.dto.ResultadoSimuladoResponse;
import br.com.marcosbassetto.concursos.domain.correcao.service.CorrecaoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Caso de uso: corrigir um simulado finalizado.
 */
@Component
@RequiredArgsConstructor
public class CorrigirSimuladoUseCase {

    private final CorrecaoService correcaoService;

    public ResultadoSimuladoResponse execute(Long usuarioId, Long simuladoId) {
        return correcaoService.corrigirSimulado(usuarioId, simuladoId);
    }
}