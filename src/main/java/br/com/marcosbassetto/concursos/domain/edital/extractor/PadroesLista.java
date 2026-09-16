package br.com.marcosbassetto.concursos.domain.edital.extractor;

import br.com.marcosbassetto.concursos.domain.edital.dto.MateriaExtraida;
import br.com.marcosbassetto.concursos.domain.edital.dto.TopicoExtraido;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Padrões de lista compartilhados pelos extratores.
 *
 * Antes viviam privados em {@code ExtratorDisciplinaCabecalho}; foram
 * extraídos para cá porque {@code ExtratorPadrao} e {@code ExtratorFallback}
 * precisam exatamente das mesmas regras de marcador.
 */
public final class PadroesLista {

    private PadroesLista() {
    }

    /** Marcadores: "▪ Item", "■ Item", "◆ Item", "• Item", "- Item". */
    public static final Pattern BULLET = Pattern.compile(
            "^\\s*[▪■◆•\\-]\\s+(.+)$", Pattern.MULTILINE);

    /** Itens romanos: "I. Item", "IV. Item". */
    public static final Pattern ROMANO = Pattern.compile(
            "^\\s*([IVXLCDM]{1,6})\\.\\s+(.+)$", Pattern.MULTILINE);

    /** Itens numerados: "1. Item", "12) Item". */
    public static final Pattern NUMERADO = Pattern.compile(
            "^\\s*(\\d{1,2})[.)]\\s+(.+)$", Pattern.MULTILINE);

    /**
     * Cabeçalho de disciplina em Markdown/tabela: "| Nome |", "## Nome",
     * "**Nome**".
     *
     * Rejeita "## III. ..." e "## a) ..." para não confundir subtópico
     * romano/alfabético com disciplina.
     */
    public static final Pattern CABECALHO_DISCIPLINA = Pattern.compile(
            "^\\s*(?:"
                    + "\\|\\s*([A-ZÁÉÍÓÚÂÊÔÃÕÇ][^|\\n]{2,80})\\s*\\|"
                    + "|##\\s+(?!I{1,3}\\.|IV\\.|V\\.|VI{0,3}\\.|IX\\.|X{1,3}\\.|[a-z]\\)\\s)"
                    + "([A-ZÁÉÍÓÚÂÊÔÃÕÇ][^\\n]{2,80})"
                    + "|\\*\\*([A-ZÁÉÍÓÚÂÊÔÃÕÇ][^\\n]{2,80})\\*\\*"
                    + ")\\s*$",
            Pattern.MULTILINE);

    /**
     * Linha inteira em maiúsculas, candidata a cabeçalho de disciplina em
     * editais sem Markdown (ex.: "LÍNGUA PORTUGUESA").
     */
    public static final Pattern CABECALHO_MAIUSCULO = Pattern.compile(
            "^\\s*([A-ZÁÉÍÓÚÂÊÔÃÕÇ][A-ZÁÉÍÓÚÂÊÔÃÕÇ\\s]{4,80}?)\\s*$",
            Pattern.MULTILINE);

    /**
     * Extrai os itens de lista de um bloco: romanos e numerados têm
     * precedência sobre bullets, que são mais genéricos.
     */
    public static List<TopicoExtraido> extrairTopicos(String bloco) {
        if (bloco == null || bloco.isBlank()) {
            return List.of();
        }

        List<TopicoExtraido> topicos = new ArrayList<>();

        Matcher mr = ROMANO.matcher(bloco);
        while (mr.find()) {
            topicos.add(new TopicoExtraido(mr.group(2).strip(), mr.group(1), null));
        }

        if (topicos.isEmpty()) {
            Matcher mn = NUMERADO.matcher(bloco);
            while (mn.find()) {
                topicos.add(new TopicoExtraido(mn.group(2).strip(), mn.group(1), null));
            }
        }

        Matcher mb = BULLET.matcher(bloco);
        while (mb.find()) {
            topicos.add(new TopicoExtraido(mb.group(1).strip(), null, null));
        }

        return topicos;
    }

    /**
     * Quebra o bloco de conteúdo em matérias a partir dos cabeçalhos de
     * disciplina encontrados, enriquecendo cada uma com os tópicos que
     * aparecem até o próximo cabeçalho.
     */
    public static List<MateriaExtraida> extrairPorCabecalho(String bloco) {
        if (bloco == null || bloco.isBlank()) {
            return List.of();
        }

        List<MateriaExtraida> materias = new ArrayList<>();
        Matcher m = CABECALHO_DISCIPLINA.matcher(bloco);

        int posAnterior = -1;
        String nomeAnterior = null;

        while (m.find()) {
            if (nomeAnterior != null) {
                materias.add(new MateriaExtraida(
                        nomeAnterior, extrairTopicos(bloco.substring(posAnterior, m.start()))));
            }
            nomeAnterior = primeiroGrupoPreenchido(m);
            posAnterior = m.end();
        }

        if (nomeAnterior != null) {
            materias.add(new MateriaExtraida(nomeAnterior, extrairTopicos(bloco.substring(posAnterior))));
        }

        return materias;
    }

    /**
     * Agrupa itens de lista sob as linhas em maiúsculas, para editais sem
     * formatação Markdown. Se nenhuma linha em maiúsculas existir, devolve
     * uma única matéria com todo o conteúdo — melhor uma matéria real do que
     * perder a extração inteira.
     */
    public static List<MateriaExtraida> extrairPorCabecalhoMaiusculo(String bloco,
                                                                     String nomePadrao) {
        if (bloco == null || bloco.isBlank()) {
            return List.of();
        }

        List<MateriaExtraida> materias = new ArrayList<>();
        Matcher m = CABECALHO_MAIUSCULO.matcher(bloco);

        int posAnterior = -1;
        String nomeAnterior = null;

        while (m.find()) {
            String nome = m.group(1).strip();
            if (!pareceDisciplina(nome)) {
                continue;
            }
            if (nomeAnterior != null) {
                materias.add(new MateriaExtraida(
                        nomeAnterior, extrairTopicos(bloco.substring(posAnterior, m.start()))));
            }
            nomeAnterior = nome;
            posAnterior = m.end();
        }

        if (nomeAnterior != null) {
            materias.add(new MateriaExtraida(nomeAnterior, extrairTopicos(bloco.substring(posAnterior))));
        }

        if (materias.isEmpty()) {
            List<TopicoExtraido> topicos = extrairTopicos(bloco);
            if (!topicos.isEmpty()) {
                return List.of(new MateriaExtraida(nomePadrao, topicos));
            }
        }

        return materias;
    }

    public static String primeiroGrupoPreenchido(Matcher m) {
        for (int i = 1; i <= m.groupCount(); i++) {
            if (m.group(i) != null) {
                return m.group(i).strip();
            }
        }
        return "";
    }

    /**
     * Descarta linhas em maiúsculas que são ruído de edital (títulos de
     * seção, avisos) e não nome de disciplina.
     */
    private static boolean pareceDisciplina(String nome) {
        String n = nome.strip();
        if (n.length() < 4 || n.length() > 80) return false;

        int palavras = n.split("\\s+").length;
        if (palavras > 6) return false;

        String upper = n.toUpperCase();
        return !upper.contains("ANEXO")
                && !upper.contains("EDITAL")
                && !upper.contains("CONTEUDO PROGRAMATICO")
                && !upper.contains("CONTEÚDO PROGRAMÁTICO")
                && !upper.contains("CRONOGRAMA")
                && !upper.contains("DISPOSICOES")
                && !upper.contains("DISPOSIÇÕES")
                && !upper.contains("PROCESSO SELETIVO")
                && !upper.contains("CONCURSO PUBLICO")
                && !upper.contains("CONCURSO PÚBLICO")
                && !upper.contains("GABARITO")
                && !upper.contains("RESULTADO")
                && !upper.contains("INSCRIC")
                && !upper.contains("INSCRIÇ");
    }
}