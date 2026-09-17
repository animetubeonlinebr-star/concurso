package br.com.marcosbassetto.concursos.domain.edital.interpretacao.documento;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Modelo intermediário do documento: o PDF já virou texto, mas ainda não há
 * nenhuma decisão sobre o que é matéria ou tópico.
 *
 * Existe para que a interpretação (perfil, classificação, validação) trabalhe
 * sobre uma representação estável e testável, independente do PDFBox: trocar
 * a biblioteca de PDF não deve mexer em nenhuma regra de negócio.
 */
public record DocumentModel(
        String textoLimpo,
        String cabecalho,
        List<String> linhas
) {

    private static final int LIMITE_CABECALHO = 3000;

    /** Linha em maiúsculas, candidata a cabeçalho ou disciplina. */
    private static final Pattern LINHA_MAIUSCULA = Pattern.compile(
            "^[A-ZÁÉÍÓÚÂÊÔÃÕÇ][A-ZÁÉÍÓÚÂÊÔÃÕÇ\\s\\-/]{3,80}$");

    public DocumentModel {
        if (textoLimpo == null) textoLimpo = "";
        if (cabecalho == null) cabecalho = "";
        if (linhas == null) linhas = List.of();
    }

    public static DocumentModel de(String textoLimpo) {
        if (textoLimpo == null || textoLimpo.isBlank()) {
            return new DocumentModel("", "", List.of());
        }

        List<String> linhas = new ArrayList<>();
        for (String linha : textoLimpo.split("\\R")) {
            linhas.add(linha.strip());
        }

        String cabecalho = textoLimpo.substring(0, Math.min(textoLimpo.length(), LIMITE_CABECALHO));

        return new DocumentModel(textoLimpo, cabecalho, List.copyOf(linhas));
    }

    /**
     * Quantas vezes cada linha em maiúsculas aparece no documento.
     *
     * Um cabeçalho institucional se repete a cada página; uma disciplina
     * aparece uma vez. É o sinal mais barato e determinístico para separar
     * "UNIVERSIDADE ESTADUAL PAULISTA" de "LÍNGUA PORTUGUESA".
     */
    public Map<String, Integer> frequenciaLinhasMaiusculas() {
        Map<String, Integer> frequencia = new HashMap<>();

        for (String linha : linhas) {
            if (linha.isEmpty() || !LINHA_MAIUSCULA.matcher(linha).matches()) {
                continue;
            }
            frequencia.merge(linha, 1, Integer::sum);
        }

        return frequencia;
    }

    /** Começo do documento em minúsculas, usado para reconhecer o perfil. */
    public String assinatura() {
        return cabecalho.toLowerCase();
    }

    public boolean vazio() {
        return textoLimpo.isBlank();
    }
}