package br.com.marcosbassetto.concursos.domain.edital.service;

import java.util.regex.Pattern;


public final class ExtracaoConstants {

    private ExtracaoConstants() {
    }

    public static final int MAX_TOPICO_LENGTH = 300;

    public static final int MIN_TOPICO_LENGTH = 5;

    public static final int MAX_TOPICOS_POR_MATERIA = 100;



    public static final int MIN_MATERIA_LENGTH = 4;

    public static final int MAX_MATERIA_LENGTH = 120;

    public static final int MAX_PALAVRAS_MATERIA = 8;

    public static final int MAX_MATERIAS = 50;


    public static final int MAX_LINHAS_PROCESSADAS = 5000;


    public static final Pattern PADRAO_LINHA_TABELA = Pattern.compile(
            "(?i)(" +
                    "R\\$\\s*[\\d.,]+" +                                 // valores em reais
                    "|\\bCR\\b" +                                        // "CR" sozinho
                    "|\\b(Vagas?|Vencimento|Requisitos|Benefícios|Inscrição|" +
                    "Carga\\s+Horária|Vale\\s+Transporte|Assistência\\s+Médica|" +
                    "V\\.\\s*Def|V\\.\\s*Negros?)\\b" +
                    "|Página\\s*\\d+" +
                    "|^\\s*\\d+\\s*$" +
                    "|\\d{2}/\\d{2}/\\d{4}" +
                    ")"
    );
}
