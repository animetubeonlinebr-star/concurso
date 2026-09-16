package br.com.marcosbassetto.concursos.domain.correcao.usecase;

import br.com.marcosbassetto.concursos.domain.correcao.dto.ResultadoSimuladoResponse;
import br.com.marcosbassetto.concursos.domain.correcao.service.CorrecaoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Caso de uso: recalcular a correção de um simulado.
 *
 * Uso controlado — previsto para correção de gabarito ou reprocessamento
 * administrativo, não para o fluxo normal de resolução.
 */
@Component
@RequiredArgsConstructor
public class RecalcularCorrecaoUseCase {

    private final CorrecaoService correcaoService;

    public ResultadoSimuladoResponse execute(Long usuarioId, Long simuladoId) {
        return correcaoService.recalcular(usuarioId, simuladoId);
    }
}