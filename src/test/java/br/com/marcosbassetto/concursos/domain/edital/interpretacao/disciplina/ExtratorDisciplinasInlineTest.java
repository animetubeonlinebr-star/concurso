package br.com.marcosbassetto.concursos.domain.edital.interpretacao.disciplina;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A leitura inline existe para o edital que não escreve a disciplina em linha
 * própria. Estes testes fixam o que ela reconhece e — mais importante — o que
 * precisa recusar, porque cada falso positivo aqui vira matéria inventada na
 * tela de revisão do usuário.
 */
class ExtratorDisciplinasInlineTest {

    private final ExtratorDisciplinasInline extrator = new ExtratorDisciplinasInline();

    @Test
    @DisplayName("disciplina inline com itens numerados vira matéria com tópicos")
    void disciplinaInlineComItens() {
        var materias = extrator.extrair("""
                ADMINISTRAÇÃO GERAL: 1 Evolução da administração. 1.1 Principais abordagens. \
                2 Processo administrativo. 2.1 Funções de administração.
                """);

        assertThat(materias).hasSize(1);
        assertThat(materias.get(0).nome()).isEqualTo("ADMINISTRAÇÃO GERAL");
        assertThat(materias.get(0).topicos()).extracting("nome")
                .containsExactly(
                        "Evolução da administração. 1.1 Principais abordagens.",
                        "Processo administrativo. 2.1 Funções de administração.");
        assertThat(materias.get(0).topicos()).extracting("codigo")
                .containsExactly("1", "2");
    }

    @Test
    @DisplayName("sub-níveis não viram tópicos separados")
    void subNiveisNaoViramTopicos() {
        // "1.1" e "2.5.3" são sub-itens do item de topo, não itens novos.
        // Sem essa regra a disciplina teria muitas vezes mais tópicos.
        var materias = extrator.extrair(
                "DIREITO CIVIL: 1 Parte geral. 1.1 Pessoas. 1.2 Bens. 2 Obrigações. 2.1 Fontes.");

        assertThat(materias.get(0).topicos()).hasSize(2);
        assertThat(materias.get(0).topicos()).extracting("codigo")
                .containsExactly("1", "2");
    }

    @Test
    @DisplayName("disciplina em linha própria, com o nome sozinho, também é lida")
    void disciplinaComoNomeSozinho() {
        // Variante em que o nome ocupa a linha e o conteúdo vem abaixo.
        var materias = extrator.extrair("""
                Direito Administrativo:
                Dos Atos administrativos: conceitos, requisitos, atributos.

                Direito Constitucional:
                Constituição: conceito e espécies.
                """);

        assertThat(materias).extracting("nome")
                .containsExactly("Direito Administrativo", "Direito Constitucional");
    }

    @Test
    @DisplayName("unidade de nível 1 compõe o caminho da matéria")
    void unidadeCompoeCaminho() {
        var materias = extrator.extrair("""
                CARGO 1: ANALISTA DE SISTEMAS
                LÓGICA DE PROGRAMAÇÃO: 1 Construção de algoritmos. 2 Tipos de dados.

                CARGO 2: CONTADOR
                CONTABILIDADE GERAL: 1 Balanço patrimonial. 2 Demonstrações.
                """);

        assertThat(materias).hasSize(2);
        assertThat(materias.get(0).nome()).isEqualTo("LÓGICA DE PROGRAMAÇÃO");
        assertThat(materias.get(0).caminho()).containsExactly("CARGO 1: ANALISTA DE SISTEMAS");
        assertThat(materias.get(1).nome()).isEqualTo("CONTABILIDADE GERAL");
        assertThat(materias.get(1).caminho()).containsExactly("CARGO 2: CONTADOR");
    }

    @Test
    @DisplayName("linha de conteúdo terminando em dois-pontos não vira matéria")
    void linhaDeConteudoNaoViraMateria() {
        // Caso real: o parágrafo de uma disciplina é cortado pelo PDFBox e um
        // pedaço termina em dois-pontos. Aquele pedaço é conteúdo, não nome.
        var materias = extrator.extrair("""
                DIREITO PREVIDENCIÁRIO: 1 Seguridade social.
                Seguridade social: origem e evolução legislativa no Brasil; conceito; organização.
                orçamentário; Controle de constitucionalidade de atos municipais; O mandado de Segurança.
                """);

        assertThat(materias).hasSize(1);
        assertThat(materias.get(0).nome()).isEqualTo("DIREITO PREVIDENCIÁRIO");
        assertThat(materias.get(0).topicos()).hasSize(1);
        assertThat(materias.get(0).topicos().get(0).nome())
                .contains("Seguridade social: origem e evolução legislativa");
    }

    @Test
    @DisplayName("quantidade de pontos ou itens não vira matéria")
    void medidaNaoViraMateria() {
        // "TOTAL: 45 pontos" tem a forma de disciplina inline, mas o número é
        // uma quantidade. Isto é rodapé de tabela de pontuação.
        var materias = extrator.extrair("""
                TOTAL: 45 pontos
                Prova Objetiva: 50 questões
                Duração: 4 horas
                """);

        assertThat(materias).isEmpty();
    }

    @Test
    @DisplayName("rótulo de unidade e agrupamento não viram matéria")
    void rotulosNaoViramMateria() {
        var materias = extrator.extrair("""
                CARGO: 301 – ADVOGADO
                CONHECIMENTOS ESPECÍFICOS:
                """);

        assertThat(materias).isEmpty();
    }

    @Test
    @DisplayName("seção de referência encerra o conteúdo")
    void referenciaEncerraConteudo() {
        // Sem esta parada, a bibliografia que segue o último cargo entraria
        // como conteúdo desse cargo.
        var materias = extrator.extrair("""
                CONTABILIDADE GERAL: 1 Balanço patrimonial.

                REFERÊNCIAS BIBLIOGRÁFICAS
                BRASIL. Lei nº 6.404/1976.
                """);

        assertThat(materias).extracting("nome").containsExactly("CONTABILIDADE GERAL");
    }

    @Test
    @DisplayName("linha em caixa mista com conteúdo na mesma linha é frase, não disciplina")
    void caixaMistaComConteudoEhFrase() {
        var materias = extrator.extrair(
                "Seguridade social: origem e evolução legislativa no Brasil; conceito.");

        assertThat(materias).isEmpty();
    }

    @Test
    @DisplayName("nome curto demais não nomeia disciplina")
    void nomeCurtoNaoVale() {
        assertThat(extrator.extrair("Ver: 1 item. 2 outro.")).isEmpty();
    }

    @Test
    @DisplayName("bloco vazio não produz matérias")
    void blocoVazio() {
        assertThat(extrator.extrair("")).isEmpty();
        assertThat(extrator.extrair("   \n  ")).isEmpty();
        assertThat(extrator.extrair(null)).isEmpty();
    }

    @Test
    @DisplayName("disciplina sem numeração vira um único tópico com o texto integral")
    void disciplinaSemNumeracao() {
        var materias = extrator.extrair("""
                LÍNGUA PORTUGUESA:
                Interpretação de Texto. Significação das palavras. Ortografia Oficial.
                """);

        assertThat(materias).hasSize(1);
        assertThat(materias.get(0).topicos()).hasSize(1);
        assertThat(materias.get(0).topicos().get(0).nome())
                .isEqualTo("Interpretação de Texto. Significação das palavras. Ortografia Oficial.");
    }

    @Test
    @DisplayName("nenhuma regra depende de nome de arquivo, órgão ou banca")
    void semRegraPorIdentidade() {
        // A mesma estrutura é lida igual, venha de onde vier: o que decide é a
        // forma como o documento escreve as disciplinas.
        var materias = extrator.extrair("""
                MATEMÁTICA: 1 Conjuntos numéricos. 2 Funções.
                FÍSICA: 1 Cinemática. 2 Dinâmica.
                """);

        assertThat(materias).extracting("nome")
                .containsExactly("MATEMÁTICA", "FÍSICA");
    }

    @Test
    @DisplayName("o número do item vira código, não parte do nome")
    void numeroDoItemNaoEntraNoNome() {
        var materias = extrator.extrair("MATEMÁTICA: 1 Conjuntos numéricos. 2 Funções.");

        assertThat(materias.get(0).topicos()).extracting("nome")
                .containsExactly("Conjuntos numéricos.", "Funções.");
        assertThat(materias.get(0).topicos()).extracting("codigo")
                .containsExactly("1", "2");
    }
}
