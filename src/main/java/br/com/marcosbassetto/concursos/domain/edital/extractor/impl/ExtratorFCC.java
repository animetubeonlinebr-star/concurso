package br.com.marcosbassetto.concursos.domain.edital.extractor.impl;

import br.com.marcosbassetto.concursos.domain.edital.domain.Banca;
import br.com.marcosbassetto.concursos.domain.edital.dto.CursoExtraido;
import br.com.marcosbassetto.concursos.domain.edital.dto.DadosConcurso;
import br.com.marcosbassetto.concursos.domain.edital.dto.EstruturaEditalDTO;
import br.com.marcosbassetto.concursos.domain.edital.dto.MateriaExtraida;
import br.com.marcosbassetto.concursos.domain.edital.extractor.ExtratorEdital;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class ExtratorFCC implements ExtratorEdital {

    private static final Pattern INSTITUICAO_PATTERN = Pattern.compile(
            "(?i)(CENTRO UNIVERSITÁRIO ADVENTISTA DE SÃO PAULO|UNASP)");

    private static final Pattern CURSO_PATTERN = Pattern.compile(
            "(?i)(Bacharelado em Medicina|Curso de Medicina)");

    private static final Pattern DISCIPLINAS_PATTERN = Pattern.compile(
            "(?i)(Língua Portuguesa|Literatura Brasileira|Biologia|Química|"
                    + "Língua Inglesa|Matemática|Física|História|Geografia)");

    @Override
    public Banca bancaSuportada() {
        return Banca.FCC;
    }

    @Override
    public EstruturaEditalDTO extrair(String texto) {
        log.info("Iniciando extração específica para a banca FCC...");

        if (texto == null || texto.isBlank()) {
            return EstruturaEditalDTO.naoIdentificado("Texto do edital está vazio.");
        }

        String instituicao = primeiroMatch(INSTITUICAO_PATTERN, texto);
        String cursoNome = primeiroMatch(CURSO_PATTERN, texto);
        List<String> disciplinas = todasOcorrencias(DISCIPLINAS_PATTERN, texto);

        List<MateriaExtraida> materias = disciplinas.stream()
                .map(nome -> new MateriaExtraida(nome, List.of()))
                .toList();

        CursoExtraido curso = new CursoExtraido(
                cursoNome != null ? cursoNome : "Medicina",
                null,
                materias
        );

        DadosConcurso dados = new DadosConcurso(
                "Processo Seletivo Medicina 2027",
                instituicao != null ? instituicao : "UNASP",
                cursoNome != null ? cursoNome : "Medicina",
                "FCC",
                2027
        );

        log.info("FCC Extraído -> Órgão: {} | Curso: {} | Matérias: {}",
                dados.orgao(), curso.nome(), materias.size());

        return new EstruturaEditalDTO(dados, materias, List.of(curso));
    }

    private String primeiroMatch(Pattern p, String texto) {
        Matcher m = p.matcher(texto);
        return m.find() ? m.group().trim() : null;
    }

    private List<String> todasOcorrencias(Pattern p, String texto) {
        Matcher m = p.matcher(texto);
        List<String> resultado = new ArrayList<>();
        while (m.find()) {
            String item = m.group().trim();
            if (!resultado.contains(item)) {
                resultado.add(item);
            }
        }
        return resultado;
    }
}