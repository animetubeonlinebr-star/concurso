package br.com.marcosbassetto.concursos.domain.edital.dto;

import br.com.marcosbassetto.concursos.domain.edital.domain.StatusProcessamento;

/**
 * Estado atual do processamento, consumido por polling na tela
 * {@code /concursos/{id}/processando}.
 *
 * {@code progresso} é aproximado: a extração determinística não reporta
 * progresso incremental, então o valor é derivado do status.
 */
public record StatusProcessamentoResponse(
        Long concursoId,
        StatusProcessamento status,
        int progresso,
        String mensagem,
        String mensagemErro
) {

    public static StatusProcessamentoResponse de(Long concursoId,
                                                 StatusProcessamento status,
                                                 String mensagemErro) {
        return new StatusProcessamentoResponse(
                concursoId,
                status,
                progressoDe(status),
                status != null ? status.getDescricao() : null,
                mensagemErro
        );
    }

    private static int progressoDe(StatusProcessamento status) {
        if (status == null) return 0;
        return switch (status) {
            case RECEBIDO -> 10;
            case PROCESSANDO -> 50;
            case AGUARDANDO_REVISAO -> 80;
            case CONFIRMADO -> 100;
            case ERRO -> 100;
        };
    }
}