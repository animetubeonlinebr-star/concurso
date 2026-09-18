package br.com.marcosbassetto.concursos.domain.edital.interpretacao;

import br.com.marcosbassetto.concursos.domain.edital.dto.StatusExtracao;
import br.com.marcosbassetto.concursos.domain.edital.interpretacao.documento.DocumentModel;
import br.com.marcosbassetto.concursos.domain.edital.interpretacao.estrategia.EstrategiaBlocoConteudo;
import br.com.marcosbassetto.concursos.domain.edital.interpretacao.estrategia.EstrategiaDocumentoSemConteudo;
import br.com.marcosbassetto.concursos.domain.edital.interpretacao.estrategia.EstrategiaFactory;
import br.com.marcosbassetto.concursos.domain.edital.interpretacao.classificacao.ClassificadorUnidadeEdital;
import br.com.marcosbassetto.concursos.domain.edital.interpretacao.classificacao.MontadorHierarquia;
import br.com.marcosbassetto.concursos.domain.edital.interpretacao.metadados.ExtratorMetadadosEdital;
import br.com.marcosbassetto.concursos.domain.edital.interpretacao.perfil.EditalProfile;
import br.com.marcosbassetto.concursos.domain.edital.interpretacao.perfil.EditalProfileClassifier;
import br.com.marcosbassetto.concursos.domain.edital.interpretacao.rascunho.EstruturaRascunho;
import br.com.marcosbassetto.concursos.domain.edital.interpretacao.rascunho.MateriaRascunho;
import br.com.marcosbassetto.concursos.domain.edital.interpretacao.rascunho.TopicoRascunho;
import br.com.marcosbassetto.concursos.domain.edital.interpretacao.validacao.AvaliadorQualidadeExtracao;
import br.com.marcosbassetto.concursos.domain.edital.segmenter.SegmentadorEdital;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * O Template Method existe para que a parte invariante da interpretação não
 * possa ser pulada por um formato novo de edital. Estes testes fixam esse
 * contrato: o passo variável muda o conteúdo, mas nunca as etapas ao redor.
 */
class InterpretadorEditalTemplateTest {

    private final EditalProfileClassifier classificador = new EditalProfileClassifier();
    private final AvaliadorQualidadeExtracao avaliador = new AvaliadorQualidadeExtracao();
    private final ExtratorMetadadosEdital metadados = new ExtratorMetadadosEdital();

    @Test
    @DisplayName("texto vazio não chega ao passo variável")
    void textoVazioCurtoCircuito() {
        InterpretadorEditalTemplate template = templateQueFalhaSeChamado();

        var estrutura = template.interpretar("   ");

        assertThat(estrutura.status()).isEqualTo(StatusExtracao.NAO_IDENTIFICADO);
        assertThat(estrutura.mensagem()).contains("vazio");
    }

    @Test
    @DisplayName("perfil novo não consegue pular a avaliação de qualidade")
    void qualidadeNuncaEhPulada() {
        // Um formato que devolve matérias sem tópicos: o template precisa
        // rebaixar o status, mesmo que o passo variável não faça ideia disso.
        InterpretadorEditalTemplate template = new InterpretadorEditalTemplate(
                classificador, metadados, avaliador) {
            @Override
            protected EstruturaRascunho interpretarConteudo(DocumentModel documento,
                                                            EditalProfile perfil) {
                return new EstruturaRascunho(perfil, perfil.codigo(),
                        List.of(new MateriaRascunho("DIREITO CIVIL", List.of())), null);
            }
        };

        var estrutura = template.interpretar("EDITAL DE ABERTURA\nCONTEÚDO PROGRAMÁTICO\nDIREITO CIVIL\n");

        assertThat(estrutura.status()).isEqualTo(StatusExtracao.BAIXA_CONFIANCA);
    }

    @Test
    @DisplayName("metadados são extraídos mesmo quando o conteúdo falha")
    void metadadosSobrevivemAoConteudoVazio() {
        InterpretadorEditalTemplate template = new InterpretadorEditalTemplate(
                classificador, metadados, avaliador) {
            @Override
            protected EstruturaRascunho interpretarConteudo(DocumentModel documento,
                                                            EditalProfile perfil) {
                return EstruturaRascunho.vazia(perfil, "nada aqui");
            }
        };

        var estrutura = template.interpretar("""
                CONCURSO PÚBLICO 2026
                ORGAO: Tribunal de Justica de Sao Paulo
                ANEXO II - CONTEUDO PROGRAMATICO
                """);

        assertThat(estrutura.dadosConcurso()).isNotNull();
        assertThat(estrutura.dadosConcurso().ano()).isEqualTo(2026);
        assertThat(estrutura.materiasDoPrimeiroCurso()).isEmpty();
    }

    @Test
    @DisplayName("a resposta sempre sai no formato EstruturaEditalDTO")
    void saidaTemCursoGeralImplicito() {
        InterpretadorEditalTemplate template = new InterpretadorEditalTemplate(
                classificador, metadados, avaliador) {
            @Override
            protected EstruturaRascunho interpretarConteudo(DocumentModel documento,
                                                            EditalProfile perfil) {
                return new EstruturaRascunho(perfil, perfil.codigo(), List.of(
                        new MateriaRascunho("LÍNGUA PORTUGUESA", List.of(
                                TopicoRascunho.de("Concordância verbal"))),
                        new MateriaRascunho("MATEMÁTICA", List.of(
                                TopicoRascunho.de("Porcentagem")))), null);
            }
        };

        var estrutura = template.interpretar("EDITAL\nCONTEÚDO PROGRAMÁTICO\n");

        assertThat(estrutura.cursos()).hasSize(1);
        assertThat(estrutura.cursos().get(0).nome()).isEqualTo("Geral");
        assertThat(estrutura.materiasDoPrimeiroCurso()).extracting("nome")
                .containsExactly("LÍNGUA PORTUGUESA", "MATEMÁTICA");
    }

    private InterpretadorEditalTemplate templateQueFalhaSeChamado() {
        return new InterpretadorEditalTemplate(classificador, metadados, avaliador) {
            @Override
            protected EstruturaRascunho interpretarConteudo(DocumentModel documento,
                                                            EditalProfile perfil) {
                throw new AssertionError("o passo variável não deveria ser chamado");
            }
        };
    }
}
