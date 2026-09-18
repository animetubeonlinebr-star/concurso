package br.com.marcosbassetto.concursos.domain.edital.segmenter;

import br.com.marcosbassetto.concursos.domain.edital.interpretacao.documento.DocumentModel;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Âncora mais comum: o documento declara a seção de conteúdo programático.
 *
 * Reconhece "CONTEÚDO PROGRAMÁTICO", "CONTEÚDOS PROGRAMÁTICOS", "PROGRAMA DE
 * PROVA" e "PROGRAMAS DE PROVAS", com ou sem "ANEXO <romano>" antes, com "DO"
 * no meio e com numeração de capítulo.
 */
@Component
public class AncoraTituloExplicito implements AncoraBlocoConteudo {

    private static final Pattern TITULO = Pattern.compile(
            "(?im)^\\s*"
                    + "(?:ANEXO\\s+[IVXL]+\\s*[-–—:.]?\\s*)?"
                    + "(?:\\d{1,2}(?:\\.\\d{1,2})*\\s*)?"
                    + "(?:D[AEOS]\\s+)?"
                    + "(?:"
                    + "CONTE[ÚU]DOS?\\s+PROGRAM[ÁA]TICOS?"
                    + "|PROGRAMAS?\\s+DE\\s+PROVAS?"
                    + ")"
                    + "[^\\n]*$");

    @Override
    public String nome() {
        return "TITULO_EXPLICITO";
    }

    @Override
    public int ordem() {
        return 10;
    }

    @Override
    public int encontrarInicio(DocumentModel documento) {
        if (documento == null || documento.vazio()) {
            return -1;
        }

        Matcher titulo = TITULO.matcher(documento.textoLimpo());
        return titulo.find() ? titulo.end() : -1;
    }
}
