package br.com.marcosbassetto.concursos.domain.edital.extractor;

import br.com.marcosbassetto.concursos.domain.edital.domain.Banca;
import br.com.marcosbassetto.concursos.domain.edital.domain.PadraoEdital;
import br.com.marcosbassetto.concursos.domain.edital.extractor.impl.ExtratorDisciplinaCabecalho;
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
    private final ExtratorDisciplinaCabecalho extratorDisciplinaCabecalho;

    /**
     * A banca tem precedência sobre o padrão: um edital da FCC é tratado
     * pelo extrator específico mesmo que o layout pareça genérico.
     */
    public ExtratorEdital criar(Banca banca, PadraoEdital padrao) {

        if (banca == Banca.FCC) {
            return extratorFCC;
        }

        if (padrao == null) {
            return extratorPadrao;
        }

        return switch (padrao) {
            case DISCIPLINA_CABECALHO -> extratorDisciplinaCabecalho;
            case LISTA_SIMPLES -> extratorFallback;
            // ENFASE_BLOCOS, MULTI_NIVEL e DESCONHECIDO: o extrator padrão
            // cobre blocos "CONTEÚDO PROGRAMÁTICO" e cabeçalhos, que é o
            // denominador comum nesses casos.
            default -> extratorPadrao;
        };
    }
}
