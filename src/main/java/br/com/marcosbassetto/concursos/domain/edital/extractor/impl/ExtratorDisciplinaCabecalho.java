package br.com.marcosbassetto.concursos.domain.edital.extractor.impl;

import br.com.marcosbassetto.concursos.domain.edital.domain.PadraoEdital;
import br.com.marcosbassetto.concursos.domain.edital.dto.DadosConcurso;
import br.com.marcosbassetto.concursos.domain.edital.dto.MateriaExtraida;
import br.com.marcosbassetto.concursos.domain.edital.dto.TopicoExtraido;
import br.com.marcosbassetto.concursos.domain.edital.extractor.ExtratorEditalBase;
import br.com.marcosbassetto.concursos.domain.edital.extractor.TextoPreProcessador;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ExtratorDisciplinaCabecalho extends ExtratorEditalBase {

    private static final Pattern RE_ANO = Pattern.compile("\\b(20\\d{2})\\b");

    private static final Pattern RE_ORGAO = Pattern.compile(
            "(?:CENTRO UNIVERSITÁRIO|UNIVERSIDADE|FACULDADE|INSTITUTO)[^\\n]{3,150}",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern RE_BANCA = Pattern.compile(
            "Fundação Carlos Chagas", Pattern.CASE_INSENSITIVE);

    private static final Pattern RE_INICIO_CONTEUDO = Pattern.compile(
            "(ANEXO\\s+II|CONTEÚDO\\s+PROGRAMÁTICO)", Pattern.CASE_INSENSITIVE);

    private static final Pattern RE_FIM_CONTEUDO = Pattern.compile(
            "ANEXO\\s+III", Pattern.CASE_INSENSITIVE);

    /**
     * Cabeçalho de disciplina.
     * Aceita: "| Nome |", "## Nome", "**Nome**".
     * Rejeita "## III. ..." e "## a) ..." (subtópicos romanos/alfabéticos).
     */
    private static final Pattern RE_CABECALHO_DISCIPLINA = Pattern.compile(
            "^\\s*(?:"
                    + "\\|\\s*([A-ZÁÉÍÓÚÂÊÔÃÕÇ][^|\\n]{2,80})\\s*\\|"
                    + "|##\\s+(?!I{1,3}\\.|IV\\.|V\\.|VI{0,3}\\.|IX\\.|X{1,3}\\.|[a-z]\\)\\s)"
                    + "([A-ZÁÉÍÓÚÂÊÔÃÕÇ][^\\n]{2,80})"
                    + "|\\*\\*([A-ZÁÉÍÓÚÂÊÔÃÕÇ][^\\n]{2,80})\\*\\*"
                    + ")\\s*$",
            Pattern.MULTILINE);

    private static final Pattern RE_BULLET = Pattern.compile(
            "^\\s*[▪■◆•\\-]\\s+(.+)$", Pattern.MULTILINE);

    private static final Pattern RE_ROMANO = Pattern.compile(
            "^\\s*([IVXLCDM]+)\\.\\s+(.+)$", Pattern.MULTILINE);

    public ExtratorDisciplinaCabecalho(TextoPreProcessador preProcessador) {
        super(preProcessador);
    }

    @Override
    public PadraoEdital padraoSuportado() {
        return PadraoEdital.DISCIPLINA_CABECALHO;
    }

    @Override
    protected DadosConcurso extrairDadosConcurso(String texto) {
        String cabecalho = texto.substring(0, Math.min(texto.length(), 3000));

        String orgao = primeiroMatch(cabecalho, RE_ORGAO, "Órgão não identificado");
        String anoStr = primeiroMatch(cabecalho, RE_ANO, null);
        Integer ano = anoStr != null ? Integer.parseInt(anoStr) : null;
        String banca = RE_BANCA.matcher(cabecalho).find()
                ? "Fundação Carlos Chagas"
                : "Não informada";

        String nome = "Processo Seletivo " + ano;
        String cargo = extrairCargo(cabecalho);

        return new DadosConcurso(nome, orgao.strip(), cargo, banca, ano);
    }

    @Override
    protected List<MateriaExtraida> extrairMaterias(String texto) {
        String bloco = extrairBlocoConteudo(texto);
        if (bloco.isBlank()) return List.of();

        List<MateriaExtraida> materias = new ArrayList<>();
        Matcher m = RE_CABECALHO_DISCIPLINA.matcher(bloco);

        int posAnterior = -1;
        String nomeAnterior = null;

        while (m.find()) {
            if (nomeAnterior != null) {
                String blocoMateria = bloco.substring(posAnterior, m.start());
                materias.add(new MateriaExtraida(nomeAnterior, extrairTopicos(blocoMateria)));
            }
            nomeAnterior = extrairNomeGrupo(m);
            posAnterior = m.end();
        }

        if (nomeAnterior != null) {
            String blocoFinal = bloco.substring(posAnterior);
            materias.add(new MateriaExtraida(nomeAnterior, extrairTopicos(blocoFinal)));
        }

        return materias;
    }

    @Override
    protected List<TopicoExtraido> extrairTopicos(String bloco) {
        List<TopicoExtraido> topicos = new ArrayList<>();

        Matcher mr = RE_ROMANO.matcher(bloco);
        if (mr.find()) {
            mr.reset();
            while (mr.find()) {
                topicos.add(new TopicoExtraido(mr.group(2).strip(), mr.group(1), null));
            }
        }

        Matcher mb = RE_BULLET.matcher(bloco);
        while (mb.find()) {
            String texto = mb.group(1).strip();
            if (texto.length() > 5) {
                topicos.add(new TopicoExtraido(texto, null, null));
            }
        }

        if (topicos.isEmpty()) {
            for (String linha : bloco.split("\\R")) {
                String l = linha.strip();
                if (l.length() > 40 && Character.isUpperCase(l.charAt(0))) {
                    topicos.add(new TopicoExtraido(l, null, null));
                    break;
                }
            }
        }

        return topicos;
    }

    @Override
    protected String extrairBlocoMateria(String texto, String nomeMateria) {
        return extrairBlocoConteudo(texto);
    }

    // ---- helpers ----

    private String extrairBlocoConteudo(String texto) {
        Matcher inicio = RE_INICIO_CONTEUDO.matcher(texto);
        if (!inicio.find()) return "";

        int de = inicio.end();
        int ate = texto.length();

        Matcher fim = RE_FIM_CONTEUDO.matcher(texto);
        if (fim.find(de)) {
            ate = fim.start();
        }

        return texto.substring(de, ate);
    }

    private String extrairCargo(String cabecalho) {
        Matcher m = Pattern.compile("Curso de ([A-ZÁÉÍÓÚÂÊÔÃÕÇ][a-záéíóúâêôãõç]+)",
                Pattern.CASE_INSENSITIVE).matcher(cabecalho);
        return m.find() ? "Curso de " + m.group(1) : "Geral";
    }

    private String primeiroMatch(String fonte, Pattern p, String fallback) {
        Matcher m = p.matcher(fonte);
        return m.find() ? m.group().strip() : fallback;
    }

    private String extrairNomeGrupo(Matcher m) {
        for (int i = 1; i <= m.groupCount(); i++) {
            if (m.group(i) != null) return m.group(i).strip();
        }
        return "";
    }
}
