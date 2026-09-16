package br.com.marcosbassetto.concursos.domain.correcao.domain;

/**
 * Resultado objetivo de uma questão após a correção.
 *
 * NAO_RESPONDIDA é distinto de INCORRETA: em provas de concurso a questão em
 * branco normalmente não penaliza, então a distinção afeta pontuação e
 * estatísticas de desempenho.
 */
public enum ResultadoCorrecao {
    CORRETA,
    INCORRETA,
    NAO_RESPONDIDA
}