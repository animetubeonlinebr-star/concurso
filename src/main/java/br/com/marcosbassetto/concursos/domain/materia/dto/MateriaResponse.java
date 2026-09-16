package br.com.marcosbassetto.concursos.domain.materia.dto;

import br.com.marcosbassetto.concursos.common.enums.Status;
import br.com.marcosbassetto.concursos.domain.materia.entity.MateriaEntity;

import java.math.BigDecimal;

/**
 * Matéria exposta para a navegação do frontend.
 *
 * {@code concursoId} é o concurso raiz (materia -> curso -> concurso): o
 * frontend sempre navega a partir do concurso.
 */
public record MateriaResponse(
        Long id,
        Long concursoId,
        Long cursoId,
        String nome,
        String descricao,
        Integer ordem,
        BigDecimal peso,
        Status status
) {

    public static MateriaResponse from(MateriaEntity materia) {
        Long concursoId = materia.getCurso() != null && materia.getCurso().getConcurso() != null
                ? materia.getCurso().getConcurso().getId()
                : null;

        return new MateriaResponse(
                materia.getId(),
                concursoId,
                materia.getCurso() != null ? materia.getCurso().getId() : null,
                materia.getNome(),
                materia.getDescricao(),
                materia.getOrdem(),
                materia.getPeso(),
                materia.getStatus()
        );
    }
}