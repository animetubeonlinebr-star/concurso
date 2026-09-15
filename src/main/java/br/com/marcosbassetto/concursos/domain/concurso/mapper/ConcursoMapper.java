package br.com.marcosbassetto.concursos.domain.concurso.mapper;

import br.com.marcosbassetto.concursos.domain.concurso.dto.ConcursoResponse;
import br.com.marcosbassetto.concursos.domain.concurso.entity.ConcursoEntity;
import org.springframework.stereotype.Component;

@Component
public class ConcursoMapper {

    public ConcursoResponse toResponse(ConcursoEntity entity) {
        if (entity == null) {
            return null;
        }

        return new ConcursoResponse(
                entity.getId(),
                entity.getUsuarioId(),
                entity.getNome(),
                entity.getOrgao(),
                entity.getCargo(),
                entity.getBanca(),
                entity.getAno(),
                entity.getDescricao(),
                entity.getStatus(),
                entity.getProcessado(),
                entity.getCriadoEm(),
                entity.getAtualizadoEm()
        );
    }
}
