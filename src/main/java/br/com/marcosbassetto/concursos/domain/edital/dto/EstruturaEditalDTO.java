package br.com.marcosbassetto.concursos.domain.edital.dto;

import java.util.List;

public record EstruturaEditalDTO(
        DadosConcurso dadosConcurso,
        List<MateriaExtraida> materias,
        List<CursoExtraido> cursos,
        StatusExtracao status,
        String mensagem
) {
    public EstruturaEditalDTO {
        if (materias == null) materias = List.of();
        if (cursos == null) cursos = List.of();
        if (status == null) status = StatusExtracao.PROCESSADO;
    }

    public EstruturaEditalDTO(DadosConcurso dadosConcurso,
                              List<MateriaExtraida> materias,
                              List<CursoExtraido> cursos) {
        this(dadosConcurso, materias, cursos, calcularStatus(dadosConcurso, materias, cursos), null);
    }

    public static EstruturaEditalDTO processado(DadosConcurso d, List<CursoExtraido> cursos) {
        return new EstruturaEditalDTO(d, List.of(), cursos, StatusExtracao.PROCESSADO, null);
    }

    public static EstruturaEditalDTO naoIdentificado(String msg) {
        return new EstruturaEditalDTO(null, List.of(), List.of(),
                StatusExtracao.NAO_IDENTIFICADO, msg);
    }

    public List<MateriaExtraida> materiasDoPrimeiroCurso() {
        return cursos.isEmpty() ? List.of() : cursos.get(0).materias();
    }

    private static StatusExtracao calcularStatus(DadosConcurso d,
                                                 List<MateriaExtraida> materias,
                                                 List<CursoExtraido> cursos) {
        int totalMaterias = cursos.stream().mapToInt(c -> c.materias().size()).sum();
        if (totalMaterias == 0) return StatusExtracao.NAO_IDENTIFICADO;
        if (totalMaterias < 2) return StatusExtracao.BAIXA_CONFIANCA;
        if (d == null || d.orgao() == null || d.ano() == null) return StatusExtracao.PARCIAL;
        return StatusExtracao.PROCESSADO;
    }
}
