package br.com.marcosbassetto.concursos.domain.topico.dto;

import br.com.marcosbassetto.concursos.domain.topico.entity.TopicoEntity;

public record TopicoResponse(
        Long id,
        Long materiaId,
        String nome,
        String descricao,
        Integer ordem,
        Boolean ativo
) {

    public static TopicoResponse from(TopicoEntity topico) {
        return new TopicoResponse(
                topico.getId(),
                topico.getMateria() != null ? topico.getMateria().getId() : null,
                topico.getNome(),
                topico.getDescricao(),
                topico.getOrdem(),
                topico.getAtivo()
        );
    }
}