package br.com.marcosbassetto.concursos.domain.usuario.controller;

import br.com.marcosbassetto.concursos.domain.usuario.dto.UsuarioDTO;
import br.com.marcosbassetto.concursos.domain.usuario.dto.UsuarioListResponse;
import br.com.marcosbassetto.concursos.domain.usuario.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    @PostMapping
    public ResponseEntity<UsuarioDTO> criar(@Valid @RequestBody UsuarioDTO dto) {
        log.info("Criando usuário: {}", dto.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(usuarioService.criar(dto));
    }

    @PostMapping("/register")
    public ResponseEntity<UsuarioDTO> register(@Valid @RequestBody UsuarioDTO dto) {
        log.info("Registrando novo usuário: {}", dto.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(usuarioService.criar(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UsuarioDTO> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody UsuarioDTO dto) {
        log.info("Atualizando usuário ID: {}", id);
        return ResponseEntity.ok(usuarioService.atualizar(id, dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.buscarPorId(id));
    }

    @GetMapping("/email")
    public ResponseEntity<UsuarioDTO> buscarPorEmail(@RequestParam String email) {
        return ResponseEntity.ok(usuarioService.buscarPorEmail(email));
    }

    @GetMapping
    public ResponseEntity<UsuarioListResponse> listar(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(usuarioService.listar(pageable));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<UsuarioDTO> atualizarStatus(
            @PathVariable Long id,
            @RequestParam boolean ativo) {
        log.info("Alterando status do usuário ID: {} para: {}", id, ativo);
        return ResponseEntity.ok(usuarioService.atualizarStatus(id, ativo));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        log.info("Excluindo usuário ID: {}", id);
        usuarioService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
