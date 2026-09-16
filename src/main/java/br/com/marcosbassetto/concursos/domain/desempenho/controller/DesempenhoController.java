package br.com.marcosbassetto.concursos.domain.desempenho.controller;

import br.com.marcosbassetto.concursos.common.security.UsuarioAutenticadoResolver;
import br.com.marcosbassetto.concursos.domain.correcao.dto.DesempenhoResponse;
import br.com.marcosbassetto.concursos.domain.desempenho.dto.DesempenhoMateriaResponse;
import br.com.marcosbassetto.concursos.domain.desempenho.dto.EvolucaoDesempenhoResponse;
import br.com.marcosbassetto.concursos.domain.desempenho.service.DesempenhoService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/desempenho")
@RequiredArgsConstructor
public class DesempenhoController {

    private final DesempenhoService desempenhoService;
    private final UsuarioAutenticadoResolver usuarioAutenticadoResolver;

    @GetMapping
    public DesempenhoResponse getDesempenho(Authentication auth) {
        Long usuarioId = usuarioAutenticadoResolver.resolverUsuarioId(auth);
        return desempenhoService.consultar(usuarioId);
    }

    @GetMapping("/concursos/{concursoId}")
    public DesempenhoResponse getDesempenhoPorConcurso(@PathVariable Long concursoId,
                                                       Authentication auth) {
        Long usuarioId = usuarioAutenticadoResolver.resolverUsuarioId(auth);
        return desempenhoService.consultarPorConcurso(usuarioId, concursoId);
    }

    @GetMapping("/concursos/{concursoId}/materias")
    public List<DesempenhoMateriaResponse> getDesempenhoPorMateria(@PathVariable Long concursoId,
                                                                   Authentication auth) {
        Long usuarioId = usuarioAutenticadoResolver.resolverUsuarioId(auth);
        return desempenhoService.consultarPorMateria(usuarioId, concursoId);
    }

    @GetMapping("/evolucao")
    public EvolucaoDesempenhoResponse getEvolucao(Authentication auth) {
        Long usuarioId = usuarioAutenticadoResolver.resolverUsuarioId(auth);
        return desempenhoService.consultarEvolucao(usuarioId);
    }
}