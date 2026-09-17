package br.com.marcosbassetto.concursos.domain.edital.segmenter;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Recorta o bloco de conteúdo programático do edital.
 *
 * O bloco é a fronteira do que pode virar matéria: fora dele o texto trata de
 * inscrição, prova, cronograma e contrato, e extrair dali produz estruturas
 * falsas.
 *
 * Os títulos variam mais do que parecia à primeira vista — "CONTEÚDO
 * PROGRAMÁTICO", "ANEXO II – DO CONTEÚDO PROGRAMÁTICO", "CONTEÚDOS
 * PROGRAMÁTICOS", "PROGRAMA DE PROVA" — e o mesmo vale para o fim do bloco.
 * Reconhecer só a forma mais comum era o que fazia editais reais caírem para
 * zero matérias.
 */
@Component
public class SegmentadorEdital {

    /**
     * Título explícito da seção de conteúdo. É a âncora confiável: quando
     * existe, corta-se por ele.
     */
    private static final Pattern TITULO_CONTEUDO = Pattern.compile(
            "(?im)^\\s*"
                    + "(?:ANEXO\\s+[IVXL]+\\s*[-–—:.]?\\s*)?"
                    + "(?:\\d{1,2}(?:\\.\\d{1,2})*\\s*)?"
                    + "(?:D[AEOS]\\s+)?"
                    + "(?:"
                    + "CONTE[ÚU]DOS?\\s+PROGRAM[ÁA]TICOS?"
                    + "|PROGRAMAS?\\s+DE\\s+PROVAS?"
                    + ")"
                    + "[^\\n]*$");

    /**
     * Agrupamento de conhecimentos, usado como âncora apenas quando não há
     * título explícito — editais mais enxutos dispensam o cabeçalho.
     *
     * Exige a linha inteira: a mesma expressão aparece dentro de quadros de
     * provas ("Conhecimentos Gerais" numa tabela do capítulo de provas), e
     * aceitá-la em qualquer posição cortaria o bloco cedo demais.
     */
    private static final Pattern CONHECIMENTOS = Pattern.compile(
            "(?im)^\\s*"
                    + "(?:PROVA\\s+OBJETIVA\\s+DE\\s+)?"
                    + "CONHECIMENTOS\\s+"
                    + "(?:GERAIS|ESPEC[ÍI]FICOS|B[ÁA]SICOS|COMPLEMENTARES)"
                    + "\\s*$");

    /**
     * Seções que encerram o conteúdo. Um novo anexo, o cronograma ou as
     * disposições finais marcam o fim do bloco.
     */
    private static final Pattern FIM_CONTEUDO = Pattern.compile(
            "(?im)^\\s*"
                    + "(?:"
                    + "ANEXO\\s+[IVXL]+\\b"
                    + "|DISPOSI[ÇC][ÕO]ES\\s+FINAIS"
                    + "|CRONOGRAMA"
                    + "|REFER[ÊE]NCIAS?\\s+BIBLIOGR[ÁA]FICAS?"
                    + "|BIBLIOGRAFIA"
                    + "|CALEND[ÁA]RIO"
                    + ")"
                    + "\\s*$");

    public SegmentoEdital segmentar(String textoCompleto) {
        if (textoCompleto == null || textoCompleto.isBlank()) {
            return new SegmentoEdital("", "", false);
        }

        String cabecalho = extrairCabecalho(textoCompleto);

        int posicaoInicio = encontrarInicio(textoCompleto);

        if (posicaoInicio < 0) {
            return new SegmentoEdital(cabecalho, "", false);
        }

        int posicaoFim = encontrarFim(textoCompleto, posicaoInicio);

        String conteudo = textoCompleto.substring(posicaoInicio, posicaoFim).trim();

        return new SegmentoEdital(cabecalho, conteudo, true);
    }

    /**
     * Prefere o título explícito; só recorre ao agrupamento de conhecimentos
     * quando o edital não traz o cabeçalho. Referências ao próprio anexo
     * ("...conforme o ANEXO II - CONTEÚDO PROGRAMÁTICO") aparecem no meio da
     * frase e não casam, porque o padrão exige início de linha.
     */
    private int encontrarInicio(String texto) {
        Matcher titulo = TITULO_CONTEUDO.matcher(texto);
        if (titulo.find()) {
            return titulo.end();
        }

        Matcher conhecimentos = CONHECIMENTOS.matcher(texto);
        return conhecimentos.find() ? conhecimentos.end() : -1;
    }

    private int encontrarFim(String texto, int posicaoInicio) {
        Matcher fim = FIM_CONTEUDO.matcher(texto);

        if (fim.find(posicaoInicio)) {
            return fim.start();
        }

        return texto.length();
    }

    private String extrairCabecalho(String texto) {
        int limite = Math.min(texto.length(), 3000);
        return texto.substring(0, limite);
    }

    public record SegmentoEdital(
            String cabecalho,
            String conteudoProgramatico,
            boolean encontrado
    ) {}
}