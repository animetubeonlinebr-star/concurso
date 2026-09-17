package br.com.marcosbassetto.concursos.domain.edital.interpretacao;

import br.com.marcosbassetto.concursos.domain.edital.interpretacao.classificacao.ClassificadorUnidadeEdital;
import br.com.marcosbassetto.concursos.domain.edital.interpretacao.classificacao.TipoUnidadeEdital;
import br.com.marcosbassetto.concursos.domain.edital.interpretacao.documento.DocumentModel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * O classificador é a fronteira entre "linha em destaque" e "matéria". Os
 * casos abaixo vêm dos editais de amostra, onde cada regra ausente produziu
 * um resultado errado no sistema.
 */
class ClassificadorUnidadeEditalTest {

    private final ClassificadorUnidadeEdital classificador = new ClassificadorUnidadeEdital();

    @Test
    @DisplayName("disciplina em caixa alta vira matéria")
    void disciplinaEmCaixaAlta() {
        assertThat(classificar("LÍNGUA PORTUGUESA", "Texto anterior."))
                .isEqualTo(TipoUnidadeEdital.MATERIA);
    }

    @Test
    @DisplayName("disciplina em caixa mista também vira matéria")
    void disciplinaEmCaixaMista() {
        // Exigir caixa alta descartaria editais inteiros escritos assim.
        assertThat(classificar("Língua Portuguesa", "Texto anterior."))
                .isEqualTo(TipoUnidadeEdital.MATERIA);
    }

    @Test
    @DisplayName("parágrafo de conteúdo quebra em tópicos, não em matérias")
    void paragrafoViraTopico() {
        assertThat(classificar("Leitura e interpretação de textos de gêneros variados.", null))
                .isEqualTo(TipoUnidadeEdital.TOPICO);
    }

    @Test
    @DisplayName("continuação de parágrafo quebrado não vira matéria")
    void continuacaoDeParagrafoNaoViraMateria() {
        // O PDFBox corta a linha sem pontuação final; a linha seguinte é a
        // continuação e não pode ser promovida a disciplina.
        String anterior = "Compreensão e interpretação de textos. Domínio dos mecanismos de coesão e";
        assertThat(classificar("coerência textual, incluindo referenciação.", anterior))
                .isEqualTo(TipoUnidadeEdital.TOPICO);
    }

    @Test
    @DisplayName("item de lista é tópico mesmo citando instituição")
    void itemDeListaEhTopico() {
        assertThat(classificar("▪ Biologia Vegetal: caracterização geral.", null))
                .isEqualTo(TipoUnidadeEdital.TOPICO);
    }

    @Test
    @DisplayName("rótulo de cargo vira unidade de nível 1")
    void rotuloDeCargo() {
        assertThat(classificar("CARGO 4: CONTADOR", null))
                .isEqualTo(TipoUnidadeEdital.CARGO);
    }

    @Test
    @DisplayName("agrupamento de conhecimentos é reconhecido pelo nome")
    void agrupamentoDeConhecimentos() {
        assertThat(classificar("CONHECIMENTOS ESPECÍFICOS", null))
                .isEqualTo(TipoUnidadeEdital.CONHECIMENTOS_ESPECIFICOS);
        assertThat(classificar("CONHECIMENTOS GERAIS", null))
                .isEqualTo(TipoUnidadeEdital.CONHECIMENTOS_GERAIS);
    }

    @Test
    @DisplayName("nível de escolaridade agrupa em vez de virar matéria")
    void nivelDeEscolaridade() {
        assertThat(classificar("CARGOS DE ENSINO MÉDIO / TÉCNICO COMPLETO", null))
                .isEqualTo(TipoUnidadeEdital.EIXO);
    }

    @Test
    @DisplayName("título estrutural não é conteúdo programático")
    void tituloEstrutural() {
        assertThat(classificar("CRONOGRAMA", null)).isEqualTo(TipoUnidadeEdital.SECAO);
        assertThat(classificar("ANEXO II", null)).isEqualTo(TipoUnidadeEdital.SECAO);
        assertThat(classificar("COMUNICADO", null)).isEqualTo(TipoUnidadeEdital.SECAO);
    }

    @Test
    @DisplayName("cabeçalho institucional repetido não vira matéria")
    void cabecalhoInstitucionalRepetido() {
        DocumentModel documento = DocumentModel.de("""
                UNIVERSIDADE ESTADUAL PAULISTA
                conteudo
                UNIVERSIDADE ESTADUAL PAULISTA
                conteudo
                """);

        assertThat(classificador.classificar("UNIVERSIDADE ESTADUAL PAULISTA", documento))
                .isEqualTo(TipoUnidadeEdital.CABECALHO_DOCUMENTO);
    }

    @Test
    @DisplayName("rodapé repetido em muitas páginas não vira matéria")
    void rodapeRepetidoNaoViraMateria() {
        // Repete dezenas de vezes: é mobiliário de página, não disciplina.
        StringBuilder texto = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            texto.append("IMÓVEIS DO ESTADO DE SÃO PAULO\nconteudo\n");
        }

        DocumentModel documento = DocumentModel.de(texto.toString());

        assertThat(classificador.classificar("IMÓVEIS DO ESTADO DE SÃO PAULO", documento))
                .isEqualTo(TipoUnidadeEdital.CABECALHO_DOCUMENTO);
    }

    @Test
    @DisplayName("disciplina que repete por cargo continua sendo matéria")
    void disciplinaRepetidaPorCargo() {
        // "CONHECIMENTOS ESPECÍFICOS" aparece uma vez por cargo — repetir é
        // normal e não pode ser confundido com rodapé.
        StringBuilder texto = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            texto.append("CONHECIMENTOS ESPECÍFICOS\nconteudo\n");
        }

        DocumentModel documento = DocumentModel.de(texto.toString());

        assertThat(classificador.classificar("CONHECIMENTOS ESPECÍFICOS", documento))
                .isEqualTo(TipoUnidadeEdital.CONHECIMENTOS_ESPECIFICOS);
    }

    @Test
    @DisplayName("fragmento curto sem estrutura não vira matéria")
    void fragmentoCurto() {
        // Sobra de tabela que o extrator quebrou em pedaços.
        assertThat(classificar("VALOR DE", null)).isEqualTo(TipoUnidadeEdital.TOPICO);
    }

    @Test
    @DisplayName("marcação markdown é ignorada na classificação")
    void marcacaoMarkdownIgnorada() {
        assertThat(classificar("## Lingua Portuguesa", null))
                .isEqualTo(TipoUnidadeEdital.MATERIA);
    }

    private TipoUnidadeEdital classificar(String linha, String linhaAnterior) {
        return classificador.classificar(linha, linhaAnterior, Map.of());
    }
}