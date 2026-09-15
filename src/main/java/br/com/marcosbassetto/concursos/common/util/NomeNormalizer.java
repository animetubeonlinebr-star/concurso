package br.com.marcosbassetto.concursos.common.util;

import java.text.Normalizer;


public final class NomeNormalizer {

    private NomeNormalizer() {}

    public static String normalizar(String nome) {
        if (nome == null || nome.isBlank()) {
            return "";
        }
        return Normalizer.normalize(nome, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^a-zA-Z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim()
                .toUpperCase();
    }
}