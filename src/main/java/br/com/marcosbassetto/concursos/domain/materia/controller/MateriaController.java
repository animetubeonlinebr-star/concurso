package br.com.marcosbassetto.concursos.domain.materia.controller;

import br.com.marcosbassetto.concursos.common.security.UsuarioAutenticadoResolver;
import br.com.marcosbassetto.concursos.domain.materia.dto.MateriaResponse;
import br.com.marcosbassetto.concursos.domain.materia.service.MateriaService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Matérias acessíveis pelo usuário autenticado.
 *
 * Existem duas rotas porque o frontend navega tanto a partir do concurso
 * (lista) quanto da própria matéria (detalhe).
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class MateriaController {

    private final MateriaService materiaService;
    private final UsuarioAutenticadoResolver usuarioAutenticadoResolver;

    @GetMapping("/concursos/{concursoId}/materias")
    public List<MateriaResponse> listarPorConcurso(@PathVariable Long concursoId,
                                                   Authentication auth) {
        Long usuarioId = usuarioAutenticadoResolver.resolverUsuarioId(auth);
        return materiaService.listarPorConcurso(concursoId, usuarioId);
    }

    @GetMapping("/materias/{id}")
    public MateriaResponse buscar(@PathVariable Long id, Authentication auth) {
        Long usuarioId = usuarioAutenticadoResolver.resolverUsuarioId(auth);
        return materiaService.buscarPorId(id, usuarioId);
    }
}