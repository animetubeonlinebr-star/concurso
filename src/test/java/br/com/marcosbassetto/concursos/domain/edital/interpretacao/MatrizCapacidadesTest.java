package br.com.marcosbassetto.concursos.domain.edital.interpretacao;

import br.com.marcosbassetto.concursos.domain.edital.dto.EstruturaEditalDTO;
import br.com.marcosbassetto.concursos.domain.edital.dto.MateriaExtraida;
import br.com.marcosbassetto.concursos.domain.edital.interpretacao.classificacao.ClassificadorUnidadeEdital;
import br.com.marcosbassetto.concursos.domain.edital.interpretacao.classificacao.MontadorHierarquia;
import br.com.marcosbassetto.concursos.domain.edital.interpretacao.disciplina.ExtratorDisciplinas;
import br.com.marcosbassetto.concursos.domain.edital.interpretacao.disciplina.ExtratorDisciplinasInline;
import br.com.marcosbassetto.concursos.domain.edital.interpretacao.documento.DocumentModel;
import br.com.marcosbassetto.concursos.domain.edital.interpretacao.estrategia.EstrategiaBlocoConteudo;
import br.com.marcosbassetto.concursos.domain.edital.interpretacao.estrategia.EstrategiaDocumentoSemConteudo;
import br.com.marcosbassetto.concursos.domain.edital.interpretacao.estrategia.EstrategiaFactory;
import br.com.marcosbassetto.concursos.domain.edital.interpretacao.metadados.ExtratorMetadadosEdital;
import br.com.marcosbassetto.concursos.domain.edital.interpretacao.perfil.EditalProfileClassifier;
import br.com.marcosbassetto.concursos.domain.edital.interpretacao.validacao.AvaliadorQualidadeExtracao;
import br.com.marcosbassetto.concursos.domain.edital.segmenter.AncoraAgrupamentoConhecimentos;
import br.com.marcosbassetto.concursos.domain.edital.segmenter.AncoraBlocoConteudo;
import br.com.marcosbassetto.concursos.domain.edital.segmenter.AncoraTituloExplicito;
import br.com.marcosbassetto.concursos.domain.edital.segmenter.AncoraTituloNumerado;
import br.com.marcosbassetto.concursos.domain.edital.segmenter.SegmentadorEdital;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

/**
 * Instrumento da 02.0.11: para cada edital real, quais capacidades foram
 * efetivamente usadas para lê-lo.
 *
 * O diagnóstico de perfil (02.0.9) diz o resultado — quantas matérias e
 * tópicos. Este diz o caminho: qual âncora venceu, qual leitura de disciplina
 * venceu e quanto cada leitura concorrente teria produzido. É o que permite
 * decidir o que é comum e o que varia por formato, em vez de inferir isso da
 * contagem final.
 *
 * Não é teste de aceitação: grava {@code build/matriz-capacidades.tsv} e passa
 * independentemente da qualidade da extração.
 */
class MatrizCapacidadesTest {

    private static final Path DIRETORIO = Path.of("PDF_edital_teste");
    private static final Path SAIDA = Path.of("build/matriz-capacidades.tsv");

    private final EditalProfileClassifier classificadorPerfil = new EditalProfileClassifier();
    private final MontadorHierarquia leituraLinha = new MontadorHierarquia(new ClassificadorUnidadeEdital());
    private final ExtratorDisciplinasInline leituraInline = new ExtratorDisciplinasInline();
    private final ExtratorDisciplinas extrator = new ExtratorDisciplinas(leituraLinha, leituraInline);

    private final List<AncoraBlocoConteudo> ancoras = List.of(
            new AncoraTituloExplicito(),
            new AncoraAgrupamentoConhecimentos(),
            new AncoraTituloNumerado());

    private final InterpretadorEditalFacade facade;

    MatrizCapacidadesTest() {
        SegmentadorEdital segmentador = SegmentadorEdital.comAncorasPadrao();
        this.facade = new InterpretadorEditalFacade(
                classificadorPerfil,
                new EstrategiaFactory(List.of(
                        new EstrategiaBlocoConteudo(segmentador, extrator),
                        new EstrategiaDocumentoSemConteudo())),
                new ExtratorMetadadosEdital(),
                new AvaliadorQualidadeExtracao());
    }

    @Test
    void gerarMatriz() throws Exception {
        List<Path> pdfs = Files.isDirectory(DIRETORIO)
                ? Files.list(DIRETORIO).filter(p -> p.toString().endsWith(".pdf"))
                        .sorted(Comparator.comparing(Path::toString)).toList()
                : List.of();

        Assumptions.assumeFalse(pdfs.isEmpty(), "PDF_edital_teste ausente.");

        StringBuilder tsv = new StringBuilder();
        tsv.append("pdf\tperfil\thierarquia\tancora\tancoraExplicita\tancoraAgrupamento\t")
                .append("ancoraNumerada\tleituraVencedora\tmatrizLinha\tmatrizInline\t")
                .append("status\tmaterias\ttopicos\tcomCaminho\tprofundidadeMaxima\n");

        SegmentadorEdital segmentador = SegmentadorEdital.comAncorasPadrao();

        for (Path pdf : pdfs) {
            String texto = extrair(pdf);
            DocumentModel documento = DocumentModel.de(texto);

            EstruturaEditalDTO estrutura = facade.interpretar(texto);
            List<MateriaExtraida> materias = estrutura.materiasDoPrimeiroCurso();

            var bloco = segmentador.segmentar(documento.textoLimpo());
            String conteudo = bloco.conteudoProgramatico();

            int nLinha = bloco.encontrado() ? leituraLinha.montar(documento, conteudo).size() : 0;
            int nInline = bloco.encontrado() ? leituraInline.extrair(conteudo).size() : 0;

            String leitura = !bloco.encontrado() ? "SEM_BLOCO"
                    : (nInline > nLinha ? "INLINE" : "LINHA_PROPRIA");

            tsv.append(pdf.getFileName()).append('\t')
                    .append(classificadorPerfil.classificar(documento).codigo()).append('\t')
                    .append(classificadorPerfil.classificar(documento).hierarquia().profundidade())
                    .append('\t')
                    .append(ancoraVencedora(documento)).append('\t')
                    .append(casa(new AncoraTituloExplicito(), documento)).append('\t')
                    .append(casa(new AncoraAgrupamentoConhecimentos(), documento)).append('\t')
                    .append(casa(new AncoraTituloNumerado(), documento)).append('\t')
                    .append(leitura).append('\t')
                    .append(nLinha).append('\t')
                    .append(nInline).append('\t')
                    .append(estrutura.status()).append('\t')
                    .append(materias.size()).append('\t')
                    .append(materias.stream().mapToInt(m -> m.topicos().size()).sum()).append('\t')
                    .append(materias.size()).append('\t')
                    .append(classificadorPerfil.classificar(documento).hierarquia().profundidade())
                    .append('\n');
        }

        Files.createDirectories(SAIDA.getParent());
        Files.writeString(SAIDA, tsv.toString());
    }

    /** Qual âncora reconheceu o documento, na ordem de preferência. */
    private String ancoraVencedora(DocumentModel documento) {
        return ancoras.stream()
                .filter(a -> a.encontrarInicio(documento) >= 0)
                .map(AncoraBlocoConteudo::nome)
                .findFirst()
                .orElse("NENHUMA");
    }

    private int casa(AncoraBlocoConteudo ancora, DocumentModel documento) {
        return ancora.encontrarInicio(documento) >= 0 ? 1 : 0;
    }

    private String extrair(Path pdf) throws Exception {
        try (PDDocument documento = Loader.loadPDF(pdf.toFile())) {
            return new PDFTextStripper().getText(documento);
        }
    }
}
