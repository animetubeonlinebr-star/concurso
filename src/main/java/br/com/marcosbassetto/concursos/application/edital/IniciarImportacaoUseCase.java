package br.com.marcosbassetto.concursos.application.edital;

import br.com.marcosbassetto.concursos.common.enums.Status;
import br.com.marcosbassetto.concursos.common.exception.BusinessException;
import br.com.marcosbassetto.concursos.common.exception.ConflictException;
import br.com.marcosbassetto.concursos.common.exception.ErrorCodes;
import br.com.marcosbassetto.concursos.common.exception.ResourceNotFoundException;
import br.com.marcosbassetto.concursos.domain.concurso.entity.ConcursoEntity;
import br.com.marcosbassetto.concursos.domain.concurso.repository.ConcursoRepository;
import br.com.marcosbassetto.concursos.domain.edital.domain.StatusProcessamento;
import br.com.marcosbassetto.concursos.domain.edital.dto.IniciarImportacaoResponse;
import br.com.marcosbassetto.concursos.domain.edital.entity.EditalImportacaoEntity;
import br.com.marcosbassetto.concursos.domain.edital.repository.EditalImportacaoRepository;
import br.com.marcosbassetto.concursos.domain.usuario.entity.UsuarioEntity;
import br.com.marcosbassetto.concursos.domain.usuario.repository.UsuarioRepository;
import br.com.marcosbassetto.concursos.infrastructure.hash.HashService;
import br.com.marcosbassetto.concursos.infrastructure.pdf.PdfTextExtractorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Registra a importação de um edital: calcula o hash, recusa repetição do
 * mesmo arquivo pelo mesmo usuário e cria o concurso em estado RECEBIDO.
 *
 * O processamento em si não acontece aqui — este caso de uso apenas publica
 * {@link ImportacaoIniciadaEvent}, para o upload responder rápido.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IniciarImportacaoUseCase {

    private final ConcursoRepository concursoRepository;
    private final EditalImportacaoRepository importacaoRepository;
    private final UsuarioRepository usuarioRepository;
    private final HashService hashService;
    private final PdfTextExtractorService pdfTextExtractor;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public IniciarImportacaoResponse importar(UsuarioEntity usuario, MultipartFile arquivo) {
        byte[] conteudo = lerConteudo(arquivo);
        String hash = hashService.calcularSha256(conteudo);

        var jaImportado = importacaoRepository
                .findByUsuario_IdAndHashSha256(usuario.getId(), hash);

        if (jaImportado.isPresent()) {
            EditalImportacaoEntity existente = jaImportado.get();
            log.info("Edital já importado | usuarioId={} | concursoId={}",
                    usuario.getId(), existente.getConcursoId());

            throw new ConflictException(
                    ErrorCodes.EDITAL_JA_IMPORTADO,
                    "Este edital já foi importado. Abra o concurso existente para continuar de onde parou.",
                    Map.of("concursoId", existente.getConcursoId()));
        }

        String texto = extrairTexto(arquivo, conteudo);

        ConcursoEntity concurso = criarConcurso(usuario, arquivo.getOriginalFilename(), texto);

        EditalImportacaoEntity importacao = importacaoRepository.save(
                EditalImportacaoEntity.builder()
                        .concurso(concurso)
                        .usuario(usuario)
                        .nomeArquivo(nomeArquivoOuPadrao(arquivo))
                        .hashSha256(hash)
                        .tamanhoBytes((long) conteudo.length)
                        .status(StatusProcessamento.RECEBIDO)
                        .build());

        concurso.setImportacao(importacao);
        concursoRepository.save(concurso);

        log.info("Importação registrada | concursoId={} | importacaoId={} | bytes={}",
                concurso.getId(), importacao.getId(), conteudo.length);

        eventPublisher.publishEvent(
                new ImportacaoIniciadaEvent(concurso.getId(), importacao.getId()));

        return new IniciarImportacaoResponse(
                concurso.getId(), importacao.getId(), StatusProcessamento.RECEBIDO, false);
    }

    private byte[] lerConteudo(MultipartFile arquivo) {
        try {
            return arquivo.getBytes();
        } catch (IOException e) {
            throw new BusinessException(
                    ErrorCodes.ARQUIVO_INVALIDO,
                    "Não foi possível ler o arquivo enviado.", e);
        }
    }

    /**
     * O texto é extraído já no upload porque a importação precisa ficar
     * auditável mesmo que o processamento falhe depois: o PDF original não
     * é armazenado, então esta é a única cópia do conteúdo.
     */
    private String extrairTexto(MultipartFile arquivo, byte[] conteudo) {
        try {
            String texto = pdfTextExtractor.extractText(
                    new java.io.ByteArrayInputStream(conteudo));

            if (texto == null || texto.isBlank()) {
                throw new BusinessException(
                        ErrorCodes.ARQUIVO_INVALIDO,
                        "Não foi possível extrair texto do PDF. "
                                + "O arquivo pode estar corrompido ou ser uma imagem.");
            }
            return texto;
        } catch (IOException e) {
            throw new BusinessException(
                    ErrorCodes.ARQUIVO_INVALIDO,
                    "Não foi possível ler o PDF enviado.", e);
        }
    }

    private ConcursoEntity criarConcurso(UsuarioEntity usuario, String nomeOriginal, String texto) {
        return concursoRepository.save(
                ConcursoEntity.builder()
                        .usuario(usuario)
                        .nome(nomeConcursoDe(nomeOriginal))
                        .status(Status.ATIVO)
                        .statusProcessamento(StatusProcessamento.RECEBIDO)
                        .processado(false)
                        .textoExtraido(texto)
                        .criadoEm(LocalDateTime.now())
                        .build());
    }

    private String nomeConcursoDe(String nomeOriginal) {
        if (nomeOriginal == null || nomeOriginal.isBlank()) {
            return "Edital importado";
        }
        String semExtensao = nomeOriginal.replaceAll("(?i)\\.pdf$", "").strip();
        String nome = semExtensao.isBlank() ? "Edital importado" : semExtensao;
        return nome.length() > 200 ? nome.substring(0, 200) : nome;
    }

    private String nomeArquivoOuPadrao(MultipartFile arquivo) {
        String nome = arquivo.getOriginalFilename();
        return (nome == null || nome.isBlank()) ? "edital.pdf" : nome;
    }

    /** Garante que o usuário autenticado existe antes de criar o concurso. */
    public UsuarioEntity buscarUsuario(Long usuarioId) {
        return usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", "id", usuarioId));
    }
}
