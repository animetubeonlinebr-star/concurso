package br.com.marcosbassetto.concursos.api.controller;

import br.com.marcosbassetto.concursos.common.exception.ResourceNotFoundException;
import br.com.marcosbassetto.concursos.domain.concurso.dto.AtualizarConcursoRequest;
import br.com.marcosbassetto.concursos.domain.concurso.dto.ConcursoResponse;
import br.com.marcosbassetto.concursos.domain.concurso.dto.CriarConcursoRequest;
import br.com.marcosbassetto.concursos.domain.concurso.service.ConcursoService;
import br.com.marcosbassetto.concursos.domain.usuario.entity.UsuarioEntity;
import br.com.marcosbassetto.concursos.domain.usuario.repository.UsuarioRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/concursos")
@RequiredArgsConstructor
public class ConcursoController {

    private final ConcursoService concursoService;
    private final UsuarioRepository usuarioRepository;

    @GetMapping
    public List<ConcursoResponse> listar(Authentication auth) {
        Long usuarioId = getUsuarioId(auth);
        return concursoService.listarPorUsuario(usuarioId);
    }

    @GetMapping("/{id}")
    public ConcursoResponse buscar(@PathVariable Long id, Authentication auth) {
        Long usuarioId = getUsuarioId(auth);
        return concursoService.buscarPorId(id, usuarioId);
    }

    @PostMapping
    public ResponseEntity<ConcursoResponse> criar(
            @Valid @RequestBody CriarConcursoRequest request,
            Authentication auth) {

        Long usuarioId = getUsuarioId(auth);
        ConcursoResponse novo = concursoService.criar(usuarioId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(novo);
    }

    @PutMapping("/{id}")
    public ConcursoResponse atualizar(
            @PathVariable Long id,
            @Valid @RequestBody AtualizarConcursoRequest request,
            Authentication auth) {

        Long usuarioId = getUsuarioId(auth);
        return concursoService.atualizar(id, request, usuarioId);
    }

    private Long getUsuarioId(Authentication auth) {
        String email = auth.getName();
        UsuarioEntity usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", "email", email));
        return usuario.getId();
    }
}
