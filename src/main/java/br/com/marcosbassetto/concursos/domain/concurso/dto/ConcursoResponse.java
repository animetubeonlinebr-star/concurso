package br.com.marcosbassetto.concursos.domain.concurso.dto;

import br.com.marcosbassetto.concursos.common.enums.Status;

import java.time.LocalDateTime;


public record ConcursoResponse(
        Long id,
        Long usuarioId,
        String nome,
        String orgao,
        String cargo,
        String banca,
        Integer ano,
        String descricao,
        Status status,
        Boolean processado,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm
) {
}