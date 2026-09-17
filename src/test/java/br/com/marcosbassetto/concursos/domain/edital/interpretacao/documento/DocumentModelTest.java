package br.com.marcosbassetto.concursos.domain.edital.interpretacao.documento;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * O modelo é a única ponte entre o PDFBox e as regras de negócio. Se ele
 * perder informação ou normalizar demais, todas as decisões seguintes herdam
 * o erro.
 */
class DocumentModelTest {

    @Test
    @DisplayName("texto vazio produz modelo vazio em vez de nulo")
    void textoVazio() {
        assertThat(DocumentModel.de(null).vazio()).isTrue();
        assertThat(DocumentModel.de("  ").vazio()).isTrue();
        assertThat(DocumentModel.de(null).linhas()).isEmpty();
    }

    @Test
    @DisplayName("linhas são separadas e sem espaços nas pontas")
    void linhasSemEspacosNasPontas() {
        DocumentModel documento = DocumentModel.de("  LÍNGUA PORTUGUESA  \n  Interpretação.  ");

        assertThat(documento.linhas()).containsExactly("LÍNGUA PORTUGUESA", "Interpretação.");
    }

    @Test
    @DisplayName("frequência conta apenas linhas em caixa alta")
    void frequenciaContaCaixaAlta() {
        DocumentModel documento = DocumentModel.de("""
                UNIVERSIDADE ESTADUAL PAULISTA
                Língua Portuguesa
                UNIVERSIDADE ESTADUAL PAULISTA
                """);

        // A disciplina em caixa mista é ignorada: se entrasse, a contagem
        // deixaria de distinguir cabeçalho de conteúdo.
        assertThat(documento.frequenciaLinhasMaiusculas())
                .isEqualTo(Map.of("UNIVERSIDADE ESTADUAL PAULISTA", 2));
    }

    @Test
    @DisplayName("frequência ignora linhas longas demais para serem título")
    void frequenciaIgnoraLinhasLongas() {
        String paragrafo = "TEXTO EM CAIXA ALTA ".repeat(20).strip();
        DocumentModel documento = DocumentModel.de(paragrafo + "\n" + paragrafo);

        assertThat(documento.frequenciaLinhasMaiusculas()).isEmpty();
    }

    @Test
    @DisplayName("cabeçalho limita-se ao começo do documento")
    void cabecalhoLimitadoAoComeco() {
        String texto = "A".repeat(5000);
        DocumentModel documento = DocumentModel.de(texto);

        assertThat(documento.cabecalho()).hasSize(3000);
        assertThat(documento.textoLimpo()).hasSize(5000);
    }
}