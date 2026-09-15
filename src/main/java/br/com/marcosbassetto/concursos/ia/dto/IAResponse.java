package br.com.marcosbassetto.concursos.ia.dto;

import lombok.Getter;

@Getter
public class IAResponse {

    private final boolean sucesso;
    private final String conteudo;
    private final String modelo;
    private final Integer tokensUtilizados;
    private final String erro;

    public IAResponse(boolean sucesso, String conteudo, String modelo, Integer tokensUtilizados) {
        this.sucesso = sucesso;
        this.conteudo = conteudo;
        this.modelo = modelo;
        this.tokensUtilizados = tokensUtilizados != null ? tokensUtilizados : 0;
        this.erro = null;
    }

    public IAResponse(boolean sucesso, String erro) {
        this.sucesso = sucesso;
        this.conteudo = null;
        this.modelo = null;
        this.tokensUtilizados = 0;
        this.erro = erro;
    }

    public boolean isSucesso() {
        return sucesso;
    }

    public String getConteudo() {
        return conteudo;
    }

    public String getModelo() {
        return modelo;
    }

    public Integer getTokensUtilizados() {
        return tokensUtilizados;
    }

    public String getErro() {
        return erro;
    }
}
