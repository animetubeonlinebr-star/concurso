package br.com.marcosbassetto.concursos.domain.edital.extractor;

import br.com.marcosbassetto.concursos.domain.edital.domain.Banca;
import br.com.marcosbassetto.concursos.domain.edital.domain.PadraoEdital;
import br.com.marcosbassetto.concursos.domain.edital.extractor.impl.ExtratorFallback;
import br.com.marcosbassetto.concursos.domain.edital.extractor.impl.ExtratorFCC;
import br.com.marcosbassetto.concursos.domain.edital.extractor.impl.ExtratorPadrao;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ExtratorEditalFactory {

    private final ExtratorFCC extratorFCC;
    private final ExtratorFallback extratorFallback;
    private final ExtratorPadrao extratorPadrao;

    public ExtratorEdital criar(Banca banca, PadraoEdital padrao) {

        if (banca == Banca.FCC) {
            return extratorFCC;
        }

        if (padrao == PadraoEdital.LISTA_SIMPLES) {
            return extratorFallback;
        }

        return extratorPadrao;
    }
}
