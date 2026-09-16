package br.com.marcosbassetto.concursos.domain.edital.extractor.impl;

import br.com.marcosbassetto.concursos.domain.edital.domain.PadraoEdital;
import br.com.marcosbassetto.concursos.domain.edital.dto.DadosConcurso;
import br.com.marcosbassetto.concursos.domain.edital.dto.MateriaExtraida;
import br.com.marcosbassetto.concursos.domain.edital.dto.TopicoExtraido;
import br.com.marcosbassetto.concursos.domain.edital.extractor.ExtratorEditalBase;
import br.com.marcosbassetto.concursos.domain.edital.extractor.PadroesLista;
import br.com.marcosbassetto.concursos.domain.edital.extractor.TextoPreProcessador;
import br.com.marcosbassetto.concursos.domain.edital.segmenter.SegmentadorEdital;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Último recurso: edital sem estrutura reconhecida. Ainda assim extrai os
 * dados cadastrais capturáveis e tenta agrupar as disciplinas por linha em
 * maiúsculas, para não devolver estrutura vazia.
 */
@Slf4j
@Component
public class ExtratorFallback extends ExtratorEditalBase {

    private static final Pattern CURSO_PATTERN = Pattern.compile(
            "(?i)(Curso de Graduação em [A-Za-zÀ-ÿ]+|Bacharelado em [A-Za-zÀ-ÿ]+|Curso:\\s*[A-Za-zÀ-ÿ]+)");

    private static final Pattern VAGAS_PATTERN = Pattern.compile(
            "(?i)([0-9]+)\\s*(?:vagas|\\(uma\\)\\s*vaga|vaga)");

    private static final Pattern TAXA_PATTERN = Pattern.compile(
            "(?i)R\\$\\s*([0-9]{1,3}(?:\\.[0-9]{3})*,[0-9]{2})");

    private static final Pattern DATAS_CHAVE_PATTERN = Pattern.compile(
            "(?i)(inscriç[ãa]o|provas?|gabarito).{0,100}?(\\d{2}/\\d{2}/\\d{4})");

    private static final Pattern RE_ANO = Pattern.compile("\\b(20\\d{2})\\b");

    private static final Pattern RE_ORGAO = Pattern.compile(
            "(?:CENTRO UNIVERSITÁRIO|UNIVERSIDADE|FACULDADE|INSTITUTO|"
                    + "TRIBUNAL|MINISTÉRIO|PREFEITURA|CÂMARA|ASSEMBLEIA|"
                    + "COMPANHIA|EMPRESA|FUNDAÇÃO)[^\\n]{3,120}",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern RE_BANCA_EXPLICITA = Pattern.compile(
            "(?:BANCA|BANCA EXAMINADORA)\\s*[:\\-]?\\s*([^\\n,;]{3,60})",
            Pattern.CASE_INSENSITIVE);

    private final SegmentadorEdital segmentador;

    public ExtratorFallback(TextoPreProcessador preProcessador, SegmentadorEdital segmentador) {
        super(preProcessador);
        this.segmentador = segmentador;
    }

    @Override
    public PadraoEdital padraoSuportado() {
        return PadraoEdital.LISTA_SIMPLES;
    }

    @Override
    protected DadosConcurso extrairDadosConcurso(String texto) {
        String cabecalho = segmentador.segmentar(texto).cabecalho();

        String curso = capturarPrimeiro(CURSO_PATTERN, cabecalho, 0);
        String vagas = capturarPrimeiro(VAGAS_PATTERN, cabecalho, 1);
        String taxa = capturarPrimeiro(TAXA_PATTERN, cabecalho, 1);
        String dataProva = capturarPrimeiro(DATAS_CHAVE_PATTERN, cabecalho, 2);
        String orgao = capturarPrimeiro(RE_ORGAO, cabecalho, 0);
        String banca = capturarPrimeiro(RE_BANCA_EXPLICITA, cabecalho, 1);
        String anoStr = capturarPrimeiro(RE_ANO, cabecalho, 1);
        Integer ano = anoStr != null ? Integer.parseInt(anoStr) : null;

        log.debug("ExtratorFallback | curso={} vagas={} taxa=R$ {} dataProva={}",
                curso, vagas, taxa, dataProva);

        String nome = curso != null
                ? curso
                : (ano != null ? "Concurso " + ano : "Concurso não identificado");

        return new DadosConcurso(
                nome.strip(),
                orgao != null ? orgao.strip() : null,
                curso,
                banca != null ? banca.strip() : null,
                ano);
    }

    @Override
    protected List<MateriaExtraida> extrairMaterias(String texto) {
        SegmentadorEdital.SegmentoEdital segmento = segmentador.segmentar(texto);
        String bloco = segmento.conteudoProgramatico();

        if (bloco == null || bloco.isBlank()) {
            bloco = texto;
        }

        List<MateriaExtraida> materias = PadroesLista.extrairPorCabecalho(bloco);
        if (materias.isEmpty()) {
            materias = PadroesLista.extrairPorCabecalhoMaiusculo(bloco, "Conteúdo Programático");
        }

        log.debug("ExtratorFallback | materias={}", materias.size());

        return materias;
    }

    @Override
    protected List<TopicoExtraido> extrairTopicos(String bloco) {
        return PadroesLista.extrairTopicos(bloco);
    }

    private String capturarPrimeiro(Pattern pattern, String texto, int grupo) {
        Matcher m = pattern.matcher(texto);
        if (!m.find()) {
            return null;
        }
        String valor = m.group(grupo);
        return valor != null ? valor.strip() : null;
    }
}
