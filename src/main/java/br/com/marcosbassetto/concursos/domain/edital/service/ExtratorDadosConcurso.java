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


    public String extrairNome(String texto) {
        String[] patterns = {
                "EDITAL\\s+N[º°]\\s*\\d+\\s*/\\s*\\d+\\s*-\\s*CONCURSO\\s+PÚBLICO\\s+PARA\\s+([A-ZÀ-Ú\\s]+)",
                "CONCURSO\\s+PÚBLICO\\s+PARA\\s+PROVIMENTO\\s+DE\\s+VAGAS\\s+NO\\s+CARGO\\s+DE\\s+([A-ZÀ-Ú\\s]+)",
                "CONCURSO\\s+PÚBLICO\\s+PARA\\s+([A-ZÀ-Ú\\s]+)",
                "CONCURSO\\s+PÚBLICO\\s+([A-ZÀ-Ú\\s]+)",
                "CONCURSO\\s+([A-ZÀ-Ú\\s]+)"
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
                "BANCA\\s+EXAMINADORA\\s*[:]\\s*([A-ZÀ-Ú\\s]+)",
                "BANCA\\s*[:]\\s*([A-ZÀ-Ú\\s]+)",
                "ORGANIZADORA\\s*[:]\\s*([A-ZÀ-Ú\\s]+)",
                "INSTITUIÇÃO\\s*[:]\\s*([A-ZÀ-Ú\\s]+)"
        };
        for (String padrao : padroes) {
            Matcher m = Pattern.compile(padrao, Pattern.CASE_INSENSITIVE | Pattern.DOTALL)
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
        Matcher m = Pattern.compile("ÓRG[AO]\\s*[:]\\s*([A-ZÀ-Ú\\s]+)",
                Pattern.CASE_INSENSITIVE).matcher(texto);
        if (m.find()) return m.group(1).trim();

        m = Pattern.compile(
                "AUTARQUIA\\s+MUNICIPAL\\s+DE\\s+SAÚDE\\s*-\\s*([A-ZÀ-Ú\\s]+?)(?=\\s+CONCURSO|\\s+EDITAL|$)",
                Pattern.CASE_INSENSITIVE).matcher(texto);
        if (m.find()) return "Autarquia Municipal de Saúde - " + m.group(1).trim();

        m = Pattern.compile("MUNIC[ÍI]PIO\\s+DE\\s+([A-ZÀ-Ú\\s]+)",
                Pattern.CASE_INSENSITIVE).matcher(texto);
        if (m.find()) return "Município de " + m.group(1).trim();

        m = Pattern.compile("PREFEITURA\\s+MUNICIPAL\\s+DE\\s+([A-ZÀ-Ú\\s]+)",
                Pattern.CASE_INSENSITIVE).matcher(texto);
        if (m.find()) return "Prefeitura Municipal de " + m.group(1).trim();

        m = Pattern.compile("SECRETARIA\\s+DE\\s+([A-ZÀ-Ú\\s]+)",
                Pattern.CASE_INSENSITIVE).matcher(texto);
        if (m.find()) return "Secretaria de " + m.group(1).trim();

        m = Pattern.compile("MINIST[ÉE]RIO\\s+DA\\s+([A-ZÀ-Ú\\s]+)",
                Pattern.CASE_INSENSITIVE).matcher(texto);
        if (m.find()) return "Ministério da " + m.group(1).trim();

        m = Pattern.compile("TRIBUNAL\\s+([A-ZÀ-Ú\\s]+)",
                Pattern.CASE_INSENSITIVE).matcher(texto);
        if (m.find()) return "Tribunal " + m.group(1).trim();

        m = Pattern.compile("POL[ÍI]CIA\\s+([A-ZÀ-Ú\\s]+)",
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
