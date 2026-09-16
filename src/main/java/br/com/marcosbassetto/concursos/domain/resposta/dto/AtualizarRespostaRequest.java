package br.com.marcosbassetto.concursos.domain.resposta.dto;

import jakarta.validation.constraints.Size;

/**
 * Atualização de uma resposta existente.
 * A questão do simulado permanece a mesma.
 */
public record AtualizarRespostaRequest(

        @Size(max = 10, message = "A alternativa selecionada deve ter no máximo 10 caracteres")
        String alternativaSelecionada,

        @Size(max = 20000, message = "A resposta textual deve ter no máximo 20000 caracteres")
        String respostaTexto
) {
}