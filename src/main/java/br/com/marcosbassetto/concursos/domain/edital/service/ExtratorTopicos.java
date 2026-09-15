package br.com.marcosbassetto.concursos.domain.edital.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class ExtratorTopicos {

    private static final Pattern PADRAO_TOPICO_NUMERADO =
            Pattern.compile("^\\s*\\d{1,3}[\\.\\)\\-]\\s+(.+)$");

    private static final Pattern PADRAO_TOPICO_LETRA =
            Pattern.compile("^\\s*[a-zA-Z][\\)\\.]\\s+(.+)$");

    /**
     * Retorna o texto do tópico extraído da linha, ou {@code null}
     * se a linha não representar um tópico válido.
     */
    public String extrairTopicoDaLinha(String linha) {
        if (linha == null || linha.isBlank()) return null;
        if (ExtracaoConstants.PADRAO_LINHA_TABELA.matcher(linha).find()) return null;

        Matcher m = PADRAO_TOPICO_NUMERADO.matcher(linha);
        if (m.matches()) return validar(m.group(1));

        m = PADRAO_TOPICO_LETRA.matcher(linha);
        if (m.matches()) return validar(m.group(1));

        if (linha.startsWith("-") || linha.startsWith("•") || linha.startsWith("*")) {
            return validar(linha.substring(1));
        }

        return null;
    }

    private String validar(String candidato) {
        if (candidato == null) return null;

        String limpo = candidato.trim().replaceAll("\\s+", " ");

        if (limpo.length() < ExtracaoConstants.MIN_TOPICO_LENGTH) return null;
        if (ExtracaoConstants.PADRAO_LINHA_TABELA.matcher(limpo).find()) return null;

        if (limpo.length() > ExtracaoConstants.MAX_TOPICO_LENGTH) {
            limpo = limpo.substring(0, ExtracaoConstants.MAX_TOPICO_LENGTH - 3) + "...";
        }
        return limpo;
    }
}
