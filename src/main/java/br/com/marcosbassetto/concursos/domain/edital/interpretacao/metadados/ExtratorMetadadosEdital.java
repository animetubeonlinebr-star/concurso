package br.com.marcosbassetto.concursos.domain.edital.interpretacao.metadados;

import br.com.marcosbassetto.concursos.domain.edital.dto.DadosConcurso;
import br.com.marcosbassetto.concursos.domain.edital.interpretacao.documento.DocumentModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extrai os dados cadastrais do edital (órgão, cargo, banca, ano).
 *
 * Separado da interpretação da estrutura de propósito: esses campos vivem no
 * cabeçalho e não dependem de como o conteúdo programático está organizado.
 * Antes eles vinham de dentro de cada extrator, o que obrigava um extrator
 * focado em estrutura a também saber ler datas e valores.
 *
 * A extração é conservadora: devolve null em vez de adivinhar. Um campo nulo
 * aparece como "não identificado" na revisão, enquanto um campo errado passa
 * despercebido pelo usuário.
 */
@Slf4j
@Component
public class ExtratorMetadadosEdital {

    private static final Pattern ROTULO_ORGAO = Pattern.compile(
            "^\\s*(?:ÓRGÃO|ORGÃO|ENTIDADE|INSTITUIÇÃO)\\s*[:\\-]\\s*(.+)$",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);

    private static final Pattern INSTITUICAO = Pattern.compile(
            "(?:CENTRO UNIVERSITÁRIO|UNIVERSIDADE|FACULDADE|INSTITUTO|"
                    + "TRIBUNAL|MINISTÉRIO|PREFEITURA|CÂMARA MUNICIPAL|ASSEMBLEIA|"
                    + "COMPANHIA|EMPRESA|FUNDAÇÃO|SECRETARIA|CONSELHO)"
                    + "[^\\n]{3,120}",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern ROTULO_BANCA = Pattern.compile(
            "^\\s*(?:BANCA|BANCA EXAMINADORA|INSTITUIÇÃO RESPONSÁVEL|ORGANIZADORA)"
                    + "\\s*[:\\-]\\s*(.+)$",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);

    private static final Pattern BANCA_CONHECIDA = Pattern.compile(
            "(Fundação Carlos Chagas|FCC|VUNESP|CEBRASPE|CESPE|FGV|IBFC|"
                    + "IDECAN|INSTITUTO AOCP|QUADRIX|CONSULPLAN|INSTITUTO EXEMPLO)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern ROTULO_CARGO = Pattern.compile(
            "^\\s*(?:CARGO|FUNÇÃO|EMPREGO)\\s*[:\\-]\\s*(.+)$",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);

    private static final Pattern CURSO = Pattern.compile(
            "(Bacharelado em [A-Za-zÀ-ÿ]+|Curso de (?:Graduação em )?[A-Za-zÀ-ÿ]+)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern ANO = Pattern.compile("\\b(20\\d{2})\\b");

    public DadosConcurso extrair(DocumentModel documento) {
        if (documento == null || documento.vazio()) {
            return null;
        }

        String cabecalho = documento.cabecalho();

        String orgao = primeiroGrupoRotulado(ROTULO_ORGAO, cabecalho);
        if (orgao == null) {
            orgao = primeiroGrupo(INSTITUICAO, cabecalho);
        }
        if (orgao == null) {
            orgao = linhaInstitucional(cabecalho);
        }

        String banca = primeiroGrupoRotulado(ROTULO_BANCA, cabecalho);
        if (banca == null) {
            banca = primeiroGrupo(BANCA_CONHECIDA, cabecalho);
        }

        String cargo = primeiroGrupoRotulado(ROTULO_CARGO, cabecalho);
        if (cargo == null) {
            cargo = primeiroGrupo(CURSO, cabecalho);
        }

        Integer ano = anoDoDocumento(cabecalho);

        log.debug("Metadados | orgao={} | banca={} | cargo={} | ano={}",
                orgao, banca, cargo, ano);

        return new DadosConcurso(nomePadrao(ano), limpar(orgao), limpar(cargo),
                limpar(banca), ano);
    }

    private Integer anoDoDocumento(String cabecalho) {
        Matcher m = ANO.matcher(cabecalho);
        return m.find() ? Integer.parseInt(m.group(1)) : null;
    }

    private String nomePadrao(Integer ano) {
        return ano != null ? "Concurso " + ano : null;
    }

    /**
     * Última tentativa para o órgão: a primeira linha em maiúsculas do
     * documento costuma ser o nome da instituição.
     */
    private String linhaInstitucional(String cabecalho) {
        for (String linha : cabecalho.split("\\R")) {
            String l = linha.strip();
            if (l.length() >= 8 && l.length() <= 120
                    && l.equals(l.toUpperCase())
                    && l.matches("[A-ZÁÉÍÓÚÂÊÔÃÕÇ0-9 \\-/]+")) {
                return l;
            }
        }
        return null;
    }

    private String primeiroGrupoRotulado(Pattern p, String texto) {
        Matcher m = p.matcher(texto);
        return m.find() ? m.group(m.groupCount()) : null;
    }

    private String primeiroGrupo(Pattern p, String texto) {
        Matcher m = p.matcher(texto);
        return m.find() ? m.group() : null;
    }

    private String limpar(String valor) {
        if (valor == null) return null;
        String limpo = valor.replaceAll("\\s+", " ").strip();
        return limpo.isEmpty() ? null : limpo;
    }
}