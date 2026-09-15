package br.com.marcosbassetto.concursos.application.edital;

import br.com.marcosbassetto.concursos.domain.concurso.service.ConcursoService;
import br.com.marcosbassetto.concursos.domain.edital.dto.EstruturaEditalDTO;
import br.com.marcosbassetto.concursos.domain.edital.service.EstruturaEditalProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProcessarEditalUseCase {

    private final ConcursoService concursoService;
    private final EstruturaEditalProvider estruturaProvider;

    public EstruturaEditalDTO processar(Long concursoId) {

        String texto = concursoService.buscarTextoExtraido(concursoId);

        return estruturaProvider.extrairEstrutura(texto);
    }
}
