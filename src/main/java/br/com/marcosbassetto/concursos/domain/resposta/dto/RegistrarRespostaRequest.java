package br.com.marcosbassetto.concursos.domain.resposta.dto;

import jakarta.validation.constraints.Size;

/**
 * Registro inicial de uma resposta.
 *
 * A validação de tipo (alternativa x texto) depende do tipo da questão e
 * é feita no service, que tem acesso ao enunciado da questão.
 */
public record RegistrarRespostaRequest(

        @Size(max = 10, message = "A alternativa selecionada deve ter no máximo 10 caracteres")
        String alternativaSelecionada,

        @Size(max = 20000, message = "A resposta textual deve ter no máximo 20000 caracteres")
        String respostaTexto
) {
}