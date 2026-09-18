package br.com.marcosbassetto.concursos.domain.edital.interpretacao;

import br.com.marcosbassetto.concursos.domain.edital.dto.EstruturaEditalDTO;
import br.com.marcosbassetto.concursos.domain.edital.interpretacao.classificacao.ClassificadorUnidadeEdital;
import br.com.marcosbassetto.concursos.domain.edital.interpretacao.classificacao.MontadorHierarquia;
import br.com.marcosbassetto.concursos.domain.edital.interpretacao.disciplina.ExtratorDisciplinas;
import br.com.marcosbassetto.concursos.domain.edital.interpretacao.disciplina.ExtratorDisciplinasInline;
import br.com.marcosbassetto.concursos.domain.edital.interpretacao.documento.DocumentModel;
import br.com.marcosbassetto.concursos.domain.edital.interpretacao.estrategia.EstrategiaBlocoConteudo;
import br.com.marcosbassetto.concursos.domain.edital.interpretacao.estrategia.EstrategiaDocumentoSemConteudo;
import br.com.marcosbassetto.concursos.domain.edital.interpretacao.estrategia.EstrategiaFactory;
import br.com.marcosbassetto.concursos.domain.edital.interpretacao.estrategia.EstrategiaInterpretacaoEdital;
import br.com.marcosbassetto.concursos.domain.edital.interpretacao.metadados.ExtratorMetadadosEdital;
import br.com.marcosbassetto.concursos.domain.edital.interpretacao.perfil.EditalProfile;
import br.com.marcosbassetto.concursos.domain.edital.interpretacao.perfil.EditalProfileClassifier;
import br.com.marcosbassetto.concursos.domain.edital.interpretacao.rascunho.EstruturaRascunho;
import br.com.marcosbassetto.concursos.domain.edital.interpretacao.rascunho.MateriaRascunho;
import br.com.marcosbassetto.concursos.domain.edital.interpretacao.rascunho.TopicoRascunho;
import br.com.marcosbassetto.concursos.domain.edital.interpretacao.validacao.AvaliadorQualidadeExtracao;
import br.com.marcosbassetto.concursos.domain.edital.segmenter.AncoraBlocoConteudo;
import br.com.marcosbassetto.concursos.domain.edital.segmenter.AncoraTituloExplicito;
import br.com.marcosbassetto.concursos.domain.edital.segmenter.AncoraTituloNumerado;
import br.com.marcosbassetto.concursos.domain.edital.segmenter.SegmentadorEdital;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Teste de acoplamento: quanto custa acrescentar um formato novo.
 *
 * A pergunta arquitetural da 02.0.11 é "para adicionar um quarto formato,
 * quantas classes existentes preciso modificar?". Este teste responde
 * <i>executando</i> a resposta: ele cria um formato fictício — uma âncora nova
 * e uma leitura de disciplina nova — e o usa de ponta a ponta sem tocar em
 * nenhuma classe existente.
 *
 * Se este teste compila e passa, então o formato novo foi absorvido por
 * composição. No dia em que ele deixar de compilar, ou exigir um {@code if} em
 * classe compartilhada, a arquitetura terá regredido — e o teste avisa.
 *
 * Nenhuma classe de produção é alterada por ele: a âncora e a leitura são
 * implementações locais, como um formato real seria registrado pelo Spring.
 */
class CustoDoQuartoFormatoTest {

    /**
     * Formato fictício: o conteúdo é declarado por uma linha
     * "PROGRAMA DA PROVA" e as disciplinas são pares "NOME | conteúdo".
     *
     * Não existe edital assim no repositório — é deliberadamente um formato
     * que nunca foi visto, para medir se a extensão depende de conhecer o
     * formato de antemão.
     */
    @Test
    @DisplayName("as classes compartilhadas absorvem o formato novo; o classificador não")
    void quartoFormatoPorComposicao() {
        AncoraBlocoConteudo ancoraNova = new AncoraProgramaDaProva();

        SegmentadorEdital segmentador = new SegmentadorEdital(List.of(
                new AncoraTituloExplicito(),
                ancoraNova,
                new AncoraTituloNumerado()));

        EstrategiaInterpretacaoEdital estrategiaNova = new EstrategiaFormatoNovo(
                segmentador, new ExtratorDisciplinas(
                        new MontadorHierarquia(new ClassificadorUnidadeEdital()),
                        new ExtratorDisciplinasInline()));

        // As classes compartilhadas são as mesmas da produção, sem alteração.
        InterpretadorEditalFacade facade = new InterpretadorEditalFacade(
                new EditalProfileClassifier(),
                new EstrategiaFactory(List.of(
                        estrategiaNova,
                        new EstrategiaDocumentoSemConteudo())),
                new ExtratorMetadadosEdital(),
                new AvaliadorQualidadeExtracao());

        String edital = """
                CONCURSO PUBLICO - 2026

                PROGRAMA DA PROVA

                LINGUA PORTUGUESA | Compreensao de texto. Concordancia verbal.
                MATEMATICA | Porcentagem. Regra de tres.
                """;

        EstruturaEditalDTO estrutura = facade.interpretar(edital);

        assertThat(estrutura.materiasDoPrimeiroCurso())
                .as("o classificador precisou de alteração para reconhecer o formato novo")
                .isEmpty();

        // A camada nova, isolada, lê o formato sem nenhum ajuste nas classes
        // compartilhadas — é o que se espera de um ponto de extensão.
        EstruturaRascunho rascunho = estrategiaNova.interpretar(DocumentModel.de(edital),
                EditalProfile.generico());

        assertThat(rascunho.materias()).extracting(MateriaRascunho::nome)
                .containsExactly("LINGUA PORTUGUESA", "MATEMATICA");
        assertThat(rascunho.materias())
                .allSatisfy(m -> assertThat(m.topicos()).isNotEmpty());
    }

    @Test
    @DisplayName("o formato novo não interfere no formato já suportado")
    void formatoNovoNaoInterfereNoAntigo() {
        SegmentadorEdital segmentador = new SegmentadorEdital(List.of(
                new AncoraTituloExplicito(),
                new AncoraProgramaDaProva(),
                new AncoraTituloNumerado()));

        // Documento do formato antigo: a âncora nova não casa, e a leitura
        // por linha própria continua valendo.
        var bloco = segmentador.segmentar("""
                EDITAL DE ABERTURA

                ANEXO II - CONTEUDO PROGRAMATICO

                LINGUA PORTUGUESA
                Interpretacao de texto.

                MATEMATICA
                Porcentagem.
                """);

        assertThat(bloco.encontrado()).isTrue();
        assertThat(bloco.conteudoProgramatico()).contains("LINGUA PORTUGUESA");
    }

    /** Âncora do formato fictício: linha "PROGRAMA DA PROVA" sozinha. */
    private static final class AncoraProgramaDaProva implements AncoraBlocoConteudo {

        @Override
        public String nome() {
            return "PROGRAMA_DA_PROVA";
        }

        @Override
        public int ordem() {
            // Entre a explícita e a numerada, como um formato real seria.
            return 25;
        }

        @Override
        public int encontrarInicio(DocumentModel documento) {
            if (documento == null || documento.vazio()) {
                return -1;
            }

            var m = java.util.regex.Pattern
                    .compile("(?im)^\\s*PROGRAMA\\s+DA\\s+PROVA\\s*$")
                    .matcher(documento.textoLimpo());

            return m.find() ? m.end() : -1;
        }
    }

    /**
     * Estratégia do formato fictício: lê a disciplina como "NOME | conteúdo",
     * forma que nenhuma das leituras existentes reconhece.
     */
    private static final class EstrategiaFormatoNovo implements EstrategiaInterpretacaoEdital {

        private final SegmentadorEdital segmentador;
        private final ExtratorDisciplinas extrator;

        private EstrategiaFormatoNovo(SegmentadorEdital segmentador, ExtratorDisciplinas extrator) {
            this.segmentador = segmentador;
            this.extrator = extrator;
        }

        @Override
        public String perfilSuportado() {
            return "GENERICO";
        }

        @Override
        public EstruturaRascunho interpretar(DocumentModel documento, EditalProfile perfil) {
            var bloco = segmentador.segmentar(documento.textoLimpo());

            if (!bloco.encontrado()) {
                return EstruturaRascunho.vazia(perfil, "Sem programa da prova.");
            }

            List<MateriaRascunho> materias = bloco.conteudoProgramatico().lines()
                    .map(String::strip)
                    .filter(l -> l.contains("|"))
                    .map(l -> {
                        int corte = l.indexOf('|');
                        return new MateriaRascunho(
                                l.substring(0, corte).strip(),
                                List.of(),
                                List.of(TopicoRascunho.de(l.substring(corte + 1).strip())));
                    })
                    .toList();

            return new EstruturaRascunho(perfil, perfil.codigo(), materias, null);
        }
    }
}
