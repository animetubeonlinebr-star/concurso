package br.com.marcosbassetto.concursos.domain.edital.extractor;

import br.com.marcosbassetto.concursos.domain.edital.domain.Banca;
import br.com.marcosbassetto.concursos.domain.edital.domain.PadraoEdital;
import br.com.marcosbassetto.concursos.domain.edital.dto.EstruturaEditalDTO;

public interface ExtratorEdital {

    default Banca bancaSuportada() { return null; }

    default PadraoEdital padraoSuportado() { return null; }

    EstruturaEditalDTO extrair(String texto);
}
