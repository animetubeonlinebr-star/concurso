package br.com.marcosbassetto.concursos.domain.edital.service;

import br.com.marcosbassetto.concursos.domain.edital.dto.EstruturaEditalDTO;

public interface EstruturaEditalProvider {
    EstruturaEditalDTO extrairEstrutura(String textoExtraido);
}
