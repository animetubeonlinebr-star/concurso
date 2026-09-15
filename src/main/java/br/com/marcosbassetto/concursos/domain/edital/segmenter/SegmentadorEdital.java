package br.com.marcosbassetto.concursos.domain.edital.segmenter;

import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class SegmentadorEdital {

    private static final Pattern INICIO_CONTEUDO = Pattern.compile(
            "(?im)^\\s*" +
                    "(?:ANEXO\\s+[IVX]+\\s*[-–—:.]?\\s*)?" +
                    "(?:" +
                    "CONTE[ÚU]DOS?\\s+PROGRAM[ÁA]TICOS?" +
                    "|PROGRAMA\\s+DE\\s+PROVA" +
                    "|CONTE[ÚU]DO\\s+PROGRAM[ÁA]TICO" +
                    ")" +
                    "\\s*$"
    );

    private static final Pattern FIM_CONTEUDO = Pattern.compile(
            "(?im)^\\s*" +
                    "(?:" +
                    "ANEXO\\s+[IVX]+\\b" +
                    "|DISPOSI[ÇC][ÕO]ES\\s+FINAIS" +
                    "|CRONOGRAMA" +
                    "|REFER[ÊE]NCIAS?\\s+BIBLIOGR[ÁA]FICAS?" +
                    "|BIBLIOGRAFIA" +
                    "|ANEXO\\s+[IVX]+\\s*[-–—:]\\s*ATRIBUI[ÇC][ÕO]ES" +
                    ")" +
                    "\\s*$"
    );

    public SegmentoEdital segmentar(String textoCompleto) {
        if (textoCompleto == null || textoCompleto.isBlank()) {
            return new SegmentoEdital("", "", false);
        }

        String cabecalho = extrairCabecalho(textoCompleto);

        Matcher inicio = INICIO_CONTEUDO.matcher(textoCompleto);
        if (!inicio.find()) {
            return new SegmentoEdital(cabecalho, textoCompleto, false);
        }

        int posicaoInicio = inicio.end();

        Matcher fim = FIM_CONTEUDO.matcher(textoCompleto);
        int posicaoFim = textoCompleto.length();

        if (fim.find(posicaoInicio)) {
            posicaoFim = fim.start();
        }

        String conteudo = textoCompleto.substring(posicaoInicio, posicaoFim).trim();

        return new SegmentoEdital(cabecalho, conteudo, true);
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
