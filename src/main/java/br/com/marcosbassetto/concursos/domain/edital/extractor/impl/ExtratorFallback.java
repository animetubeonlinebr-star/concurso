package br.com.marcosbassetto.concursos.domain.edital.extractor.impl;

import br.com.marcosbassetto.concursos.domain.edital.dto.EstruturaEditalDTO;
import br.com.marcosbassetto.concursos.domain.edital.extractor.ExtratorEdital;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class ExtratorFallback implements ExtratorEdital {

    private static final Pattern CURSO_PATTERN = Pattern.compile(
            "(?i)(Curso de Graduação em [A-Za-zÀ-ÿ]+|Bacharelado em [A-Za-zÀ-ÿ]+|Curso:\\s*[A-Za-zÀ-ÿ]+)");

    private static final Pattern VAGAS_PATTERN = Pattern.compile(
            "(?i)([0-9]+)\\s*(?:vagas|\\(uma\\)\\s*vaga|vaga)");

    private static final Pattern TAXA_PATTERN = Pattern.compile(
            "(?i)R\\$\\s*([0-9]{1,3}(?:\\.[0-9]{3})*,[0-9]{2})");

    private static final Pattern DATAS_CHAVE_PATTERN = Pattern.compile(
            "(?i)(inscriç[ãa]o|provas?|gabarito).{0,100}?(\\d{2}/\\d{2}/\\d{4})");

    @Override
    public EstruturaEditalDTO extrair(String texto) {
        if (texto == null || texto.isBlank()) {
            return new EstruturaEditalDTO(null, List.of(), List.of());
        }

        String curso = extrairPrimeiro(CURSO_PATTERN, texto);
        String vagas = extrairPrimeiroGrupo(VAGAS_PATTERN, texto, 1);
        String taxa = extrairPrimeiroGrupo(TAXA_PATTERN, texto, 1);
        String data = extrairPrimeiroGrupo(DATAS_CHAVE_PATTERN, texto, 2);

        log.info("Fallback extraiu -> Curso: {} | Vagas: {} | Taxa: R$ {} | Data ref: {}",
                curso, vagas, taxa, data);

        // TODO: Instanciar os objetos internos de EstruturaEditalDTO com as variáveis capturadas acima.
        // Exemplo:
        // DadosGeraisDTO dadosGerais = new DadosGeraisDTO();
        // dadosGerais.setCurso(curso);
        // dadosGerais.setVagas(vagas != null ? Integer.parseInt(vagas) : 0);

        return new EstruturaEditalDTO(null, List.of(), List.of());
    }


    private String extrairPrimeiro(Pattern pattern, String texto) {
        Matcher matcher = pattern.matcher(texto);
        if (matcher.find()) {
            return matcher.group().trim();
        }
        return null;
    }

     private String extrairPrimeiroGrupo(Pattern pattern, String texto, int grupo) {
        Matcher matcher = pattern.matcher(texto);
        if (matcher.find()) {
            return matcher.group(grupo).trim();
        }
        return null;
    }
}
