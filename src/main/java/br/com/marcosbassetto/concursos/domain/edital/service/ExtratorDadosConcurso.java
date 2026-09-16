package br.com.marcosbassetto.concursos.domain.edital.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Slf4j
@Service
public class ExtratorDadosConcurso {

    private static final List<String> BANCAS_CONHECIDAS = List.of(
            "CEBRASPE", "FGV", "VUNESP", "FCC", "CESPE", "CESGRANRIO",
            "IBFC", "AOCP", "FUNDATEC", "FUNDEP", "FUMARC", "FADESP",
            "FAPESE", "FEPESE", "FAUEL", "FAURGS", "UEG", "COMPERVE",
            "COVEST", "CETREDE", "CEPERJ", "COPESE", "COPEVE", "IBGP",
            "CONSULPLAN", "QUADRIX", "IADES", "INAZ", "IDECAN", "SELECON",
            "IBADE", "LEGATUS", "AVANÇA SP", "AVANCASP", "SHDIAS",
            "INSTITUTO MAIS", "INSTITUTO SELECON", "INSTITUTO AOCP",
            "INSTITUTO CONSULPLAN", "INSTITUTO QUADRIX", "INSTITUTO IDECAN"
    );


    /**
     * Valores de rótulo de uma linha (BANCA:, ÓRGÃO:, ANO:) terminam no
     * próximo rótulo conhecido. O texto chega aqui com os espaços já
     * colapsados, então não há quebra de linha para delimitar o valor — sem
     * esta âncora, "BANCA: CEBRASPE\nÓRGÃO: ..." viraria "CEBRASPE ÓRGÃO".
     */
    private static final String FIM_DO_VALOR =
            "(?=\\s+(?:ÓRG[ÃA]O|ORGAO|ANO|CARGO|BANCA|ORGANIZADORA|INSTITUIÇÃO"
                    + "|INSTITUICAO|CONTEÚDO|CONTEUDO|EDITAL|VAGAS)\\b|$)";

    // \p{L} e não A-Z: órgãos como "Polícia Federal" chegam com caixa mista,
    // enquanto bancas como "CEBRASPE" vêm em caixa alta. Ambos precisam caber.
    private static final String VALOR =
            "([\\p{L}][\\p{L} ]*?)" + FIM_DO_VALOR;

    public String extrairNome(String texto) {
        String[] patterns = {
                "EDITAL\\s+N[º°]\\s*\\d+\\s*/\\s*\\d+\\s*-\\s*CONCURSO\\s+PÚBLICO\\s+PARA\\s+([A-ZÀ-Ú][A-ZÀ-Ú \\t]*)",
                "CONCURSO\\s+PÚBLICO\\s+PARA\\s+PROVIMENTO\\s+DE\\s+VAGAS\\s+NO\\s+CARGO\\s+DE\\s+([A-ZÀ-Ú][A-ZÀ-Ú \\t]*)",
                "CONCURSO\\s+PÚBLICO\\s+PARA\\s+([A-ZÀ-Ú][A-ZÀ-Ú \\t]*)",
                "CONCURSO\\s+PÚBLICO\\s+([A-ZÀ-Ú][A-ZÀ-Ú \\t]*)",
                "CONCURSO\\s+([\\p{L}][\\p{L} ]*?)" + FIM_DO_VALOR
        };
        for (String p : patterns) {
            Matcher m = Pattern.compile(p, Pattern.CASE_INSENSITIVE).matcher(texto);
            if (m.find()) return m.group(1).trim();
        }

        String orgao = extrairOrgao(texto);
        if (orgao != null && !orgao.isBlank()) return orgao;

        Matcher m = Pattern.compile("CONCURSO\\s+PÚBLICO\\s+\\d+/(\\d{4})",
                Pattern.CASE_INSENSITIVE).matcher(texto);
        if (m.find()) return "Concurso Público " + m.group(1);

        return null;
    }


    public String extrairBanca(String texto) {
        String upper = texto.toUpperCase();

        if (upper.contains("AVANÇASP") || upper.contains("AVANCASP")) return "AVANÇASP";

        String[] padroes = {
                "BANCA\\s+EXAMINADORA\\s*[:]\\s*" + VALOR,
                "BANCA\\s*[:]\\s*" + VALOR,
                "ORGANIZADORA\\s*[:]\\s*" + VALOR,
                "INSTITUIÇÃO\\s*[:]\\s*" + VALOR
        };
        for (String padrao : padroes) {
            Matcher m = Pattern.compile(padrao, Pattern.CASE_INSENSITIVE)
                    .matcher(texto);
            if (m.find()) {
                String result = m.group(1).trim();
                if (!result.isBlank() && result.length() > 2) return result;
            }
        }

        for (String banca : BANCAS_CONHECIDAS) {
            if (upper.contains(banca.toUpperCase())) return banca;
        }
        return null;
    }

    // =================================================================
    // ÓRGÃO
    // =================================================================
    public String extrairOrgao(String texto) {
        Matcher m = Pattern.compile("ÓRG[ÃA]O\\s*[:]\\s*" + VALOR,
                Pattern.CASE_INSENSITIVE).matcher(texto);
        if (m.find()) return m.group(1).trim();

        m = Pattern.compile(
                "AUTARQUIA\\s+MUNICIPAL\\s+DE\\s+SAÚDE\\s*-\\s*([A-ZÀ-Ú\\s]+?)(?=\\s+CONCURSO|\\s+EDITAL|$)",
                Pattern.CASE_INSENSITIVE).matcher(texto);
        if (m.find()) return "Autarquia Municipal de Saúde - " + m.group(1).trim();

        m = Pattern.compile("MUNIC[ÍI]PIO\\s+DE\\s+([\\p{L}][\\p{L} ]*?)" + FIM_DO_VALOR,
                Pattern.CASE_INSENSITIVE).matcher(texto);
        if (m.find()) return "Município de " + m.group(1).trim();

        m = Pattern.compile("PREFEITURA\\s+MUNICIPAL\\s+DE\\s+([\\p{L}][\\p{L} ]*?)" + FIM_DO_VALOR,
                Pattern.CASE_INSENSITIVE).matcher(texto);
        if (m.find()) return "Prefeitura Municipal de " + m.group(1).trim();

        m = Pattern.compile("SECRETARIA\\s+DE\\s+([\\p{L}][\\p{L} ]*?)" + FIM_DO_VALOR,
                Pattern.CASE_INSENSITIVE).matcher(texto);
        if (m.find()) return "Secretaria de " + m.group(1).trim();

        m = Pattern.compile("MINIST[ÉE]RIO\\s+DA\\s+([\\p{L}][\\p{L} ]*?)" + FIM_DO_VALOR,
                Pattern.CASE_INSENSITIVE).matcher(texto);
        if (m.find()) return "Ministério da " + m.group(1).trim();

        m = Pattern.compile("TRIBUNAL\\s+([\\p{L}][\\p{L} ]*?)" + FIM_DO_VALOR,
                Pattern.CASE_INSENSITIVE).matcher(texto);
        if (m.find()) return "Tribunal " + m.group(1).trim();

        m = Pattern.compile("POL[ÍI]CIA\\s+([\\p{L}][\\p{L} ]*?)" + FIM_DO_VALOR,
                Pattern.CASE_INSENSITIVE).matcher(texto);
        if (m.find()) return "Polícia " + m.group(1).trim();

        return null;
    }


    public Integer extrairAno(String texto) {
        String[] patterns = {
                "CONCURSO\\s+PÚBLICO\\s+\\d+/(\\d{4})",
                "EDITAL\\s+N[º°]\\s*\\d+\\s*/\\s*(\\d{4})",
                "ANO\\s*[:]\\s*(\\d{4})",
                "\\b(20\\d{2})\\b"
        };
        for (String p : patterns) {
            Matcher m = Pattern.compile(p, Pattern.CASE_INSENSITIVE).matcher(texto);
            if (m.find()) {
                try {
                    return Integer.parseInt(m.group(1));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return null;
    }
}
