package br.com.marcosbassetto.concursos.domain.edital.dto;

import java.util.List;


public record MateriaExtraida(
        String nome,
        List<TopicoExtraido> topicos
) {
    public MateriaExtraida {
        if (topicos == null) topicos = List.of();
    }
}
