package br.com.marcosbassetto.concursos.domain.edital.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DadosConcursoDTO {

    private String banca;
    private String instituicao;
    private String titulo;
    private BigDecimal taxaInscricao;
    private LocalDate dataInicioInscricao;
    private LocalDate dataFimInscricao;
    private LocalDate dataProva;

    @Builder.Default
    private List<String> cidadesProva = new ArrayList<>();

    @Builder.Default
    private String status = "IDENTIFICADO";
}
