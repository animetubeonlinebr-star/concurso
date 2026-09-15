package br.com.marcosbassetto.concursos.domain.edital.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class CriarConcursoCompletoRequest {

    @NotBlank(message = "O nome do concurso é obrigatório")
    @Size(max = 200, message = "O nome deve ter no máximo 200 caracteres")
    private String nome;

    private String orgao;
    private String banca;

    private Integer ano;

    @NotNull(message = "A lista de matérias não pode ser nula")
    @Valid
    private List<MateriaRequest> materias;


    @Data
    public static class MateriaRequest {

        @NotBlank(message = "O nome da matéria é obrigatório")
        @Size(max = 200, message = "O nome da matéria deve ter no máximo 200 caracteres")
        private String nome;

        private List<String> topicos;
    }
}
