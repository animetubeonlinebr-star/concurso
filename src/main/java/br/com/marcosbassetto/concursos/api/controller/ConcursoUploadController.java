package br.com.marcosbassetto.concursos.api.controller;

import br.com.marcosbassetto.concursos.common.exception.BusinessException;
import br.com.marcosbassetto.concursos.common.exception.ErrorCodes;
import br.com.marcosbassetto.concursos.domain.edital.domain.Banca;
import br.com.marcosbassetto.concursos.domain.edital.domain.PadraoEdital;
import br.com.marcosbassetto.concursos.domain.edital.dto.CursoExtraido;
import br.com.marcosbassetto.concursos.domain.edital.dto.EstruturaEditalDTO;
import br.com.marcosbassetto.concursos.domain.edital.extractor.ExtratorEditalFacade;
import br.com.marcosbassetto.concursos.infrastructure.pdf.PdfTextExtractorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/api/v1/concursos")
@RequiredArgsConstructor
public class ConcursoUploadController {

    private final PdfTextExtractorService pdfExtractor;
    private final ExtratorEditalFacade extratorFacade;

    @PostMapping("/upload")
    public ResponseEntity<EstruturaEditalDTO> uploadEdital(
            @RequestParam("arquivo") MultipartFile file,
            @RequestParam(value = "banca", required = false) Banca banca,
            @RequestParam(value = "padrao", required = false) PadraoEdital padrao
    ) throws Exception {

        log.info("📄 Recebendo upload de edital: {}", file.getOriginalFilename());

        validarArquivo(file);

        String texto = pdfExtractor.extractText(file);
        log.debug("📄 Texto extraído com {} caracteres", texto != null ? texto.length() : 0);

        if (texto == null || texto.trim().isEmpty()) {
            throw new BusinessException(
                    ErrorCodes.ARQUIVO_INVALIDO,
                    "Não foi possível extrair texto do PDF. O arquivo pode estar corrompido ou ser uma imagem.");
        }

        EstruturaEditalDTO estrutura = extratorFacade.extrair(texto, banca, padrao);

        logarEstrutura(estrutura);

        return ResponseEntity.ok(estrutura);
    }

    private void validarArquivo(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException(
                    ErrorCodes.ARQUIVO_INVALIDO,
                    "O arquivo enviado está vazio.");
        }
        if (!"application/pdf".equals(file.getContentType())
                && !file.getOriginalFilename().toLowerCase().endsWith(".pdf")) {
            throw new BusinessException(
                    ErrorCodes.TIPO_ARQUIVO_NAO_SUPORTADO,
                    "Apenas arquivos PDF são suportados.");
        }
    }

    private void logarEstrutura(EstruturaEditalDTO estrutura) {
        log.info("===== ESTRUTURA EXTRAÍDA =====");

        if (estrutura.dadosConcurso() != null) {
            var d = estrutura.dadosConcurso();
            log.info("Nome:   {}", d.nome());
            log.info("Banca:  {}", d.banca());
            log.info("Órgão:  {}", d.orgao());
            log.info("Ano:    {}", d.ano());
        } else {
            log.info("Dados do concurso: não identificados");
        }

        log.info("Status: {}", estrutura.status());
        log.info("Cursos: {}", estrutura.cursos().size());

        estrutura.cursos().stream().limit(5).forEach(this::logarCurso);

        if (estrutura.mensagem() != null) {
            log.info("Mensagem: {}", estrutura.mensagem());
        }
    }

    private void logarCurso(CursoExtraido curso) {
        log.info("  Curso: {} | Matérias: {}", curso.nome(), curso.materias().size());
        curso.materias().stream().limit(3).forEach(m ->
                log.info("    - Matéria: {} | Tópicos: {}", m.nome(), m.topicos().size()));
    }
}
