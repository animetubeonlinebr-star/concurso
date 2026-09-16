package br.com.marcosbassetto.concursos.domain.topico.controller;

import br.com.marcosbassetto.concursos.common.security.UsuarioAutenticadoResolver;
import br.com.marcosbassetto.concursos.domain.topico.dto.TopicoResponse;
import br.com.marcosbassetto.concursos.domain.topico.repository.TopicoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/materias")
@RequiredArgsConstructor
public class TopicoController {

    private final TopicoRepository topicoRepository;
    private final UsuarioAutenticadoResolver usuarioAutenticadoResolver;

    @GetMapping("/{materiaId}/topicos")
    public List<TopicoResponse> listarPorMateria(@PathVariable Long materiaId,
                                                 Authentication auth) {
        Long usuarioId = usuarioAutenticadoResolver.resolverUsuarioId(auth);
        return topicoRepository.findByMateriaIdAndUsuarioId(materiaId, usuarioId).stream()
                .map(TopicoResponse::from)
                .toList();
    }
}