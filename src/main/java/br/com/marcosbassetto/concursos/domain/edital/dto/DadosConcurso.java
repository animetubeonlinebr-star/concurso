package br.com.marcosbassetto.concursos.domain.edital.dto;

public record DadosConcurso(
        String nome,
        String orgao,
        String cargo,
        String banca,
        Integer ano
) {
}
