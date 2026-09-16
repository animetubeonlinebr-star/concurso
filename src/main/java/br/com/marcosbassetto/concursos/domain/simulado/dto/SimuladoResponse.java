package br.com.marcosbassetto.concursos.domain.simulado.dto;

import br.com.marcosbassetto.concursos.domain.simulado.domain.StatusSimulado;

import java.time.LocalDateTime;

public record SimuladoResponse(
        Long id,
        Long concursoId,
        Long materiaId,
        String titulo,
        String descricao,
        StatusSimulado status,
        Integer quantidadeQuestoes,
        LocalDateTime criadoEm,
        LocalDateTime iniciadoEm,
        LocalDateTime finalizadoEm
) {
}