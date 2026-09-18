package br.com.marcosbassetto.concursos.domain.edital.extractor;

import br.com.marcosbassetto.concursos.domain.edital.domain.Banca;
import br.com.marcosbassetto.concursos.domain.edital.domain.PadraoEdital;
import br.com.marcosbassetto.concursos.domain.edital.extractor.impl.ExtratorDisciplinaCabecalho;
import br.com.marcosbassetto.concursos.domain.edital.extractor.impl.ExtratorFallback;
import br.com.marcosbassetto.concursos.domain.edital.extractor.impl.ExtratorFCC;
import br.com.marcosbassetto.concursos.domain.edital.extractor.impl.ExtratorPadrao;
import br.com.marcosbassetto.concursos.domain.edital.segmenter.SegmentadorEdital;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExtratorEditalFactoryTest {

    private ExtratorEditalFactory factory;

    @BeforeEach
    void setUp() {
        TextoPreProcessador pre = new TextoPreProcessador();
        SegmentadorEdital segmentador = SegmentadorEdital.comAncorasPadrao();

        factory = new ExtratorEditalFactory(
                new ExtratorFCC(),
                new ExtratorFallback(pre, segmentador),
                new ExtratorPadrao(pre, segmentador),
                new ExtratorDisciplinaCabecalho(pre));
    }

    @Test
    @DisplayName("banca FCC tem precedência sobre o padrão")
    void fccTemPrecedencia() {
        assertThat(factory.criar(Banca.FCC, PadraoEdital.DISCIPLINA_CABECALHO))
                .isInstanceOf(ExtratorFCC.class);
    }

    @Test
    @DisplayName("DISCIPLINA_CABECALHO resolve para o extrator real, não para o stub padrão")
    void disciplinaCabecalhoNaoCaiNoPadrao() {
        ExtratorEdital extrator = factory.criar(Banca.DESCONHECIDA, PadraoEdital.DISCIPLINA_CABECALHO);

        assertThat(extrator).isInstanceOf(ExtratorDisciplinaCabecalho.class);
        assertThat(extrator).isNotInstanceOf(ExtratorPadrao.class);
    }

    @Test
    @DisplayName("LISTA_SIMPLES resolve para o fallback")
    void listaSimplesResolveParaFallback() {
        assertThat(factory.criar(Banca.DESCONHECIDA, PadraoEdital.LISTA_SIMPLES))
                .isInstanceOf(ExtratorFallback.class);
    }

    @Test
    @DisplayName("padrões de bloco e desconhecido caem no extrator padrão")
    void demaisPadroesCaemNoPadrao() {
        assertThat(factory.criar(Banca.DESCONHECIDA, PadraoEdital.ENFASE_BLOCOS))
                .isInstanceOf(ExtratorPadrao.class);
        assertThat(factory.criar(Banca.DESCONHECIDA, PadraoEdital.MULTI_NIVEL))
                .isInstanceOf(ExtratorPadrao.class);
        assertThat(factory.criar(Banca.CEBRASPE, PadraoEdital.DESCONHECIDO))
                .isInstanceOf(ExtratorPadrao.class);
    }
}
