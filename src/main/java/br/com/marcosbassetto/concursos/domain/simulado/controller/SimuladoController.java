package br.com.marcosbassetto.concursos.domain.simulado.controller;

import br.com.marcosbassetto.concursos.common.security.UsuarioAutenticadoResolver;
import br.com.marcosbassetto.concursos.domain.simulado.dto.CriarSimuladoRequest;
import br.com.marcosbassetto.concursos.domain.simulado.dto.SimuladoQuestaoResponse;
import br.com.marcosbassetto.concursos.domain.simulado.dto.SimuladoResponse;
import br.com.marcosbassetto.concursos.domain.simulado.entity.SimuladoEntity;
import br.com.marcosbassetto.concursos.domain.simulado.mapper.SimuladoMapper;
import br.com.marcosbassetto.concursos.domain.simulado.service.SimuladoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints de simulado.
 *
 * A entidade nunca é serializada diretamente: o mapeamento para DTO evita expor
 * o gabarito e mantém o contrato estável.
 */
@RestController
@RequestMapping("/api/v1/simulados")
@RequiredArgsConstructor
public class SimuladoController {

    private final SimuladoService simuladoService;
    private final SimuladoMapper simuladoMapper;
    private final UsuarioAutenticadoResolver usuarioAutenticadoResolver;

    @PostMapping
    public ResponseEntity<SimuladoResponse> criar(@Valid @RequestBody CriarSimuladoRequest request,
                                                  Authentication auth) {
        Long usuarioId = usuarioAutenticadoResolver.resolverUsuarioId(auth);
        SimuladoEntity criado = simuladoService.criar(usuarioId, simuladoMapper.toEntity(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(simuladoMapper.toResponse(criado));
    }

    @GetMapping
    public List<SimuladoResponse> listar(Authentication auth) {
        Long usuarioId = usuarioAutenticadoResolver.resolverUsuarioId(auth);
        return simuladoService.listar(usuarioId).stream()
                .map(simuladoMapper::toResponse)
                .toList();
    }

    @GetMapping("/{id}")
    public SimuladoResponse buscar(@PathVariable Long id, Authentication auth) {
        Long usuarioId = usuarioAutenticadoResolver.resolverUsuarioId(auth);
        return simuladoMapper.toResponse(simuladoService.buscarPorId(id, usuarioId));
    }

    @PostMapping("/{id}/iniciar")
    public SimuladoResponse iniciar(@PathVariable Long id, Authentication auth) {
        Long usuarioId = usuarioAutenticadoResolver.resolverUsuarioId(auth);
        return simuladoMapper.toResponse(simuladoService.iniciar(id, usuarioId));
    }

    @PostMapping("/{id}/finalizar")
    public SimuladoResponse finalizar(@PathVariable Long id, Authentication auth) {
        Long usuarioId = usuarioAutenticadoResolver.resolverUsuarioId(auth);
        return simuladoMapper.toResponse(simuladoService.finalizar(id, usuarioId));
    }

    @PostMapping("/{id}/cancelar")
    public SimuladoResponse cancelar(@PathVariable Long id, Authentication auth) {
        Long usuarioId = usuarioAutenticadoResolver.resolverUsuarioId(auth);
        return simuladoMapper.toResponse(simuladoService.cancelar(id, usuarioId));
    }

    @GetMapping("/{id}/questoes")
    public List<SimuladoQuestaoResponse> questoes(@PathVariable Long id, Authentication auth) {
        Long usuarioId = usuarioAutenticadoResolver.resolverUsuarioId(auth);
        return simuladoService.listarQuestoes(usuarioId, id);
    }
}