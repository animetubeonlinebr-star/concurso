package br.com.marcosbassetto.concursos.domain.edital.dto;

public enum StatusExtracao {
    PROCESSADO,
    PARCIAL,
    BAIXA_CONFIANCA,
    NAO_IDENTIFICADO,
    ERRO;

    public boolean exigeAtencao() {
        return this != PROCESSADO;
    }

    public String getMensagem() {
        return switch (this) {
            case PROCESSADO -> "Estrutura extraída com sucesso. Revise antes de confirmar.";
            case PARCIAL ->
                    "A estrutura foi extraída em parte: não foi possível identificar todos os "
                            + "dados do concurso (órgão e ano). Confira as matérias antes de confirmar.";
            case BAIXA_CONFIANCA ->
                    "Poucas matérias foram encontradas. Confira se o edital era o arquivo correto "
                            + "e ajuste a estrutura na revisão.";
            case NAO_IDENTIFICADO ->
                    "Nenhuma matéria foi extraída: o edital pode não ter bloco de conteúdo "
                            + "programático, ou o arquivo está ilegível. Revise o arquivo enviado.";
            case ERRO -> "Falha ao extrair a estrutura do edital.";
        };
    }
}
