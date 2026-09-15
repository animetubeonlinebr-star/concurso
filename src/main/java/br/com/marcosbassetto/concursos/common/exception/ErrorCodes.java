package br.com.marcosbassetto.concursos.common.exception;

public final class ErrorCodes {

    private ErrorCodes() {
    }

    public static final String RESOURCE_NOT_FOUND = "RESOURCE_NOT_FOUND";

    public static final String MATERIA_DUPLICADA = "MATERIA_DUPLICADA";
    public static final String TOPICO_DUPLICADO = "TOPICO_DUPLICADO";
    public static final String CONCURSO_DUPLICADO = "CONCURSO_DUPLICADO";
    public static final String QUESTOES_INSUFICIENTES = "QUESTOES_INSUFICIENTES";
    public static final String ESTADO_INVALIDO = "ESTADO_INVALIDO";
    public static final String SIMULADO_FINALIZADO = "SIMULADO_FINALIZADO";
    public static final String SIMULADO_CANCELADO = "SIMULADO_CANCELADO";
    public static final String QUESTAO_SEM_ALTERNATIVAS = "QUESTAO_SEM_ALTERNATIVAS";
    public static final String ALTERNATIVA_INEXISTENTE = "ALTERNATIVA_INEXISTENTE";
    public static final String RESPOSTA_INVALIDA = "RESPOSTA_INVALIDA";

    public static final String INVALID_CREDENTIALS = "INVALID_CREDENTIALS";
    public static final String ACCESS_DENIED = "ACCESS_DENIED";
    public static final String TOKEN_EXPIRADO = "TOKEN_EXPIRADO";
    public static final String TOKEN_INVALIDO = "TOKEN_INVALIDO";

    public static final String ARQUIVO_INVALIDO = "ARQUIVO_INVALIDO";
    public static final String ARQUIVO_MUITO_GRANDE = "ARQUIVO_MUITO_GRANDE";
    public static final String TIPO_ARQUIVO_NAO_SUPORTADO = "TIPO_ARQUIVO_NAO_SUPORTADO";

    public static final String IA_INDISPONIVEL = "IA_INDISPONIVEL";
    public static final String IA_TIMEOUT = "IA_TIMEOUT";
    public static final String IA_LIMITE_ATINGIDO = "IA_LIMITE_ATINGIDO";
    public static final String IA_RESPOSTA_INVALIDA = "IA_RESPOSTA_INVALIDA";

    public static final String VALIDATION_ERROR = "VALIDATION_ERROR";
    public static final String DADOS_INVALIDOS = "DADOS_INVALIDOS";

    public static final String CONFLITO_ESTADO = "CONFLITO_ESTADO";

    public static final String ERRO_INTERNO = "ERRO_INTERNO";
}
