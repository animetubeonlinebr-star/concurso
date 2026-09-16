package br.com.marcosbassetto.concursos.application.edital;

import br.com.marcosbassetto.concursos.common.enums.Status;
import br.com.marcosbassetto.concursos.common.exception.BusinessException;
import br.com.marcosbassetto.concursos.common.exception.ErrorCodes;
import br.com.marcosbassetto.concursos.domain.concurso.entity.ConcursoEntity;
import br.com.marcosbassetto.concursos.domain.concurso.repository.ConcursoRepository;
import br.com.marcosbassetto.concursos.domain.edital.domain.StatusProcessamento;
import br.com.marcosbassetto.concursos.domain.edital.dto.ConfirmacaoEstruturaResponse;
import br.com.marcosbassetto.concursos.domain.edital.dto.RevisaoEstruturaRequest;
import br.com.marcosbassetto.concursos.domain.edital.dto.RevisaoEstruturaResponse;
import br.com.marcosbassetto.concursos.domain.edital.entity.EditalImportacaoEntity;
import br.com.marcosbassetto.concursos.domain.edital.repository.EditalImportacaoRepository;
import br.com.marcosbassetto.concursos.domain.edital.repository.MateriaSugeridaRepository;
import br.com.marcosbassetto.concursos.domain.materia.domain.OrigemMateria;
import br.com.marcosbassetto.concursos.domain.materia.repository.MateriaRepository;
import br.com.marcosbassetto.concursos.domain.topico.repository.TopicoRepository;
import br.com.marcosbassetto.concursos.domain.usuario.entity.UsuarioEntity;
import br.com.marcosbassetto.concursos.domain.usuario.repository.UsuarioRepository;
import br.com.marcosbassetto.concursos.infrastructure.hash.HashService;
import br.com.marcosbassetto.concursos.infrastructure.pdf.PdfTextExtractorService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Fluxo completo de revisão e confirmação contra PostgreSQL real: staging
 * processado, edição do usuário, promoção para materia/topico.
 */
@SpringBootTest
class RevisaoEConfirmacaoIT {

    @Autowired
    private EditalStagingService stagingService;

    @Autowired
    private ConsultarRevisaoUseCase consultarRevisao;

    @Autowired
    private RevisarEstruturaUseCase revisarEstrutura;

    @Autowired
    private ConfirmarEstruturaUseCase confirmarEstrutura;

    @Autowired
    private EditalImportacaoRepository importacaoRepository;

    @Autowired
    private MateriaSugeridaRepository materiaSugeridaRepository;

    @Autowired
    private MateriaRepository materiaRepository;

    @Autowired
    private TopicoRepository topicoRepository;

    @Autowired
    private ConcursoRepository concursoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PdfTextExtractorService pdfTextExtractor;

    @Autowired
    private HashService hashService;

    private final List<Preparo> criados = new ArrayList<>();

    @AfterEach
    void limpar() {
        for (Preparo preparo : criados) {
            materiaRepository.findByConcursoIdAndUsuarioId(preparo.concursoId(), preparo.usuarioId())
                    .forEach(materiaRepository::delete);
            importacaoRepository.findByConcurso_Id(preparo.concursoId())
                    .ifPresent(importacaoRepository::delete);
            concursoRepository.findById(preparo.concursoId())
                    .ifPresent(concursoRepository::delete);
            usuarioRepository.findById(preparo.usuarioId())
                    .ifPresent(usuarioRepository::delete);
        }
        criados.clear();
    }

    @Test
    @DisplayName("consulta a revisão com as sugestões sinalizadas")
    void deveConsultarRevisao() throws Exception {
        Preparo preparo = preparar(EditalFixture.COM_DUPLICIDADE);

        RevisaoEstruturaResponse revisao =
                consultarRevisao.consultar(preparo.concursoId, preparo.usuarioId);

        assertThat(revisao.status()).isEqualTo(StatusProcessamento.AGUARDANDO_REVISAO);
        assertThat(revisao.materias()).hasSize(2);
        assertThat(revisao.materias())
                .filteredOn(m -> Boolean.TRUE.equals(m.possivelDuplicidade()))
                .hasSize(1)
                .allSatisfy(m -> assertThat(m.similarA()).isNotBlank());
    }

    @Test
    @DisplayName("mesclagem explícita junta os tópicos e descarta a origem")
    void deveMesclarPorAcaoDoUsuario() throws Exception {
        Preparo preparo = preparar(EditalFixture.COM_DUPLICIDADE);

        RevisaoEstruturaResponse antes =
                consultarRevisao.consultar(preparo.concursoId, preparo.usuarioId);

        var primeira = antes.materias().get(0);
        var segunda = antes.materias().get(1);

        revisarEstrutura.aplicar(preparo.concursoId, new RevisaoEstruturaRequest(List.of(
                new RevisaoEstruturaRequest.MateriaRevisaoRequest(
                        segunda.id(), segunda.nome(), true, primeira.id(), false, null),
                new RevisaoEstruturaRequest.MateriaRevisaoRequest(
                        primeira.id(), primeira.nome(), true, null, false, null)
        )), preparo.usuarioId);

        RevisaoEstruturaResponse depois =
                consultarRevisao.consultar(preparo.concursoId, preparo.usuarioId);

        assertThat(depois.materias()).hasSize(1);
        assertThat(depois.materias().get(0).id()).isEqualTo(primeira.id());
        // O tópico da matéria mesclada passa a pertencer ao destino.
        assertThat(depois.materias().get(0).topicos()).isNotEmpty();
    }

    @Test
    @DisplayName("confirmar persiste só o que ficou selecionado e encerra a importação")
    void deveConfirmarPersistindoSelecionadas() throws Exception {
        Preparo preparo = preparar(EditalFixture.PADRAO);

        RevisaoEstruturaResponse revisao =
                consultarRevisao.consultar(preparo.concursoId, preparo.usuarioId);

        // Apenas a primeira matéria fica selecionada.
        List<RevisaoEstruturaRequest.MateriaRevisaoRequest> pedidos = new ArrayList<>();
        for (int i = 0; i < revisao.materias().size(); i++) {
            var m = revisao.materias().get(i);
            pedidos.add(new RevisaoEstruturaRequest.MateriaRevisaoRequest(
                    m.id(), m.nome(), i == 0, null, i != 0, null));
        }

        revisarEstrutura.aplicar(preparo.concursoId,
                new RevisaoEstruturaRequest(pedidos), preparo.usuarioId);

        ConfirmacaoEstruturaResponse confirmacao =
                confirmarEstrutura.confirmar(preparo.concursoId, preparo.usuarioId);

        assertThat(confirmacao.materiasPersistidas()).isEqualTo(1);
        assertThat(confirmacao.topicosPersistidos()).isPositive();

        List<br.com.marcosbassetto.concursos.domain.materia.entity.MateriaEntity> materias =
                materiaRepository.findByConcursoIdAndUsuarioId(
                        preparo.concursoId, preparo.usuarioId);

        assertThat(materias).hasSize(1);
        assertThat(materias.get(0).getOrigem()).isEqualTo(OrigemMateria.EDITAL);
        assertThat(materias.get(0).getStatus()).isEqualTo(Status.ATIVO);

        EditalImportacaoEntity importacao =
                importacaoRepository.findByConcurso_Id(preparo.concursoId).orElseThrow();
        assertThat(importacao.getStatus()).isEqualTo(StatusProcessamento.CONFIRMADO);
        assertThat(importacao.getConfirmadoEm()).isNotNull();

        ConcursoEntity concurso = concursoRepository.findById(preparo.concursoId).orElseThrow();
        assertThat(concurso.getStatusProcessamento()).isEqualTo(StatusProcessamento.CONFIRMADO);
    }

    @Test
    @DisplayName("recusa confirmação duplicada da mesma importação")
    void naoDeveConfirmarDuasVezes() throws Exception {
        Preparo preparo = preparar(EditalFixture.PADRAO);

        confirmarEstrutura.confirmar(preparo.concursoId, preparo.usuarioId);

        assertThatThrownBy(() ->
                confirmarEstrutura.confirmar(preparo.concursoId, preparo.usuarioId))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                        .isEqualTo(ErrorCodes.CONFLITO_ESTADO));
    }

    @Test
    @DisplayName("recusa confirmação sem nenhuma matéria selecionada")
    void naoDeveConfirmarSemSelecao() throws Exception {
        Preparo preparo = preparar(EditalFixture.PADRAO);

        RevisaoEstruturaResponse revisao =
                consultarRevisao.consultar(preparo.concursoId, preparo.usuarioId);

        List<RevisaoEstruturaRequest.MateriaRevisaoRequest> pedidos = revisao.materias().stream()
                .map(m -> new RevisaoEstruturaRequest.MateriaRevisaoRequest(
                        m.id(), m.nome(), false, null, false, null))
                .toList();

        revisarEstrutura.aplicar(preparo.concursoId,
                new RevisaoEstruturaRequest(pedidos), preparo.usuarioId);

        assertThatThrownBy(() ->
                confirmarEstrutura.confirmar(preparo.concursoId, preparo.usuarioId))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                        .isEqualTo(ErrorCodes.DADOS_INVALIDOS));
    }

    @Test
    @DisplayName("recusa revisão de estrutura que não está aguardando revisão")
    void naoDeveRevisarEstruturaForaDeAguardandoRevisao() throws Exception {
        Preparo preparo = preparar(EditalFixture.PADRAO);

        EditalImportacaoEntity importacao =
                importacaoRepository.findByConcurso_Id(preparo.concursoId).orElseThrow();
        importacao.setStatus(StatusProcessamento.PROCESSANDO);
        importacaoRepository.save(importacao);

        assertThatThrownBy(() ->
                consultarRevisao.consultar(preparo.concursoId, preparo.usuarioId))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                        .isEqualTo(ErrorCodes.ESTRUTURA_NAO_REVISAVEL));
    }

    @Test
    @DisplayName("edição de nome e remoção de tópico ficam persistidas no staging")
    void deveEditarNomeERemoverTopico() throws Exception {
        Preparo preparo = preparar(EditalFixture.PADRAO);

        RevisaoEstruturaResponse revisao =
                consultarRevisao.consultar(preparo.concursoId, preparo.usuarioId);

        var materia = revisao.materias().get(0);
        var topicoRemovido = materia.topicos().get(0);

        List<RevisaoEstruturaRequest.TopicoRevisaoRequest> topicos = materia.topicos().stream()
                .map(t -> new RevisaoEstruturaRequest.TopicoRevisaoRequest(
                        t.id(), t.nome(), t.selecionado(), null,
                        t.id().equals(topicoRemovido.id())))
                .toList();

        revisarEstrutura.aplicar(preparo.concursoId, new RevisaoEstruturaRequest(List.of(
                new RevisaoEstruturaRequest.MateriaRevisaoRequest(
                        materia.id(), "Nome Editado", true, null, false, topicos)
        )), preparo.usuarioId);

        RevisaoEstruturaResponse depois =
                consultarRevisao.consultar(preparo.concursoId, preparo.usuarioId);

        assertThat(depois.materias()).singleElement()
                .satisfies(m -> {
                    assertThat(m.nome()).isEqualTo("Nome Editado");
                    assertThat(m.topicos()).extracting(
                            RevisaoEstruturaResponse.TopicoRevisaoResponse::id)
                            .doesNotContain(topicoRemovido.id());
                });
    }

    private Preparo preparar(String texto) throws Exception {
        UsuarioEntity usuario = usuarioRepository.save(UsuarioEntity.builder()
                .email("rev-" + UUID.randomUUID() + "@email.com")
                .nome("Usuário Revisão")
                .senhaHash("hash")
                .build());

        byte[] pdf = EditalFixture.pdfComTexto(texto);
        String textoExtraido =
                pdfTextExtractor.extractText(new java.io.ByteArrayInputStream(pdf));

        ConcursoEntity concurso = concursoRepository.save(ConcursoEntity.builder()
                .usuario(usuario)
                .nome("Concurso Revisão")
                .status(Status.ATIVO)
                .statusProcessamento(StatusProcessamento.RECEBIDO)
                .processado(false)
                .textoExtraido(textoExtraido)
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
        Preparo preparo = new Preparo(concurso.getId(), usuario.getId());
        criados.add(preparo);

        stagingService.processar(concurso.getId());

        return preparo;
    }

    private record Preparo(Long concursoId, Long usuarioId) {
    }
}
