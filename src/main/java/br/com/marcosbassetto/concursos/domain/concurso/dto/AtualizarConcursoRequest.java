package br.com.marcosbassetto.concursos.domain.concurso.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;


public record AtualizarConcursoRequest(

        @NotBlank(message = "O nome do concurso é obrigatório")
        @Size(max = 200, message = "O nome deve ter no máximo 200 caracteres")
        String nome,

        @Size(max = 200)
        String orgao,

        @Size(max = 100)
        String cargo,

        @Size(max = 100)
        String banca,

        Integer ano,

        @Size(max = 500)
        String descricao
) {
}
