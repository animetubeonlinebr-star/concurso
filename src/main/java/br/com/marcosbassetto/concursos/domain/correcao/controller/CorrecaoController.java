package br.com.marcosbassetto.concursos.domain.correcao.controller;

import br.com.marcosbassetto.concursos.common.security.UsuarioAutenticadoResolver;
import br.com.marcosbassetto.concursos.domain.correcao.dto.CorrecaoResponse;
import br.com.marcosbassetto.concursos.domain.correcao.dto.ResultadoSimuladoResponse;
import br.com.marcosbassetto.concursos.domain.correcao.usecase.BuscarCorrecaoUseCase;
import br.com.marcosbassetto.concursos.domain.correcao.usecase.CorrigirSimuladoUseCase;
import br.com.marcosbassetto.concursos.domain.correcao.usecase.RecalcularCorrecaoUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints de correção.
 *
 * Não existe endpoint que aceite resultado ou pontuação do frontend: ambos são
 * sempre derivados do gabarito no backend.
 */
@RestController
@RequestMapping("/api/v1/simulados/{simuladoId}/correcao")
@RequiredArgsConstructor
public class CorrecaoController {

    private final CorrigirSimuladoUseCase corrigirSimuladoUseCase;
    private final BuscarCorrecaoUseCase buscarCorrecaoUseCase;
    private final RecalcularCorrecaoUseCase recalcularCorrecaoUseCase;
    private final UsuarioAutenticadoResolver usuarioAutenticadoResolver;

    @PostMapping
    public ResultadoSimuladoResponse corrigir(@PathVariable Long simuladoId, Authentication auth) {
        Long usuarioId = usuarioAutenticadoResolver.resolverUsuarioId(auth);
        return corrigirSimuladoUseCase.execute(usuarioId, simuladoId);
    }

    @GetMapping
    public ResultadoSimuladoResponse resultado(@PathVariable Long simuladoId, Authentication auth) {
        Long usuarioId = usuarioAutenticadoResolver.resolverUsuarioId(auth);
        return buscarCorrecaoUseCase.resultado(usuarioId, simuladoId);
    }

    @GetMapping("/questoes")
    public List<CorrecaoResponse> listarPorQuestao(@PathVariable Long simuladoId, Authentication auth) {
        Long usuarioId = usuarioAutenticadoResolver.resolverUsuarioId(auth);
        return buscarCorrecaoUseCase.porSimulado(usuarioId, simuladoId);
    }

    @GetMapping("/questoes/{simuladoQuestaoId}")
    public CorrecaoResponse porQuestao(@PathVariable Long simuladoId,
                                       @PathVariable Long simuladoQuestaoId,
                                       Authentication auth) {
        Long usuarioId = usuarioAutenticadoResolver.resolverUsuarioId(auth);
        return buscarCorrecaoUseCase.porQuestao(usuarioId, simuladoId, simuladoQuestaoId);
    }

    @PostMapping("/recalcular")
    public ResultadoSimuladoResponse recalcular(@PathVariable Long simuladoId, Authentication auth) {
        Long usuarioId = usuarioAutenticadoResolver.resolverUsuarioId(auth);
        return recalcularCorrecaoUseCase.execute(usuarioId, simuladoId);
    }
}