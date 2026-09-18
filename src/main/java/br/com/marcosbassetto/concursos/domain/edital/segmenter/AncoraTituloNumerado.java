package br.com.marcosbassetto.concursos.domain.edital.segmenter;

import br.com.marcosbassetto.concursos.domain.edital.interpretacao.documento.DocumentModel;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Âncora do edital que numera o agrupamento dentro de um capítulo.
 *
 * É o caso em que o conteúdo não vive em um anexo próprio, mas dentro de um
 * capítulo sobre as provas: "15.2.4 CONHECIMENTOS GERAIS" e
 * "15.2.5 CONHECIMENTOS ESPECÍFICOS". O título existe — só não é um título de
 * anexo.
 *
 * O que torna esta âncora segura é a exigência do nome do agrupamento depois
 * do número. Reconhecer "qualquer linha numerada" seria errado: transformaria
 * "1. DISPOSIÇÕES PRELIMINARES" e "2. DAS INSCRIÇÕES" em conteúdo
 * programático. O número é contexto; o nome é a evidência.
 *
 * Aceita o título numerado com ou sem sub-nível ("15.2.4" e "15.2"), porque
 * a profundidade da numeração é escolha de formatação de cada edital.
 */
@Component
public class AncoraTituloNumerado implements AncoraBlocoConteudo {

    /** Nome estável da âncora, usado pelo segmentador para escolher o critério de fim. */
    public static final String NOME = "TITULO_NUMERADO";

    private static final Pattern TITULO_NUMERADO = Pattern.compile(
            "(?im)^\\s*"
                    + "\\d{1,2}(?:\\.\\d{1,2}){1,3}"
                    + "\\s+"
                    + "(?:D[AEOS]\\s+)?"
                    + "CONHECIMENTOS\\s+"
                    + "(?:GERAIS|ESPEC[ÍI]FICOS|B[ÁA]SICOS|COMPLEMENTARES)"
                    + "[^\\n]*$");

    @Override
    public String nome() {
        return NOME;
    }

    @Override
    public int ordem() {
        return 30;
    }

    @Override
    public int encontrarInicio(DocumentModel documento) {
        if (documento == null || documento.vazio()) {
            return -1;
        }

        Matcher titulo = TITULO_NUMERADO.matcher(documento.textoLimpo());
        return titulo.find() ? titulo.end() : -1;
    }
}
