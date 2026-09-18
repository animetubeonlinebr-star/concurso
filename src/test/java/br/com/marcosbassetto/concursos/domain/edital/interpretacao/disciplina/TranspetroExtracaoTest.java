package br.com.marcosbassetto.concursos.domain.edital.interpretacao.disciplina;

import br.com.marcosbassetto.concursos.domain.edital.interpretacao.classificacao.ClassificadorUnidadeEdital;
import br.com.marcosbassetto.concursos.domain.edital.interpretacao.classificacao.MontadorHierarquia;
import br.com.marcosbassetto.concursos.domain.edital.interpretacao.documento.DocumentModel;
import br.com.marcosbassetto.concursos.domain.edital.interpretacao.rascunho.MateriaRascunho;
import br.com.marcosbassetto.concursos.domain.edital.segmenter.SegmentadorEdital;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Aceitação da melhoria produzida no edital da TRANSPETRO.
 *
 * Este documento não faz parte do Perfil B: ele já era lido antes da 02.0.10.
 * Mas o que ele lia eram os campos de um formulário de inscrição —
 * "CONTEÚDOS PROGRAMÁTICOS", "PETROBRAS TRANSPORTE S.A - TRANSPETRO",
 * "Inscrições. 12/08 a 14/09/2026" — e não as disciplinas.
 *
 * A capacidade de reconhecer disciplina inline, criada para o Perfil B,
 * também corrigiu este caso. O teste existe para que essa melhoria não volte a
 * aparecer como surpresa no diagnóstico: ela passa a ser comportamento
 * verificado, não efeito colateral observado.
 */
class TranspetroExtracaoTest {

    private static final Path DIRETORIO = Path.of("PDF_edital_teste");
    private static final String ARQUIVO = "edital_n_4_completo_1703776.pdf";

    private final SegmentadorEdital segmentador = SegmentadorEdital.comAncorasPadrao();
    private final ExtratorDisciplinas extrator = new ExtratorDisciplinas(
            new MontadorHierarquia(new ClassificadorUnidadeEdital()),
            new ExtratorDisciplinasInline());

    @Test
    @DisplayName("TRANSPETRO: as disciplinas reais são reconhecidas")
    void disciplinasReaisSaoReconhecidas() throws Exception {
        List<MateriaRascunho> materias = extrair();

        assertThat(materias).isNotEmpty();
        assertThat(materias).extracting(MateriaRascunho::nome)
                .contains("LÍNGUA PORTUGUESA", "LÍNGUA INGLESA",
                        "ADMINISTRAÇÃO FINANCEIRA E ORÇAMENTÁRIA");

        assertThat(materias).allSatisfy(m -> assertThat(m.topicos()).isNotEmpty());
    }

    @Test
    @DisplayName("TRANSPETRO: campos do formulário não são classificados como matéria")
    void camposDeFormularioNaoViramMateria() throws Exception {
        List<String> nomes = extrair().stream().map(MateriaRascunho::nome).toList();

        // Antes da 02.0.10 estes eram exatamente os "matérias" produzidos.
        assertThat(nomes)
                .doesNotContain("CONTEÚDOS PROGRAMÁTICOS")
                .doesNotContain("EVENTOS BÁSICOS DATAS")
                .doesNotContain("PETROBRAS TRANSPORTE S.A - TRANSPETRO");

        // Datas soltas de cronograma também deixaram de ser matéria.
        assertThat(nomes).noneMatch(n -> n.matches("\\d{2}/\\d{2}/\\d{4}.*"));
        assertThat(nomes).noneMatch(n -> n.matches(".*Inscrições.*\\d{2}/\\d{2}.*"));
    }

    @Test
    @DisplayName("TRANSPETRO: nenhum tópico é um campo de formulário")
    void topicosNaoSaoCamposDeFormulario() throws Exception {
        List<String> topicos = extrair().stream()
                .flatMap(m -> m.topicos().stream())
                .map(t -> t.nome())
                .toList();

        assertThat(topicos).isNotEmpty();
        assertThat(topicos).noneMatch(t -> t.contains("Assinatura da pessoa candidata"));
        assertThat(topicos).noneMatch(t -> t.contains("NOME:"));
    }

    @Test
    @DisplayName("TRANSPETRO: o bloco começa no anexo de conteúdo")
    void blocoComecaNoAnexoDeConteudo() throws Exception {
        Path pdf = exigir();
        DocumentModel documento = DocumentModel.de(extrairTexto(pdf));
        var bloco = segmentador.segmentar(documento.textoLimpo());

        assertThat(bloco.encontrado()).isTrue();
        assertThat(bloco.conteudoProgramatico()).contains("LÍNGUA PORTUGUESA");
    }

    @Test
    @DisplayName("TRANSPETRO: o fim do bloco não é delimitado por anexo com subtítulo")
    void fimDoBlocoNaoEhDelimitado() throws Exception {
        // Evidência medida, não comportamento desejado: o bloco termina no fim
        // do documento porque "ANEXO V - CRONOGRAMA" não fecha o conteúdo — a
        // regra de fim exige a linha do anexo sozinha ("ANEXO V"), e este
        // edital sempre escreve o anexo com subtítulo.
        //
        // Hoje isso é inofensivo: o reconhecimento de disciplina é
        // conservador e não classifica campo de formulário. O teste fixa o
        // comportamento atual para que a 02.0.11 possa decidir se vale mudar a
        // regra de fim, em vez de a mudança passar despercebida.
        Path pdf = exigir();
        DocumentModel documento = DocumentModel.de(extrairTexto(pdf));
        var bloco = segmentador.segmentar(documento.textoLimpo());

        assertThat(bloco.conteudoProgramatico()).contains("Assinatura da pessoa candidata");

        // E mesmo assim nenhuma matéria ou tópico sai daí.
        List<MateriaRascunho> materias = extrair();
        assertThat(materias.stream().map(MateriaRascunho::nome).toList())
                .noneMatch(n -> n.contains("Assinatura"));
        assertThat(materias.stream().flatMap(m -> m.topicos().stream()).map(t -> t.nome()).toList())
                .noneMatch(t -> t.contains("Assinatura"));
    }

    private List<MateriaRascunho> extrair() throws Exception {
        Path pdf = exigir();
        DocumentModel documento = DocumentModel.de(extrairTexto(pdf));
        var bloco = segmentador.segmentar(documento.textoLimpo());

        return extrator.extrair(documento, bloco.conteudoProgramatico());
    }

    private Path exigir() {
        Path pdf = DIRETORIO.resolve(ARQUIVO);
        Assumptions.assumeTrue(Files.exists(pdf),
                "PDF_edital_teste ausente: aceitação da TRANSPETRO não pode rodar aqui.");
        return pdf;
    }

    private String extrairTexto(Path pdf) throws Exception {
        try (PDDocument documento = Loader.loadPDF(pdf.toFile())) {
            return new PDFTextStripper().getText(documento);
        }
    }
}
