package br.com.marcosbassetto.concursos.domain.edital.interpretacao.disciplina;

import br.com.marcosbassetto.concursos.domain.edital.interpretacao.classificacao.ClassificadorUnidadeEdital;
import br.com.marcosbassetto.concursos.domain.edital.interpretacao.classificacao.MontadorHierarquia;
import br.com.marcosbassetto.concursos.domain.edital.interpretacao.documento.DocumentModel;
import br.com.marcosbassetto.concursos.domain.edital.interpretacao.rascunho.MateriaRascunho;
import br.com.marcosbassetto.concursos.domain.edital.interpretacao.rascunho.TopicoRascunho;
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
 * Aceitação do formato que o diagnóstico identificou como Perfil B, sobre os
 * três documentos reais que o usam.
 *
 * O teste prova a <b>estrutura</b>, não o nome do arquivo: ele lê o PDF, deixa
 * o segmentador achar o bloco pela âncora e verifica que as disciplinas saem
 * com nome, caminho e tópicos coerentes. Se o reconhecimento dependesse de
 * qualquer regra por nome de arquivo, órgão ou banca, estes testes passariam
 * pelo motivo errado — por isso eles exercitam o pipeline real, sem atalho.
 *
 * Os arquivos vivem em {@code PDF_edital_teste}, que não está versionado em
 * todos os ambientes; quando ausente, o teste é pulado em vez de falhar.
 */
class PerfilBExtracaoTest {

    private static final Path DIRETORIO = Path.of("PDF_edital_teste");

    /** Documento 1: âncora numerada, disciplina inline, cargo rotulado. */
    private static final String PONTA_PORA =
            "4D1A8250B5C31E91D6EF22CD6FFAEAC93329629A1E7618F7A7BCAA271EABA272.pdf";

    /** Documento 2: mesmo formato, com "[...]" no lugar de trechos do conteúdo. */
    private static final String PONTA_PORA_2 =
            "9B3058B2458F3924E45700EB3A164B6B50566BB6897073E773353659DCE5E91D.pdf";

    /** Documento 3: disciplina em linha própria terminando em dois-pontos. */
    private static final String ILHABELA = "edital_n_01_2026_1704225.pdf";

    private final SegmentadorEdital segmentador = SegmentadorEdital.comAncorasPadrao();
    private final ExtratorDisciplinas extrator = new ExtratorDisciplinas(
            new MontadorHierarquia(new ClassificadorUnidadeEdital()),
            new ExtratorDisciplinasInline());

    // ---------------------------------------------------------------- bloco

    @Test
    @DisplayName("Ponta Porã: o bloco é ancorado pelo título numerado, não pelo anexo")
    void pontaPoraAncoraNumerada() throws Exception {
        var bloco = blocoDe(PONTA_PORA);

        // O documento não tem "ANEXO ... CONTEÚDO PROGRAMÁTICO"; o conteúdo
        // começa em "15.2.4 CONHECIMENTOS GERAIS".
        assertThat(bloco.encontrado()).isTrue();
        assertThat(bloco.conteudoProgramatico())
                .contains("CARGO 1: ANALISTA DE LICITAÇÃO E CONTRATOS")
                .contains("ADMINISTRAÇÃO GERAL");
    }

    @Test
    @DisplayName("Ponta Porã: o bloco termina antes do cronograma, não no fim do documento")
    void pontaPoraFimDoBloco() throws Exception {
        var bloco = blocoDe(PONTA_PORA);

        // O bloco é grande mas delimitado: se seguisse até o fim do documento,
        // anexos de cronograma e formulários virariam conteúdo programático.
        assertThat(bloco.conteudoProgramatico()).doesNotContain("CRONOGRAMA PREVISTO");
        assertThat(bloco.conteudoProgramatico().length()).isLessThan(60_000);
    }

    @Test
    @DisplayName("Ponta Porã: disciplinas inline saem com nome, tópicos e caminho de cargo")
    void pontaPoraDisciplinas() throws Exception {
        var materias = extrair(PONTA_PORA);

        assertThat(materias).isNotEmpty();

        List<String> nomes = materias.stream().map(m -> m.nome()).toList();
        assertThat(nomes)
                .contains("ADMINISTRAÇÃO GERAL")
                .contains("LÓGICA DE PROGRAMAÇÃO")
                .contains("CONTABILIDADE GERAL");

        // O conteúdo geral é compartilhado por todos os cargos, então as
        // matérias dele não têm unidade; as específicas têm.
        assertThat(materias).filteredOn(m -> m.nome().equals("ADMINISTRAÇÃO GERAL"))
                .allSatisfy(m -> assertThat(m.caminho()).anyMatch(c -> c.startsWith("CARGO")));

        // E cada matéria tem conteúdo: o diagnóstico mostrou que a falha
        // anterior era justamente não ter nada.
        assertThat(materias).allSatisfy(m -> assertThat(m.topicos()).isNotEmpty());

        // Os tópicos são os itens de conteúdo, não os rótulos do documento.
        var administracaoGeral = materias.stream()
                .filter(m -> m.nome().equals("ADMINISTRAÇÃO GERAL"))
                .findFirst()
                .orElseThrow();
        List<String> topicos = administracaoGeral.topicos().stream()
                .map(TopicoRascunho::nome)
                .toList();
        assertThat(topicos).anyMatch(t -> t.contains("Evolução da administração"));
    }

    @Test
    @DisplayName("Ponta Porã: o nome da matéria não é o número do item")
    void pontaPoraNaoInventaMateriaDeRotulo() throws Exception {
        var materias = extrair(PONTA_PORA);

        assertThat(materias).extracting("nome")
                .doesNotContain("CARGO 1: ANALISTA DE LICITAÇÃO E CONTRATOS")
                .doesNotContain("CONHECIMENTOS GERAIS")
                .doesNotContain("CONHECIMENTOS ESPECÍFICOS");
    }

    // ------------------------------------------------- documento 2 e 3

    @Test
    @DisplayName("Ponta Porã nº 2: mesma expectativa estrutural do documento 1")
    void pontaPora2MesmaEstrutura() throws Exception {
        var bloco = blocoDe(PONTA_PORA_2);

        assertThat(bloco.encontrado()).isTrue();
        assertThat(bloco.conteudoProgramatico()).contains("CONTABILIDADE GERAL");

        var materias = extrair(PONTA_PORA_2);

        assertThat(materias).isNotEmpty();
        assertThat(materias).extracting("nome")
                .contains("CONTABILIDADE GERAL", "CONTABILIDADE PÚBLICA", "AUDITORIA");
        assertThat(materias).allSatisfy(m -> assertThat(m.topicos()).isNotEmpty());
        assertThat(materias).allSatisfy(m ->
                assertThat(m.caminho()).anyMatch(c -> c.startsWith("CARGO")));
    }

    @Test
    @DisplayName("Ilhabela: disciplina em linha própria terminando em dois-pontos")
    void ilhabelaDisciplinaComDoisPontos() throws Exception {
        var bloco = blocoDe(ILHABELA);

        assertThat(bloco.encontrado()).isTrue();
        assertThat(bloco.conteudoProgramatico()).contains("LÍNGUA PORTUGUESA:");

        var materias = extrair(ILHABELA);

        assertThat(materias).isNotEmpty();
        assertThat(materias).extracting("nome")
                .contains("LÍNGUA PORTUGUESA", "Direito Administrativo", "Direito Tributário");

        // Cada matéria tem conteúdo: a falha anterior era não ter nada.
        assertThat(materias).allSatisfy(m -> assertThat(m.topicos()).isNotEmpty());

        // As disciplinas do primeiro cargo são atribuídas a ele, e não ao
        // último cargo visto nem a um agrupamento solto.
        assertThat(materias)
                .filteredOn(m -> m.nome().equals("Direito Administrativo"))
                .allSatisfy(m -> assertThat(m.caminho())
                        .anyMatch(c -> c.startsWith("CARGO")));
    }

    @Test
    @DisplayName("Ilhabela: o cargo de cada matéria é o do bloco, não o último visto")
    void ilhabelaCargoCorreto() throws Exception {
        var materias = extrair(ILHABELA);

        // Cada cargo tem sua própria LÍNGUA PORTUGUESA: o conteúdo de uma não
        // pode ficar atribuído à outra.
        var doPrimeiroCargo = materias.stream()
                .filter(m -> m.nome().equals("LÍNGUA PORTUGUESA"))
                .filter(m -> m.caminho().stream().anyMatch(c -> c.contains("301")))
                .toList();

        assertThat(doPrimeiroCargo).isNotEmpty();
    }

    @Test
    @DisplayName("Ilhabela: limitação conhecida — agrupamento do cargo anterior vaza para o seguinte")
    void ilhabelaLimitacaoConhecida() throws Exception {
        // Comportamento aceito, não desejado, e medido com precisão.
        //
        // O documento separa os cargos em blocos de agrupamento:
        //
        //   CARGO: 301 – ADVOGADO
        //   LÍNGUA PORTUGUESA:
        //   CONHECIMENTOS ESPECÍFICOS:
        //   Direito Administrativo:
        //   ...
        //   CARGO: 302 – CONTROLE INTERNO     <- novo cargo
        //   LÍNGUA PORTUGUESA:
        //
        // O agrupamento "CONHECIMENTOS ESPECÍFICOS:" abre um bloco que continua
        // válido no cargo 302 (o edital lista as disciplinas do cargo seguinte
        // sob o mesmo bloco), mas o caminho fica com a ordem invertida:
        // [CONHECIMENTOS ESPECÍFICOS:, CARGO: 302] em vez de
        // [CARGO: 302, CONHECIMENTOS ESPECÍFICOS:].
        //
        // A causa é o caminho ser uma lista na ordem de aparição, sem noção de
        // profundidade. Corrigir isso pede a árvore tipada, que é justamente a
        // 02.0.14 — por isso a limitação fica registrada, não remendada aqui.
        var materias = extrair(ILHABELA);

        var doCargo302 = materias.stream()
                .filter(m -> m.caminho().stream().anyMatch(c -> c.contains("302")))
                .toList();

        assertThat(doCargo302).isNotEmpty();

        assertThat(doCargo302)
                .as("o agrupamento aparece antes do cargo, invertendo a hierarquia")
                .allSatisfy(m -> assertThat(m.caminho().get(0))
                        .startsWith("CONHECIMENTOS"));

        // O cargo 301, cujo bloco abre na ordem natural, sai correto.
        var doCargo301 = materias.stream()
                .filter(m -> m.caminho().stream().anyMatch(c -> c.contains("301")))
                .toList();

        assertThat(doCargo301)
                .as("quando o cargo vem antes do agrupamento, a ordem está certa")
                .allSatisfy(m -> assertThat(m.caminho().get(0)).startsWith("CARGO"));
    }

    // ------------------------------------------------------------- helper

    private SegmentadorEdital.SegmentoEdital blocoDe(String arquivo) throws Exception {
        Path pdf = exigir(arquivo);
        return segmentador.segmentar(DocumentModel.de(extrairTexto(pdf)).textoLimpo());
    }

    private List<MateriaRascunho> extrair(String arquivo) throws Exception {

        Path pdf = exigir(arquivo);
        DocumentModel documento = DocumentModel.de(extrairTexto(pdf));
        var bloco = segmentador.segmentar(documento.textoLimpo());

        return extrator.extrair(documento, bloco.conteudoProgramatico());
    }

    private Path exigir(String arquivo) {
        Path pdf = DIRETORIO.resolve(arquivo);
        Assumptions.assumeTrue(Files.exists(pdf),
                "PDF_edital_teste ausente: aceitação do Perfil B não pode rodar aqui.");
        return pdf;
    }

    private String extrairTexto(Path pdf) throws Exception {
        try (PDDocument documento = Loader.loadPDF(pdf.toFile())) {
            return new PDFTextStripper().getText(documento);
        }
    }
}
