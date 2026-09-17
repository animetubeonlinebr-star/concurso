package br.com.marcosbassetto.concursos.domain.edital.interpretacao.classificacao;

import br.com.marcosbassetto.concursos.domain.edital.interpretacao.documento.DocumentModel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * O montador transforma a sequência de linhas classificadas na hierarquia de
 * matérias e tópicos. Cada erro aqui aparece direto na tela de revisão.
 */
class MontadorHierarquiaTest {

    private final MontadorHierarquia montador =
            new MontadorHierarquia(new ClassificadorUnidadeEdital());

    @Test
    @DisplayName("disciplina recolhe os tópicos seguintes")
    void disciplinaRecolheTopicos() {
        String bloco = """
                LÍNGUA PORTUGUESA
                Leitura e interpretação de textos.
                Ortografia oficial.
                """;

        var materias = montador.montar(DocumentModel.de(bloco), bloco);

        assertThat(materias).hasSize(1);
        assertThat(materias.get(0).nome()).isEqualTo("LÍNGUA PORTUGUESA");
        assertThat(materias.get(0).topicos())
                .extracting("nome")
                .containsExactly("Leitura e interpretação de textos.", "Ortografia oficial.");
    }

    @Test
    @DisplayName("tópicos antes de qualquer disciplina são descartados")
    void topicosOrfaosSaoDescartados() {
        // Sem matéria dona, o tópico não tem onde ser gravado.
        String bloco = """
                Texto solto antes do conteúdo.
                LÍNGUA PORTUGUESA
                Leitura e interpretação de textos.
                """;

        var materias = montador.montar(DocumentModel.de(bloco), bloco);

        assertThat(materias).hasSize(1);
        assertThat(materias.get(0).topicos()).hasSize(1);
    }

    @Test
    @DisplayName("cabeçalho de página não vira matéria")
    void cabecalhoDePaginaNaoViraMateria() {
        String bloco = """
                UNIVERSIDADE ESTADUAL PAULISTA
                LÍNGUA PORTUGUESA
                Leitura e interpretação de textos.
                UNIVERSIDADE ESTADUAL PAULISTA
                """;

        var materias = montador.montar(DocumentModel.de(bloco), bloco);

        assertThat(materias).extracting("nome").containsExactly("LÍNGUA PORTUGUESA");
    }

    @Test
    @DisplayName("marcação de formatação não aparece no nome")
    void marcacaoNaoApareceNoNome() {
        String bloco = """
                ## Língua Portuguesa
                - Interpretação de textos.
                """;

        var materias = montador.montar(DocumentModel.de(bloco), bloco);

        assertThat(materias.get(0).nome()).isEqualTo("Língua Portuguesa");
        assertThat(materias.get(0).topicos().get(0).nome())
                .isEqualTo("Interpretação de textos.");
    }

    @Test
    @DisplayName("caminho hierárquico registra o cargo da matéria")
    void caminhoRegistraCargo() {
        String bloco = """
                CARGO 4: CONTADOR
                CONHECIMENTOS ESPECÍFICOS
                CONTABILIDADE GERAL
                Balanço patrimonial.
                """;

        var materias = montador.montar(DocumentModel.de(bloco), bloco);

        assertThat(materias).hasSize(1);
        // A origem sobrevive para a auditoria saber de qual cargo veio.
        assertThat(materias.get(0).caminho()).contains("CARGO 4: CONTADOR");
    }

    @Test
    @DisplayName("bloco sem disciplina reconhecida não produz matéria")
    void blocoSemDisciplina() {
        String bloco = "Apenas um parágrafo solto que não é título de nada.";

        assertThat(montador.montar(DocumentModel.de(bloco), bloco)).isEmpty();
    }
}