package br.com.marcosbassetto.concursos.domain.concurso.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;


public record CriarConcursoRequest(

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
        String descricao,

        @NotEmpty(message = "O concurso deve ter pelo menos uma matéria")
        @Valid
        List<MateriaRequest> materias
) {

        public record MateriaRequest(

                @NotBlank(message = "O nome da matéria é obrigatório")
                @Size(max = 200, message = "O nome da matéria deve ter no máximo 200 caracteres")
                String nome,

                @NotEmpty(message = "A matéria deve ter pelo menos um tópico")
                List<@NotBlank(message = "O tópico não pode ser vazio") String> topicos
        ) {
        }
}
