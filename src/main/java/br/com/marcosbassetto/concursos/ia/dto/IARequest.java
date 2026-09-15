package br.com.marcosbassetto.concursos.ia.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public class IARequest {

    private final String prompt;
    private final String modelo;
    private final Double temperatura;
    private final Integer maxTokens;
    private final Map<String, Object> contexto;

    public IARequest(String prompt, String modelo, Double temperatura, Integer maxTokens, Map<String, Object> contexto) {
        this.prompt = prompt;
        this.modelo = modelo;
        this.temperatura = temperatura != null ? temperatura : 0.7;
        this.maxTokens = maxTokens != null ? maxTokens : 1000;
        this.contexto = contexto;
    }

    public static IARequestBuilder builder() {
        return new IARequestBuilder();
    }

    public static class IARequestBuilder {
        private String prompt;
        private String modelo;
        private Double temperatura = 0.7;
        private Integer maxTokens = 1000;
        private Map<String, Object> contexto;

        public IARequestBuilder prompt(String prompt) {
            this.prompt = prompt;
            return this;
        }

        public IARequestBuilder modelo(String modelo) {
            this.modelo = modelo;
            return this;
        }

        public IARequestBuilder temperatura(Double temperatura) {
            this.temperatura = temperatura;
            return this;
        }

        public IARequestBuilder maxTokens(Integer maxTokens) {
            this.maxTokens = maxTokens;
            return this;
        }

        public IARequestBuilder contexto(Map<String, Object> contexto) {
            this.contexto = contexto;
            return this;
        }

        public IARequest build() {
            return new IARequest(prompt, modelo, temperatura, maxTokens, contexto);
        }
    }
}
