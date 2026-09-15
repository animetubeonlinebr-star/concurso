package br.com.marcosbassetto.concursos.domain.edital.dto;

import java.util.List;

public record CursoExtraido(
        String nome,
        String codigo,
        List<MateriaExtraida> materias
) {
    public CursoExtraido {
        if (materias == null) materias = List.of();
    }
}