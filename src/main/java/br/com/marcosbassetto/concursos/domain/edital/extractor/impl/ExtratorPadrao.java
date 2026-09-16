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
 * Extrator genérico para edital bem formatado: delimitado por
 * "CONTEÚDO PROGRAMÁTICO"/"PROGRAMA DE PROVA" e com disciplinas em
 * cabeçalho Markdown/tabela.
 */
@Slf4j
@Component
public class ExtratorPadrao extends ExtratorEditalBase {

    private static final Pattern RE_ANO = Pattern.compile("\\b(20\\d{2})\\b");

    private static final Pattern RE_ORGAO = Pattern.compile(
            "^\\s*(?:ÓRGÃO|ORGÃO|ENTIDADE)\\s*[:\\-]\\s*(.+)$",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);

    private static final Pattern RE_ORG_INSTITUICAO = Pattern.compile(
            "(?:CENTRO UNIVERSITÁRIO|UNIVERSIDADE|FACULDADE|INSTITUTO|"
                    + "TRIBUNAL|MINISTÉRIO|PREFEITURA|CÂMARA|ASSEMBLEIA|"
                    + "COMPANHIA|EMPRESA|FUNDAÇÃO)[^\\n]{3,120}",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern RE_BANCA = Pattern.compile(
            "^\\s*(?:BANCA|BANCA EXAMINADORA|INSTITUIÇÃO RESPONSÁVEL)\\s*[:\\-]\\s*(.+)$",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);

    private static final Pattern RE_CARGO = Pattern.compile(
            "^\\s*(?:CARGO|FUNÇÃO)\\s*[:\\-]\\s*(.+)$",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);

    private static final Pattern RE_TITULO = Pattern.compile(
            "^\\s*((?:CONCURSO|PROCESSO SELETIVO|EDITAL)[^\\n]{5,150})$",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);

    private final SegmentadorEdital segmentador;

    public ExtratorPadrao(TextoPreProcessador preProcessador, SegmentadorEdital segmentador) {
        super(preProcessador);
        this.segmentador = segmentador;
    }

    @Override
    public PadraoEdital padraoSuportado() {
        return PadraoEdital.ENFASE_BLOCOS;
    }

    @Override
    protected DadosConcurso extrairDadosConcurso(String texto) {
        // O cabeçalho fica fora do bloco de conteúdo programático, então é
        // lido do segmento (primeiros caracteres do edital).
        String cabecalho = segmentador.segmentar(texto).cabecalho();

        String titulo = primeiroMatch(cabecalho, RE_TITULO);
        String orgao = primeiroMatch(cabecalho, RE_ORGAO);
        if (orgao == null) {
            orgao = primeiroMatch(cabecalho, RE_ORG_INSTITUICAO);
        }
        String banca = primeiroMatch(cabecalho, RE_BANCA);
        String cargo = primeiroMatch(cabecalho, RE_CARGO);
        String anoStr = primeiroMatch(cabecalho, RE_ANO);
        Integer ano = anoStr != null ? Integer.parseInt(anoStr) : null;

        String nome = titulo != null
                ? titulo
                : (ano != null ? "Concurso " + ano : "Concurso não identificado");

        return new DadosConcurso(
                nome.strip(),
                orgao != null ? orgao.strip() : null,
                cargo != null ? cargo.strip() : null,
                banca != null ? banca.strip() : null,
                ano);
    }

    @Override
    protected List<MateriaExtraida> extrairMaterias(String texto) {
        SegmentadorEdital.SegmentoEdital segmento = segmentador.segmentar(texto);
        String bloco = segmento.conteudoProgramatico();

        if (bloco == null || bloco.isBlank()) {
            // Sem bloco delimitado, usa o texto inteiro para não descartar
            // editais que listam disciplinas sem o título "CONTEÚDO
            // PROGRAMÁTICO".
            bloco = texto;
        }

        List<MateriaExtraida> materias = PadroesLista.extrairPorCabecalho(bloco);

        if (materias.isEmpty()) {
            materias = PadroesLista.extrairPorCabecalhoMaiusculo(
                    bloco, "Conteúdo Programático");
        }

        log.debug("ExtratorPadrao | blocoEncontrado={} | materias={}",
                segmento.encontrado(), materias.size());

        return materias;
    }

    @Override
    protected List<TopicoExtraido> extrairTopicos(String bloco) {
        return PadroesLista.extrairTopicos(bloco);
    }

    private String primeiroMatch(String fonte, Pattern p) {
        Matcher m = p.matcher(fonte);
        return m.find() ? m.group(m.groupCount()) : null;
    }
}
