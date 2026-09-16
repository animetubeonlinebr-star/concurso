package br.com.marcosbassetto.concursos.domain.simulado.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Criação de simulado a partir de uma matéria.
 * A seleção das questões acontece no backend.
 */
public record CriarSimuladoRequest(

        @NotNull(message = "O concurso é obrigatório")
        Long concursoId,

        @NotNull(message = "A matéria é obrigatória")
        Long materiaId,

        @Min(value = 1, message = "A quantidade de questões deve ser pelo menos 1")
        @Max(value = 100, message = "A quantidade de questões deve ser no máximo 100")
        Integer quantidadeQuestoes,

        String titulo
) {
}