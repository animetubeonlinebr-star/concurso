package br.com.marcosbassetto.concursos.domain.questao.domain;

/**
 * Alternativa de uma questão objetiva, persistida como item do JSON
 * {@code alternativas} da questão.
 */
public record Alternativa(String letra, String texto) {

    public boolean possuiLetra(String letraComparada) {
        return letra != null && letraComparada != null && letra.equalsIgnoreCase(letraComparada);
    }
}