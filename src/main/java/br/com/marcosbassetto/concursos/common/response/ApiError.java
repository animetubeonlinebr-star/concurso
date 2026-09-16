package br.com.marcosbassetto.concursos.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiError {

    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String code;
    private String message;
    private String path;
    private Map<String, String> validationErrors;

    /**
     * Dados extra que o cliente precisa para reagir ao erro. Em
     * EDITAL_JA_IMPORTADO carrega o concursoId existente, para o frontend
     * oferecer "abrir o concurso" em vez de só exibir a mensagem.
     */
    private Map<String, Object> details;
}
