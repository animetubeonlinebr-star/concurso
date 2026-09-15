package br.com.marcosbassetto.concursos.infrastructure.pdf;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

@Service
public class PdfTextExtractorService {

    /**
     * Extrai texto a partir de um MultipartFile (uso típico em Controllers).
     */
    public String extractText(MultipartFile file) throws IOException {
        try (InputStream is = file.getInputStream()) {
            return extractText(is);
        }
    }


    public String extractText(InputStream is) throws IOException {
        try (PDDocument document = Loader.loadPDF(is.readAllBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }
}
