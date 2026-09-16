package br.com.marcosbassetto.concursos.application.edital;

import br.com.marcosbassetto.concursos.common.enums.Status;
import br.com.marcosbassetto.concursos.domain.concurso.entity.ConcursoEntity;
import br.com.marcosbassetto.concursos.domain.concurso.repository.ConcursoRepository;
import br.com.marcosbassetto.concursos.domain.edital.domain.StatusProcessamento;
import br.com.marcosbassetto.concursos.domain.edital.entity.EditalImportacaoEntity;
import br.com.marcosbassetto.concursos.domain.edital.entity.MateriaSugeridaEntity;
import br.com.marcosbassetto.concursos.domain.edital.entity.TopicoSugeridoEntity;
import br.com.marcosbassetto.concursos.domain.edital.repository.EditalImportacaoRepository;
import br.com.marcosbassetto.concursos.domain.edital.repository.MateriaSugeridaRepository;
import br.com.marcosbassetto.concursos.domain.edital.repository.TopicoSugeridoRepository;
import br.com.marcosbassetto.concursos.domain.usuario.entity.UsuarioEntity;
import br.com.marcosbassetto.concursos.domain.usuario.repository.UsuarioRepository;
import br.com.marcosbassetto.concursos.infrastructure.hash.HashService;
import br.com.marcosbassetto.concursos.infrastructure.pdf.PdfTextExtractorService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Processamento real contra PostgreSQL, sem mocks: PDF de verdade passando
 * por PdfTextExtractorService e pelos extratores determinísticos.
 *
 * A importação é montada direto nos repositórios, sem passar por
 * IniciarImportacaoUseCase: aquele caminho publica ImportacaoIniciadaEvent,
 * que dispara o listener @Async e faria o teste concorrer com um segundo
 * processamento sobre o mesmo staging.
 */
@SpringBootTest
class ProcessarEditalUseCaseTest {

    @Autowired
    private ProcessarEditalUseCase processarEditalUseCase;

    @Autowired
    private EditalStagingService stagingService;

    @Autowired
    private EditalImportacaoRepository importacaoRepository;

    @Autowired
    private MateriaSugeridaRepository materiaSugeridaRepository;

    @Autowired
    private TopicoSugeridoRepository topicoSugeridoRepository;

    @Autowired
    private ConcursoRepository concursoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PdfTextExtractorService pdfTextExtractor;

    @Autowired
    private HashService hashService;

    private final List<Long> concursosCriados = new ArrayList<>();
    private final List<Long> usuariosCriados = new ArrayList<>();

    @AfterEach
    void limpar() {
        for (Long concursoId : concursosCriados) {
            importacaoRepository.findByConcurso_Id(concursoId)
                    .ifPresent(importacaoRepository::delete);
            concursoRepository.findById(concursoId).ifPresent(concursoRepository::delete);
        }
        usuariosCriados.forEach(usuarioRepository::deleteById);

        concursosCriados.clear();
        usuariosCriados.clear();
    }

    @Test
    @DisplayName("processa o edital e grava matérias e tópicos no staging")
    void deveProcessarEGravarStaging() throws Exception {
        Long concursoId = prepararImportacao(EditalFixture.PADRAO);

        processarEditalUseCase.processar(concursoId);

        ConcursoEntity concurso = concursoRepository.findById(concursoId).orElseThrow();
        assertThat(concurso.getStatusProcessamento())
                .isEqualTo(StatusProcessamento.AGUARDANDO_REVISAO);
        // Dados cadastrais extraídos completam o concurso.
        assertThat(concurso.getBanca()).isEqualTo("VUNESP");
        assertThat(concurso.getAno()).isEqualTo(2026);

        EditalImportacaoEntity importacao =
                importacaoRepository.findByConcurso_Id(concursoId).orElseThrow();
        assertThat(importacao.getStatus()).isEqualTo(StatusProcessamento.AGUARDANDO_REVISAO);
        assertThat(importacao.getExtraidoEm()).isNotNull();
        assertThat(importacao.getMensagemErro()).isNull();

        List<MateriaSugeridaEntity> materias =
                materiaSugeridaRepository.findByImportacao_IdOrderByOrdemAsc(importacao.getId());

        assertThat(materias).extracting(MateriaSugeridaEntity::getNome)
                .contains("Lingua Portuguesa", "Direito Constitucional");

        MateriaSugeridaEntity portugues = materias.stream()
                .filter(m -> m.getNome().equals("Lingua Portuguesa"))
                .findFirst().orElseThrow();

        List<TopicoSugeridoEntity> topicos = topicoSugeridoRepository
                .findByMateriaSugerida_IdOrderByOrdemAsc(portugues.getId());

        assertThat(topicos).extracting(TopicoSugeridoEntity::getNome)
                .anyMatch(n -> n.contains("Concordancia"));
        assertThat(topicos).allMatch(t -> t.getOrdem() > 0);
    }

    @Test
    @DisplayName("sinaliza duplicidade sem mesclar nada automaticamente")
    void deveSinalizarDuplicidadeSemMesclar() throws Exception {
        Long concursoId = prepararImportacao(EditalFixture.COM_DUPLICIDADE);

        processarEditalUseCase.processar(concursoId);

        EditalImportacaoEntity importacao =
                importacaoRepository.findByConcurso_Id(concursoId).orElseThrow();
        List<MateriaSugeridaEntity> materias =
                materiaSugeridaRepository.findByImportacao_IdOrderByOrdemAsc(importacao.getId());

        // As duas matérias continuam existindo: mesclar é decisão do usuário.
        // Só a ocorrência repetida é sinalizada; a primeira vira a referência.
        assertThat(materias).hasSize(2);
        assertThat(materias).filteredOn(m -> Boolean.TRUE.equals(m.getPossivelDuplicidade()))
                .hasSize(1);

        MateriaSugeridaEntity duplicada = materias.stream()
                .filter(m -> Boolean.TRUE.equals(m.getPossivelDuplicidade()))
                .findFirst().orElseThrow();

        assertThat(duplicada.getSimilarAId()).isNotNull();
        assertThat(duplicada.getSimilarAId()).isNotEqualTo(duplicada.getId());
        assertThat(materias).anyMatch(m ->
                m.getId().equals(duplicada.getSimilarAId())
                        && !Boolean.TRUE.equals(m.getPossivelDuplicidade()));
    }

    @Test
    @DisplayName("grava status ERRO com mensagem quando o texto sumiu")
    void deveRegistrarErroQuandoTextoAusente() throws Exception {
        Long concursoId = prepararImportacao(EditalFixture.PADRAO);

        ConcursoEntity concurso = concursoRepository.findById(concursoId).orElseThrow();
        concurso.setTextoExtraido(null);
        concursoRepository.save(concurso);

        processarEditalUseCase.processar(concursoId);

        ConcursoEntity apos = concursoRepository.findById(concursoId).orElseThrow();
        assertThat(apos.getStatusProcessamento()).isEqualTo(StatusProcessamento.ERRO);

        EditalImportacaoEntity importacao =
                importacaoRepository.findByConcurso_Id(concursoId).orElseThrow();
        assertThat(importacao.getStatus()).isEqualTo(StatusProcessamento.ERRO);
        assertThat(importacao.getMensagemErro()).contains("texto do edital");
        assertThat(importacao.getExtraidoEm()).isNull();
    }

    @Test
    @DisplayName("reprocessar substitui o staging anterior em vez de acumular")
    void reprocessarNaoAcumulaStaging() throws Exception {
        Long concursoId = prepararImportacao(EditalFixture.PADRAO);

        processarEditalUseCase.processar(concursoId);
        EditalImportacaoEntity importacao =
                importacaoRepository.findByConcurso_Id(concursoId).orElseThrow();

        int primeiraExecucao = materiaSugeridaRepository
                .findByImportacao_IdOrderByOrdemAsc(importacao.getId()).size();

        stagingService.processar(concursoId);

        int segundaExecucao = materiaSugeridaRepository
                .findByImportacao_IdOrderByOrdemAsc(importacao.getId()).size();

        assertThat(primeiraExecucao).isPositive();
        assertThat(segundaExecucao).isEqualTo(primeiraExecucao);
    }

    @Test
    @Transactional
    @DisplayName("processamento deixa o staging visível junto com o status")
    void processamentoCommutaStatusParaAguardandoRevisao() throws Exception {
        Long concursoId = prepararImportacao(EditalFixture.PADRAO);

        stagingService.processar(concursoId);

        EditalImportacaoEntity importacao =
                importacaoRepository.findByConcurso_IdAndStatus(
                        concursoId, StatusProcessamento.AGUARDANDO_REVISAO).orElseThrow();

        assertThat(materiaSugeridaRepository
                .findByImportacao_IdOrderByOrdemAsc(importacao.getId())).isNotEmpty();
    }

    /** Monta concurso + importação com o texto já extraído, sem publicar evento. */
    private Long prepararImportacao(String textoEdital) throws Exception {
        UsuarioEntity usuario = usuarioRepository.save(UsuarioEntity.builder()
                .email("proc-" + UUID.randomUUID() + "@email.com")
                .nome("Usuário Processamento")
                .senhaHash("hash")
                .build());
        usuariosCriados.add(usuario.getId());

        byte[] pdf = EditalFixture.pdfComTexto(textoEdital);
        String texto = pdfTextExtractor.extractText(new java.io.ByteArrayInputStream(pdf));

        ConcursoEntity concurso = concursoRepository.save(ConcursoEntity.builder()
                .usuario(usuario)
                .nome("Concurso de Teste")
                .status(Status.ATIVO)
                .statusProcessamento(StatusProcessamento.RECEBIDO)
                .processado(false)
                .textoExtraido(texto)
                .criadoEm(LocalDateTime.now())
                .build());

        EditalImportacaoEntity importacao = importacaoRepository.save(
                EditalImportacaoEntity.builder()
                        .concurso(concurso)
                        .usuario(usuario)
                        .nomeArquivo("edital.pdf")
                        .hashSha256(hashService.calcularSha256(pdf))
                        .tamanhoBytes((long) pdf.length)
                        .status(StatusProcessamento.RECEBIDO)
                        .build());

        concurso.setImportacao(importacao);
        concursoRepository.save(concurso);

        concursosCriados.add(concurso.getId());
        return concurso.getId();
    }
}
