package br.com.marcosbassetto.concursos.domain.edital.dto;

import java.util.List;

/**
 * Resultado da confirmação: o que efetivamente foi gravado em
 * {@code materia}/{@code topico}.
 */
public record ConfirmacaoEstruturaResponse(
        Long concursoId,
        int materiasPersistidas,
        int topicosPersistidos,
        List<String> materiasIgnoradas
) {
}