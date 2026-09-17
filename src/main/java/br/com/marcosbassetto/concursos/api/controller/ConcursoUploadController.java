package br.com.marcosbassetto.concursos.api.controller;

import br.com.marcosbassetto.concursos.application.edital.IniciarImportacaoUseCase;
import br.com.marcosbassetto.concursos.common.exception.BusinessException;
import br.com.marcosbassetto.concursos.common.exception.ErrorCodes;
import br.com.marcosbassetto.concursos.common.security.UsuarioAutenticadoResolver;
import br.com.marcosbassetto.concursos.domain.edital.dto.CursoExtraido;
import br.com.marcosbassetto.concursos.domain.edital.dto.EstruturaEditalDTO;
import br.com.marcosbassetto.concursos.domain.edital.dto.IniciarImportacaoResponse;
import br.com.marcosbassetto.concursos.domain.edital.interpretacao.InterpretadorEditalFacade;
import br.com.marcosbassetto.concursos.domain.usuario.entity.UsuarioEntity;
import br.com.marcosbassetto.concursos.infrastructure.pdf.PdfTextExtractorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/api/v1/concursos")
@RequiredArgsConstructor
public class ConcursoUploadController {

    private final PdfTextExtractorService pdfExtractor;
    private final InterpretadorEditalFacade interpretador;
    private final IniciarImportacaoUseCase iniciarImportacaoUseCase;
    private final UsuarioAutenticadoResolver usuarioResolver;

    /**
     * Fluxo real: persiste a importação, deduplica por hash e devolve o
     * {@code concursoId} que o frontend usa nas rotas
     * {@code /concursos/{id}/processando|revisao|conteudo}.
     *
     * Responde 202 porque a extração continua em background.
     */
    @PostMapping("/importar")
    public ResponseEntity<IniciarImportacaoResponse> importarEdital(
            @RequestParam("arquivo") MultipartFile arquivo,
            Authentication auth
    ) {
        validarArquivo(arquivo);

        UsuarioEntity usuario = iniciarImportacaoUseCase.buscarUsuario(
                usuarioResolver.resolverUsuarioId(auth));

        IniciarImportacaoResponse resposta =
                iniciarImportacaoUseCase.importar(usuario, arquivo);

        return ResponseEntity.accepted().body(resposta);
    }

    /**
     * Pré-visualização sem persistência: devolve a estrutura para o usuário
     * conferir antes de decidir importar. O caminho que grava dados é
     * {@code /importar}.
     *
     * Usa a mesma interpretação do fluxo persistido. Enquanto chamava o
     * extrator legado, o mesmo arquivo podia mostrar uma estrutura na tela de
     * pré-visualização e outra na revisão — e só a segunda era real.
     */
    @PostMapping("/upload")
    public ResponseEntity<EstruturaEditalDTO> uploadEdital(
            @RequestParam("arquivo") MultipartFile file
    ) throws IOException {

        log.info("Recebendo upload de edital (pré-visualização): {}", file.getOriginalFilename());

        validarArquivo(file);

        String texto = pdfExtractor.extractText(file);
        log.debug("Texto extraído com {} caracteres", texto != null ? texto.length() : 0);

        if (texto == null || texto.trim().isEmpty()) {
            throw new BusinessException(
                    ErrorCodes.ARQUIVO_INVALIDO,
                    "Não foi possível extrair texto do PDF. O arquivo pode estar corrompido ou ser uma imagem.");
        }

        EstruturaEditalDTO estrutura = interpretador.interpretar(texto);

        logarEstrutura(estrutura);

        return ResponseEntity.ok(estrutura);
    }

    private void validarArquivo(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(
                    ErrorCodes.ARQUIVO_INVALIDO,
                    "O arquivo enviado está vazio.");
        }

        String nome = file.getOriginalFilename();
        boolean extensaoPdf = nome != null && nome.toLowerCase().endsWith(".pdf");

        if (!"application/pdf".equals(file.getContentType()) && !extensaoPdf) {
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
