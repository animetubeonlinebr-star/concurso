package br.com.marcosbassetto.concursos.domain.edital.extractor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.stream.Collectors;

@Slf4j
@Component
public class TextoPreProcessador {

    private static final double LIMIAR_LIXO = 0.7;
    private static final int TAMANHO_MINIMO_SUSPEITO = 20;

    public String limpar(String texto) {
        if (texto == null || texto.isBlank()) {
            return "";
        }

        String[] linhas = texto.split("\\R");
        int total = linhas.length;

        String resultado = Arrays.stream(linhas)
                .filter(linha -> !isLixoEncoding(linha))
                .map(String::stripTrailing)
                .collect(Collectors.joining("\n"))
                .replaceAll("\n{3,}", "\n\n")
                .strip();

        int restantes = resultado.isEmpty() ? 0 : resultado.split("\\R").length;
        long removidas = total - restantes;

        if (removidas > 0) {
            log.debug("TextoPreProcessador | removidas={} de {}", removidas, total);
        }

        return resultado;
    }

    private boolean isLixoEncoding(String linha) {
        String t = linha.strip();
        if (t.length() < TAMANHO_MINIMO_SUSPEITO) {
            return false;
        }

        long suspeitos = t.chars()
                .filter(c -> c >= 0x00A0 && c <= 0x00FF)
                .count();

        double proporcao = (double) suspeitos / t.length();
        if (proporcao < LIMIAR_LIXO) {
            return false;
        }

        String normalizado = Normalizer.normalize(t, Normalizer.Form.NFC);
        return !normalizado.matches(".*[áàâãéêíóôõúçÁÀÂÃÉÊÍÓÔÕÚÇ].*");
    }
}
