package br.com.marcosbassetto.concursos.domain.simulado.controller;

import br.com.marcosbassetto.concursos.common.exception.ResourceNotFoundException;
import br.com.marcosbassetto.concursos.domain.simulado.entity.SimuladoEntity;
import br.com.marcosbassetto.concursos.domain.simulado.service.SimuladoService;
import br.com.marcosbassetto.concursos.domain.usuario.entity.UsuarioEntity;
import br.com.marcosbassetto.concursos.domain.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/simulados")
@RequiredArgsConstructor
public class SimuladoController {

    private final SimuladoService simuladoService;
    private final UsuarioRepository usuarioRepository; // ← Injete o repository

    @PostMapping
    public ResponseEntity<SimuladoEntity> criar(@RequestBody SimuladoEntity simulado, Authentication auth) {
        Long usuarioId = getUsuarioId(auth);
        return ResponseEntity.status(201).body(simuladoService.criar(usuarioId, simulado));
    }

    @GetMapping
    public List<SimuladoEntity> listar(Authentication auth) {
        Long usuarioId = getUsuarioId(auth);
        return simuladoService.listar(usuarioId);
    }

    @PostMapping("/{id}/finalizar")
    public ResponseEntity<SimuladoEntity> finalizar(@PathVariable Long id, Authentication auth) {
        Long usuarioId = getUsuarioId(auth);
        return ResponseEntity.ok(simuladoService.finalizar(id, usuarioId));
    }

    private Long getUsuarioId(Authentication auth) {
        String email = auth.getName();
        UsuarioEntity usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", "email", email));
        return usuario.getId();
    }
}
