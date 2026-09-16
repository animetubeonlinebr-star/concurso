package br.com.marcosbassetto.concursos.api.controller;

import br.com.marcosbassetto.concursos.application.edital.ConfirmarEstruturaUseCase;
import br.com.marcosbassetto.concursos.application.edital.ConsultarRevisaoUseCase;
import br.com.marcosbassetto.concursos.application.edital.RevisarEstruturaUseCase;
import br.com.marcosbassetto.concursos.common.security.UsuarioAutenticadoResolver;
import br.com.marcosbassetto.concursos.domain.edital.dto.ConfirmacaoEstruturaResponse;
import br.com.marcosbassetto.concursos.domain.edital.dto.RevisaoEstruturaRequest;
import br.com.marcosbassetto.concursos.domain.edital.dto.RevisaoEstruturaResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Revisão da estrutura extraída e confirmação final: telas
 * {@code /concursos/{id}/revisao} e {@code /concursos/{id}/conteudo}.
 */
@RestController
@RequestMapping("/api/v1/concursos/{id}")
@RequiredArgsConstructor
public class EditalRevisaoController {

    private final ConsultarRevisaoUseCase consultarRevisao;
    private final RevisarEstruturaUseCase revisarEstrutura;
    private final ConfirmarEstruturaUseCase confirmarEstrutura;
    private final UsuarioAutenticadoResolver usuarioResolver;

    @GetMapping("/revisao")
    public ResponseEntity<RevisaoEstruturaResponse> revisao(
            @PathVariable Long id,
            Authentication auth) {

        Long usuarioId = usuarioResolver.resolverUsuarioId(auth);
        return ResponseEntity.ok(consultarRevisao.consultar(id, usuarioId));
    }

    @PutMapping("/revisao")
    public ResponseEntity<RevisaoEstruturaResponse> atualizarRevisao(
            @PathVariable Long id,
            @RequestBody RevisaoEstruturaRequest request,
            Authentication auth) {

        Long usuarioId = usuarioResolver.resolverUsuarioId(auth);

        revisarEstrutura.aplicar(id, request, usuarioId);

        // Devolve o estado já persistido: a tela continua em sincronia sem
        // precisar de um segundo GET após cada edição.
        return ResponseEntity.ok(consultarRevisao.consultar(id, usuarioId));
    }

    @PostMapping("/confirmar")
    public ResponseEntity<ConfirmacaoEstruturaResponse> confirmar(
            @PathVariable Long id,
            Authentication auth) {

        Long usuarioId = usuarioResolver.resolverUsuarioId(auth);
        return ResponseEntity.ok(confirmarEstrutura.confirmar(id, usuarioId));
    }
}
