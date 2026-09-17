package br.com.marcosbassetto.concursos.domain.edital.segmenter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * O segmentador decide onde o conteúdo programático começa e termina. Cada
 * variação de cabeçalho abaixo existe em pelo menos um edital real da amostra;
 * a que faltava fazia o edital inteiro ser descartado.
 */
class SegmentadorEditalTest {

    private final SegmentadorEdital segmentador = new SegmentadorEdital();

    @Test
    @DisplayName("corta na forma simples do cabeçalho")
    void cabecalhoSimples() {
        var segmento = segmentador.segmentar("""
                EDITAL DE ABERTURA

                CONTEÚDO PROGRAMÁTICO
                LÍNGUA PORTUGUESA
                Interpretação de textos.
                """);

        assertThat(segmento.encontrado()).isTrue();
        assertThat(segmento.conteudoProgramatico()).contains("LÍNGUA PORTUGUESA");
    }

    @Test
    @DisplayName("reconhece o cabeçalho com ANEXO e travessão")
    void cabecalhoComAnexo() {
        var segmento = segmentador.segmentar("""
                EDITAL DE ABERTURA

                ANEXO II – DO CONTEÚDO PROGRAMÁTICO

                ENSINO FUNDAMENTAL COMPLETO
                CONHECIMENTOS GERAIS
                Língua Portuguesa: 1) Leitura e interpretação de textos;
                """);

        assertThat(segmento.encontrado()).isTrue();
        assertThat(segmento.conteudoProgramatico()).contains("Leitura e interpretação");
    }

    @Test
    @DisplayName("reconhece o cabeçalho com ANEXO e dois-pontos")
    void cabecalhoComAnexoEDoisPontos() {
        var segmento = segmentador.segmentar("""
                EDITAL DE ABERTURA

                ANEXO II: CONTEÚDO PROGRAMÁTICO

                LÍNGUA PORTUGUESA
                Interpretação de textos.
                """);

        assertThat(segmento.encontrado()).isTrue();
        assertThat(segmento.conteudoProgramatico()).contains("LÍNGUA PORTUGUESA");
    }

    @Test
    @DisplayName("referência ao próprio anexo no corpo não inicia o bloco")
    void referenciaAoProprioAnexoNaoIniciaBloco() {
        // O capítulo das provas cita o anexo antes de a seção existir. Cortar
        // nessa citação prenderia o regulamento inteiro dentro do conteúdo.
        var segmento = segmentador.segmentar("""
                EDITAL DE ABERTURA

                As provas observarão o disposto no ANEXO II - CONTEÚDO PROGRAMÁTICO deste Edital.

                CAPÍTULO 8 – DAS PROVAS
                Prova objetiva com 40 questões.

                ANEXO II – DO CONTEÚDO PROGRAMÁTICO

                LÍNGUA PORTUGUESA
                Interpretação de textos.
                """);

        assertThat(segmento.conteudoProgramatico())
                .doesNotContain("CAPÍTULO 8")
                .contains("LÍNGUA PORTUGUESA");
    }

    @Test
    @DisplayName("agrupamento de conhecimentos inicia o bloco quando não há título")
    void conhecimentosIniciamBlocoSemTitulo() {
        // Editais enxutos vão direto ao agrupamento, sem cabeçalho de anexo.
        var segmento = segmentador.segmentar("""
                EDITAL DE PROCESSO SELETIVO

                CONHECIMENTOS ESPECÍFICOS

                -Conceitos de programação orientada a objetos em Python e C++;
                -Banco de dados relacionais;
                """);

        assertThat(segmento.encontrado()).isTrue();
        assertThat(segmento.conteudoProgramatico()).contains("orientada a objetos");
    }

    @Test
    @DisplayName("quadro de provas com conhecimentos não antecipa o início")
    void quadroDeProvasNaoAntecipaInicio() {
        // "Conhecimentos Específicos" aparece numa tabela do capítulo de
        // provas; se o segmentador cortasse ali, o bloco começaria no meio do
        // regulamento e traria capítulos inteiros como conteúdo.
        var segmento = segmentador.segmentar("""
                EDITAL DE ABERTURA

                CAPÍTULO 8 – DAS PROVAS
                Prova Objetiva:
                Conhecimentos Gerais
                - Língua Portuguesa
                - Matemática
                Conhecimentos Específicos
                - Conhecimentos Específicos

                CAPÍTULO 9 – DA CLASSIFICAÇÃO
                Critérios de desempate.

                ANEXO II – DO CONTEÚDO PROGRAMÁTICO

                LÍNGUA PORTUGUESA
                Interpretação de textos.
                """);

        assertThat(segmento.conteudoProgramatico())
                .doesNotContain("CAPÍTULO 9")
                .contains("LÍNGUA PORTUGUESA");
    }

    @Test
    @DisplayName("fim do bloco é o anexo seguinte")
    void fimEhAnexoSeguinte() {
        var segmento = segmentador.segmentar("""
                CONTEÚDO PROGRAMÁTICO
                LÍNGUA PORTUGUESA
                Interpretação de textos.

                ANEXO III
                FORMULÁRIO DE RECURSO
                """);

        assertThat(segmento.conteudoProgramatico())
                .contains("Interpretação de textos")
                .doesNotContain("FORMULÁRIO");
    }

    @Test
    @DisplayName("documento sem bloco de conteúdo é sinalizado como não encontrado")
    void documentoSemBloco() {
        var segmento = segmentador.segmentar("""
                EDITAL DE ABERTURA
                CAPÍTULO 1 – DAS DISPOSIÇÕES PRELIMINARES
                O concurso será regido por este edital.
                """);

        assertThat(segmento.encontrado()).isFalse();
        assertThat(segmento.conteudoProgramatico()).isEmpty();
    }
}