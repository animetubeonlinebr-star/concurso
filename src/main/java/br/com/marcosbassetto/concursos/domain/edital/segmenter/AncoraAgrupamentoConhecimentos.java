package br.com.marcosbassetto.concursos.domain.edital.segmenter;

import br.com.marcosbassetto.concursos.domain.edital.interpretacao.documento.DocumentModel;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Âncora do edital que dispensa o cabeçalho e declara apenas o agrupamento
 * de conhecimentos.
 *
 * É o caso dos editais enxutos, que começam o conteúdo direto em
 * "CONHECIMENTOS GERAIS". Exige a linha inteira: a mesma expressão aparece
 * dentro de quadros de provas ("Conhecimentos Gerais" numa tabela do capítulo
 * de provas), e aceitá-la em qualquer posição cortaria o bloco cedo demais.
 */
@Component
public class AncoraAgrupamentoConhecimentos implements AncoraBlocoConteudo {

    private static final Pattern CONHECIMENTOS = Pattern.compile(
            "(?im)^\\s*"
                    + "(?:PROVA\\s+OBJETIVA\\s+DE\\s+)?"
                    + "CONHECIMENTOS\\s+"
                    + "(?:GERAIS|ESPEC[ÍI]FICOS|B[ÁA]SICOS|COMPLEMENTARES)"
                    + "\\s*$");

    @Override
    public String nome() {
        return "AGRUPAMENTO_CONHECIMENTOS";
    }

    @Override
    public int ordem() {
        return 20;
    }

    @Override
    public int encontrarInicio(DocumentModel documento) {
        if (documento == null || documento.vazio()) {
            return -1;
        }

        Matcher conhecimentos = CONHECIMENTOS.matcher(documento.textoLimpo());
        return conhecimentos.find() ? conhecimentos.end() : -1;
    }
}
