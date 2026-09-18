package br.com.marcosbassetto.concursos.domain.edital.interpretacao;

import br.com.marcosbassetto.concursos.domain.edital.dto.EstruturaEditalDTO;
import br.com.marcosbassetto.concursos.domain.edital.dto.MateriaExtraida;
import br.com.marcosbassetto.concursos.domain.edital.interpretacao.classificacao.ClassificadorUnidadeEdital;
import br.com.marcosbassetto.concursos.domain.edital.interpretacao.classificacao.MontadorHierarquia;
import br.com.marcosbassetto.concursos.domain.edital.interpretacao.documento.DocumentModel;
import br.com.marcosbassetto.concursos.domain.edital.interpretacao.estrategia.EstrategiaBlocoConteudo;
import br.com.marcosbassetto.concursos.domain.edital.interpretacao.estrategia.EstrategiaDocumentoSemConteudo;
import br.com.marcosbassetto.concursos.domain.edital.interpretacao.estrategia.EstrategiaFactory;
import br.com.marcosbassetto.concursos.domain.edital.interpretacao.metadados.ExtratorMetadadosEdital;
import br.com.marcosbassetto.concursos.domain.edital.interpretacao.perfil.EditalProfile;
import br.com.marcosbassetto.concursos.domain.edital.interpretacao.perfil.EditalProfileClassifier;
import br.com.marcosbassetto.concursos.domain.edital.interpretacao.validacao.AvaliadorQualidadeExtracao;
import br.com.marcosbassetto.concursos.domain.edital.segmenter.SegmentadorEdital;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

/**
 * Diagnóstico temporário: mede o que o interpretador atual faz com cada edital
 * real de {@code PDF_edital_teste}. Não é teste de aceitação — serve para
 * escolher objetivamente o segundo perfil.
 */
class DiagnosticoPerfisReaisTest {

    private static final Path DIRETORIO = Path.of("PDF_edital_teste");
    private static final Path SAIDA = Path.of("build/diagnostico-perfis.tsv");

    private final EditalProfileClassifier classificadorPerfil = new EditalProfileClassifier();
    private final EstrategiaFactory estrategiaFactory = new EstrategiaFactory(List.of(
            new EstrategiaBlocoConteudo(
                    new SegmentadorEdital(),
                    new MontadorHierarquia(new ClassificadorUnidadeEdital())),
            new EstrategiaDocumentoSemConteudo()));

    private final InterpretadorEditalFacade facade = new InterpretadorEditalFacade(
            classificadorPerfil,
            estrategiaFactory,
            new ExtratorMetadadosEdital(),
            new AvaliadorQualidadeExtracao());

    @Test
    void medirPerfis() throws Exception {
        List<Path> pdfs = Files.list(DIRETORIO)
                .filter(p -> p.toString().endsWith(".pdf"))
                .sorted(Comparator.comparing(Path::toString))
                .toList();

        StringBuilder tsv = new StringBuilder();
        tsv.append("pdf\tperfil\thierarquia\tstatus\tmaterias\ttopicos\tcargo\tarea\tcurso\tcabecalhos\tamostra\n");

        for (Path pdf : pdfs) {
            String texto = extrair(pdf);
            DocumentModel documento = DocumentModel.de(texto);

            EstruturaEditalDTO estrutura = facade.interpretar(texto);

            List<MateriaExtraida> materias = estrutura.materiasDoPrimeiroCurso();
            int topicos = materias.stream().mapToInt(m -> m.topicos().size()).sum();

            tsv.append(pdf.getFileName()).append('\t')
                    .append(perfil(documento)).append('\t')
                    .append(hierarquia(documento)).append('\t')
                    .append(estrutura.status()).append('\t')
                    .append(materias.size()).append('\t')
                    .append(topicos).append('\t')
                    .append(ocorrencias(documento, "CARGO")).append('\t')
                    .append(ocorrencias(documento, "AREA")).append('\t')
                    .append(ocorrencias(documento, "CURSO")).append('\t')
                    .append(documento.frequenciaLinhasMaiusculas().size()).append('\t')
                    .append(materias.stream().limit(5)
                            .map(m -> m.nome() + "[" + m.topicos().size() + "]").toList())
                    .append('\n');
        }

        Files.createDirectories(SAIDA.getParent());
        Files.writeString(SAIDA, tsv.toString());
    }

    private String perfil(DocumentModel documento) {
        return classificadorPerfil.classificar(documento).codigo();
    }

    private String hierarquia(DocumentModel documento) {
        return classificadorPerfil.classificar(documento).hierarquia().niveis().toString();
    }

    private int ocorrencias(DocumentModel documento, String rotulo) {
        return (int) documento.linhas().stream()
                .filter(l -> l.strip().toUpperCase().startsWith(rotulo + ":")
                        || l.strip().toUpperCase().startsWith(rotulo + " "))
                .count();
    }

    private String extrair(Path pdf) throws Exception {
        try (PDDocument documento = Loader.loadPDF(pdf.toFile())) {
            return new PDFTextStripper().getText(documento);
        }
    }
}
