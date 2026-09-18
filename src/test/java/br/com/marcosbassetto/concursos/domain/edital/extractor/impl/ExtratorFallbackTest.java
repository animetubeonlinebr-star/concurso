package br.com.marcosbassetto.concursos.domain.edital.extractor.impl;

import br.com.marcosbassetto.concursos.domain.edital.domain.PadraoEdital;
import br.com.marcosbassetto.concursos.domain.edital.dto.DadosConcurso;
import br.com.marcosbassetto.concursos.domain.edital.dto.EstruturaEditalDTO;
import br.com.marcosbassetto.concursos.domain.edital.dto.MateriaExtraida;
import br.com.marcosbassetto.concursos.domain.edital.extractor.TextoPreProcessador;
import br.com.marcosbassetto.concursos.domain.edital.segmenter.SegmentadorEdital;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ExtratorFallbackTest {

    private ExtratorFallback extrator;

    @BeforeEach
    void setUp() {
        extrator = new ExtratorFallback(new TextoPreProcessador(), SegmentadorEdital.comAncorasPadrao());
    }

    @Test
    @DisplayName("declara o padrão LISTA_SIMPLES")
    void deveDeclararPadrao() {
        assertThat(extrator.padraoSuportado()).isEqualTo(PadraoEdital.LISTA_SIMPLES);
    }

    @Test
    @DisplayName("extrai dados cadastrais capturáveis sem bloco de conteúdo")
    void deveExtrairDadosCadastrais() {
        EstruturaEditalDTO estrutura = extrator.extrair(textoListaSimples());

        DadosConcurso dados = estrutura.dadosConcurso();
        assertThat(dados).isNotNull();
        assertThat(dados.orgao()).containsIgnoringCase("Universidade");
        assertThat(dados.banca()).isEqualTo("Instituto Exemplo");
        assertThat(dados.ano()).isEqualTo(2025);
    }

    @Test
    @DisplayName("agrupa itens de lista sob disciplinas em maiúsculas")
    void deveExtrairMateriasDeLista() {
        EstruturaEditalDTO estrutura = extrator.extrair(textoListaSimples());

        List<MateriaExtraida> materias = estrutura.materiasDoPrimeiroCurso();

        assertThat(materias).extracting(MateriaExtraida::nome)
                .contains("LÍNGUA PORTUGUESA", "DIREITO ADMINISTRATIVO");
    }

    @Test
    @DisplayName("devolve estrutura não-identificada em vez de lançar exceção")
    void naoQuebraComTextoVazio() {
        EstruturaEditalDTO vazio = extrator.extrair("");
        assertThat(vazio).isNotNull();
        assertThat(vazio.materiasDoPrimeiroCurso()).isEmpty();

        assertThat(extrator.extrair(null)).isNotNull();
    }

    private String textoListaSimples() {
        return """
                UNIVERSIDADE FEDERAL DE EXEMPLO
                EDITAL DE PROCESSO SELETIVO 2025
                BANCA: Instituto Exemplo

                CONTEÚDO PROGRAMÁTICO

                LÍNGUA PORTUGUESA
                ▪ Compreensão e interpretação de texto.
                ▪ Ortografia oficial.
                ▪ Classes de palavras.

                DIREITO ADMINISTRATIVO
                ▪ Princípios da Administração Pública.
                ▪ Atos administrativos.
                """;
    }
}
