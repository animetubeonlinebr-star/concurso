package br.com.marcosbassetto.concursos.api.controller;

import br.com.marcosbassetto.concursos.application.edital.ConsultarProcessamentoUseCase;
import br.com.marcosbassetto.concursos.common.security.UsuarioAutenticadoResolver;
import br.com.marcosbassetto.concursos.domain.edital.domain.StatusProcessamento;
import br.com.marcosbassetto.concursos.domain.edital.dto.StatusProcessamentoResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Acompanhamento da importação do edital: alimenta o polling da tela
 * {@code /concursos/{id}/processando}.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/concursos/{id}")
@RequiredArgsConstructor
public class EditalImportacaoController {

    private final ConsultarProcessamentoUseCase consultarProcessamento;
    private final UsuarioAutenticadoResolver usuarioResolver;

    @GetMapping("/processamento")
    public ResponseEntity<StatusProcessamentoResponse> status(
            @PathVariable Long id,
            Authentication auth) {

        Long usuarioId = usuarioResolver.resolverUsuarioId(auth);
        return ResponseEntity.ok(consultarProcessamento.consultar(id, usuarioId));
    }

    @PostMapping("/reprocessar")
    public ResponseEntity<StatusProcessamentoResponse> reprocessar(
            @PathVariable Long id,
            Authentication auth) {

        Long usuarioId = usuarioResolver.resolverUsuarioId(auth);

        consultarProcessamento.validarReprocessamento(id, usuarioId);
        consultarProcessamento.dispararReprocessamento(id);

        log.info("Reprocessamento solicitado | concursoId={}", id);

        // PROCESSANDO já na resposta: o frontend retoma o polling sem
        // precisar esperar a primeira volta do executor.
        return ResponseEntity.accepted().body(
                StatusProcessamentoResponse.de(id, StatusProcessamento.PROCESSANDO, null, null));
    }
}
