package br.com.marcosbassetto.concursos.domain.edital.domain;

public enum Banca {
    CEBRASPE,
    FCC,
    FGV,
    VUNESP,
    UNESP,
    DESCONHECIDA;

    public boolean isConhecida() {
        return this != DESCONHECIDA;
    }
}
