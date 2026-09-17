package br.com.marcosbassetto.concursos.domain.edital.interpretacao.estrategia;

import br.com.marcosbassetto.concursos.domain.edital.interpretacao.classificacao.ClassificadorUnidadeEdital;
import br.com.marcosbassetto.concursos.domain.edital.interpretacao.classificacao.MontadorHierarquia;
import br.com.marcosbassetto.concursos.domain.edital.interpretacao.documento.DocumentModel;
import br.com.marcosbassetto.concursos.domain.edital.interpretacao.perfil.EditalProfile;
import br.com.marcosbassetto.concursos.domain.edital.segmenter.SegmentadorEdital;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A estratégia decide o que ler e quando desistir. O caso mais importante é o
 * da desistência: quando não há seção de conteúdo, extrair o documento inteiro
 * produz disciplinas a partir de cronograma e de cláusulas.
 */
class EstrategiaBlocoConteudoTest {

    private final EstrategiaBlocoConteudo estrategia = new EstrategiaBlocoConteudo(
            new SegmentadorEdital(),
            new MontadorHierarquia(new ClassificadorUnidadeEdital()));

    @Test
    @DisplayName("lê apenas o que está dentro da seção declarada")
    void leApenasASecaoDeclarada() {
        var rascunho = estrategia.interpretar(DocumentModel.de("""
                EDITAL DE ABERTURA
                CAPÍTULO 1 – DAS DISPOSIÇÕES PRELIMINARES
                O concurso será regido por este edital.

                CONTEÚDO PROGRAMÁTICO

                LÍNGUA PORTUGUESA
                Leitura e interpretação de textos.

                MATEMÁTICA
                Raciocínio lógico.
                """), EditalProfile.generico());

        assertThat(rascunho.materias()).extracting("nome")
                .containsExactly("LÍNGUA PORTUGUESA", "MATEMÁTICA");
    }

    @Test
    @DisplayName("documento sem seção de conteúdo não produz matérias")
    void semSecaoNaoProduzMaterias() {
        // Extrair o texto inteiro como reserva fazia o regulamento virar
        // estrutura. A ausência de seção é resposta definitiva.
        var rascunho = estrategia.interpretar(DocumentModel.de("""
                EDITAL DE ABERTURA
                CAPÍTULO 1 – DAS DISPOSIÇÕES PRELIMINARES
                O concurso será regido por este edital.
                CAPÍTULO 2 – DOS CARGOS
                CARGOS DE NÍVEL SUPERIOR
                """), EditalProfile.generico());

        assertThat(rascunho.materias()).isEmpty();
        assertThat(rascunho.mensagem()).contains("conteúdo programático");
    }

    @Test
    @DisplayName("texto vazio devolve rascunho vazio com mensagem")
    void textoVazio() {
        var rascunho = estrategia.interpretar(DocumentModel.de(""), EditalProfile.generico());

        assertThat(rascunho.vazia()).isTrue();
        assertThat(rascunho.mensagem()).isNotBlank();
    }

    @Test
    @DisplayName("estratégia cobre os perfis que compartilham o formato de bloco")
    void cobrePerfisDeBloco() {
        assertThat(estrategia.suporta("GENERICO")).isTrue();
        assertThat(estrategia.suporta("CARGO")).isTrue();
        assertThat(estrategia.suporta("CONHECIMENTOS")).isTrue();
        assertThat(estrategia.suporta("SEM_CONTEUDO")).isFalse();
    }
}