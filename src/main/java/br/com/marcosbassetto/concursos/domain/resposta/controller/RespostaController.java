package br.com.marcosbassetto.concursos.domain.resposta.controller;

import br.com.marcosbassetto.concursos.common.security.UsuarioAutenticadoResolver;
import br.com.marcosbassetto.concursos.domain.resposta.dto.AtualizarRespostaRequest;
import br.com.marcosbassetto.concursos.domain.resposta.dto.RegistrarRespostaRequest;
import br.com.marcosbassetto.concursos.domain.resposta.dto.RespostaResponse;
import br.com.marcosbassetto.concursos.domain.resposta.usecase.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Registro e consulta das respostas do usuário durante o simulado.
 *
 * As rotas de resposta e correção coexistem sob /simulados/{id}: registrar a
 * resposta é parte de responder o simulado, enquanto a correção ocorre depois
 * da finalização.
 */
@RestController
@RequestMapping("/api/v1/simulados/{simuladoId}")
@RequiredArgsConstructor
public class RespostaController {

    private final RegistrarRespostaUseCase registrarRespostaUseCase;
    private final AtualizarRespostaUseCase atualizarRespostaUseCase;
    private final BuscarRespostaUseCase buscarRespostaUseCase;
    private final ListarRespostasUseCase listarRespostasUseCase;
    private final UsuarioAutenticadoResolver usuarioAutenticadoResolver;

    @PostMapping("/questoes/{simuladoQuestaoId}/resposta")
    public ResponseEntity<RespostaResponse> registrar(
            @PathVariable Long simuladoId,
            @PathVariable Long simuladoQuestaoId,
            @Valid @RequestBody RegistrarRespostaRequest request,
            Authentication auth) {

        Long usuarioId = usuarioAutenticadoResolver.resolverUsuarioId(auth);
        RespostaResponse resposta = registrarRespostaUseCase.execute(
                usuarioId, simuladoId, simuladoQuestaoId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(resposta);
    }

    @PutMapping("/questoes/{simuladoQuestaoId}/resposta")
    public RespostaResponse atualizar(
            @PathVariable Long simuladoId,
            @PathVariable Long simuladoQuestaoId,
            @Valid @RequestBody AtualizarRespostaRequest request,
            Authentication auth) {

        Long usuarioId = usuarioAutenticadoResolver.resolverUsuarioId(auth);
        return atualizarRespostaUseCase.execute(usuarioId, simuladoId, simuladoQuestaoId, request);
    }

    @GetMapping("/questoes/{simuladoQuestaoId}/resposta")
    public ResponseEntity<RespostaResponse> buscar(
            @PathVariable Long simuladoId,
            @PathVariable Long simuladoQuestaoId,
            Authentication auth) {

        Long usuarioId = usuarioAutenticadoResolver.resolverUsuarioId(auth);
        return buscarRespostaUseCase.execute(usuarioId, simuladoId, simuladoQuestaoId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/respostas")
    public List<RespostaResponse> listar(@PathVariable Long simuladoId, Authentication auth) {
        Long usuarioId = usuarioAutenticadoResolver.resolverUsuarioId(auth);
        return listarRespostasUseCase.execute(usuarioId, simuladoId);
    }
}