package br.com.marcosbassetto.concursos.application.edital;

/**
 * Publicado quando uma importação é registrada e pode ser processada.
 *
 * O upload não espera a extração: o listener assíncrono (ver
 * {@code ProcessarEditalUseCase}) assume a partir daqui, e o frontend
 * acompanha por polling em {@code /processamento}.
 */
public record ImportacaoIniciadaEvent(Long concursoId, Long importacaoId) {
}
