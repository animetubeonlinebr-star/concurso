package br.com.marcosbassetto.concursos.domain.edital.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CursoDTO {

    private String nome;
    private Integer vagas;
    private String modalidade;
}
