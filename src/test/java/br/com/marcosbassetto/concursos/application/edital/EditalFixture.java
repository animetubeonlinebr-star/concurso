package br.com.marcosbassetto.concursos.application.edital;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;

/**
 * PDFs de edital gerados em memória para os testes de importação.
 *
 * São PDFs reais (não mocks): o fluxo passa por PdfTextExtractorService e
 * pela extração determinística de verdade.
 */
final class EditalFixture {

    private EditalFixture() {
    }

    // Texto sem acentos: o PDFBox não codifica não-ASCII nas fontes
    // padrão, e o objetivo aqui é o pipeline, não a acentuação.
    static final String PADRAO = """
            CONCURSO PUBLICO PARA TECNICO JUDICIARIO - 2026
            ORGAO: Tribunal de Justica de Sao Paulo
            BANCA: VUNESP
            CARGO: Tecnico Judiciario

            ANEXO I - CONTEUDO PROGRAMATICO

            ## Lingua Portuguesa
            - Interpretacao de texto.
            - Concordancia verbal e nominal.

            ## Direito Constitucional
            - Principios fundamentais.
            - Controle de constitucionalidade.

            ANEXO II - CRONOGRAMA
            """;

    /** Duas matérias que o detector deve apontar como possíveis duplicidades. */
    static final String COM_DUPLICIDADE = """
            CONCURSO PUBLICO - 2026

            ANEXO I - CONTEUDO PROGRAMATICO

            ## Direito Constitucional
            - Principios fundamentais.

            ## Direito Constitucional
            - Direitos e garantias fundamentais.

            ANEXO II - CRONOGRAMA
            """;

    /**
     * Edital real sem bloco de conteúdo programático (roteiro item 9): o
     * arquivo é um PDF válido e legível, mas não há de onde extrair matérias.
     */
    static final String SEM_CONTEUDO_PROGRAMATICO = """
            EDITAL DE ABERTURA - CONCURSO PUBLICO 2026

            ORGAO: Tribunal de Justica de Sao Paulo
            BANCA: VUNESP
            CARGO: Tecnico Judiciario

            ANEXO I - DAS DISPOSICOES PRELIMINARES
            O concurso sera regido por este edital e destina-se a preencher
            vagas do quadro permanente.

            ANEXO II - DO CRONOGRAMA
            Inscricoes de 10 a 30 de janeiro de 2026.
            """;

    static MockMultipartFile arquivo(String texto) throws Exception {
        return new MockMultipartFile(
                "arquivo", "edital.pdf", "application/pdf", pdfComTexto(texto));
    }

    static byte[] pdfComTexto(String texto) throws Exception {
        try (PDDocument documento = new PDDocument()) {
            PDPage pagina = new PDPage();
            documento.addPage(pagina);

            try (PDPageContentStream conteudo = new PDPageContentStream(documento, pagina)) {
                conteudo.beginText();
                conteudo.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 11);
                conteudo.setLeading(14f);
                conteudo.newLineAtOffset(40, 760);

                for (String linha : texto.split("\n")) {
                    // PDFBox não aceita não-ASCII no Helvetica padrão.
                    conteudo.showText(paraAscii(linha));
                    conteudo.newLine();
                }
                conteudo.endText();
            }

            ByteArrayOutputStream saida = new ByteArrayOutputStream();
            documento.save(saida);
            return saida.toByteArray();
        }
    }

    private static String paraAscii(String linha) {
        String normalizada = java.text.Normalizer.normalize(linha, java.text.Normalizer.Form.NFD);
        return normalizada.replaceAll("\\p{M}", "");
    }
}
