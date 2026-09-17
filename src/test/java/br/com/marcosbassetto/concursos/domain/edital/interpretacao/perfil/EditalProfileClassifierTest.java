package br.com.marcosbassetto.concursos.domain.edital.interpretacao.perfil;

import br.com.marcosbassetto.concursos.domain.edital.interpretacao.documento.DocumentModel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static br.com.marcosbassetto.concursos.domain.edital.interpretacao.classificacao.TipoUnidadeEdital.CONHECIMENTOS_ESPECIFICOS;
import static br.com.marcosbassetto.concursos.domain.edital.interpretacao.classificacao.TipoUnidadeEdital.CONHECIMENTOS_GERAIS;
import static br.com.marcosbassetto.concursos.domain.edital.interpretacao.classificacao.TipoUnidadeEdital.MATERIA;
import static br.com.marcosbassetto.concursos.domain.edital.interpretacao.classificacao.TipoUnidadeEdital.TOPICO;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * O perfil responde "que tipo de edital é este?" antes de qualquer extração.
 * Sem essa resposta, um comunicado institucional é lido como se fosse um
 * edital e produz matérias a partir de linhas soltas.
 */
class EditalProfileClassifierTest {

    private final EditalProfileClassifier classificador = new EditalProfileClassifier();

    @Test
    @DisplayName("edital com bloco de conteúdo recebe o perfil genérico")
    void editalComBloco() {
        EditalProfile perfil = classificar("""
                EDITAL DE ABERTURA

                ANEXO II – DO CONTEÚDO PROGRAMÁTICO

                LÍNGUA PORTUGUESA
                Interpretação de textos.
                """);

        assertThat(perfil.codigo()).isEqualTo("GENERICO");
    }

    @Test
    @DisplayName("edital que separa conhecimentos gerais e específicos")
    void editalPorConhecimentos() {
        EditalProfile perfil = classificar("""
                CONTEÚDO PROGRAMÁTICO

                CONHECIMENTOS GERAIS
                Língua Portuguesa: interpretação de textos.

                CONHECIMENTOS ESPECÍFICOS
                Direito Constitucional: princípios fundamentais.
                """);

        assertThat(perfil.codigo()).isEqualTo("CONHECIMENTOS");
        assertThat(perfil.hierarquia().niveis())
                .containsExactly(CONHECIMENTOS_GERAIS, CONHECIMENTOS_ESPECIFICOS, MATERIA, TOPICO);
    }

    @Test
    @DisplayName("comunicado sem conteúdo programático é declarado como tal")
    void comunicadoSemConteudo() {
        // Caso real VNSP2603: lista cursos em tabela e não tem uma disciplina.
        EditalProfile perfil = classificar("""
                UNIVERSIDADE ESTADUAL PAULISTA
                Reitoria

                COMUNICADO
                A Pró-reitoria comunica que estarão abertas as inscrições.
                """);

        assertThat(perfil.codigo()).isEqualTo("SEM_CONTEUDO");
        assertThat(perfil.possuiBlocoConteudo()).isFalse();
    }

    @Test
    @DisplayName("edital organizado por cargo é reconhecido")
    void editalPorCargo() {
        EditalProfile perfil = classificar("""
                CONTEÚDO PROGRAMÁTICO

                CARGO 4: CONTADOR
                CONTABILIDADE GERAL: balanço patrimonial.

                CARGO 5: ANALISTA
                DIREITO: princípios.
                """);

        assertThat(perfil.codigo()).isEqualTo("CARGO");
        assertThat(perfil.tratarCargoComoNivel()).isTrue();
    }

    @Test
    @DisplayName("documento vazio cai no perfil genérico em vez de falhar")
    void documentoVazio() {
        assertThat(classificador.classificar(DocumentModel.de("")).codigo())
                .isEqualTo("GENERICO");
        assertThat(classificador.classificar(null).codigo()).isEqualTo("GENERICO");
    }

    @Test
    @DisplayName("conhecimentos sem título de anexo ainda contam como conteúdo")
    void conhecimentosSemTituloContam() {
        // Editais enxutos vão direto ao agrupamento, sem bloco declarado.
        EditalProfile perfil = classificar("""
                EDITAL DE PROCESSO SELETIVO

                CONHECIMENTOS ESPECÍFICOS
                -Conceitos de programação orientada a objetos;
                """);

        assertThat(perfil.codigo()).isEqualTo("CONHECIMENTOS");
    }

    @Test
    @DisplayName("documento de resultado não é confundido com edital")
    void documentoDeResultado() {
        assertThat(classificar("""
                RESULTADO FINAL
                Relação dos candidatos aprovados.
                """).codigo()).isEqualTo("SEM_CONTEUDO");
    }

    private EditalProfile classificar(String texto) {
        return classificador.classificar(DocumentModel.de(texto));
    }
}