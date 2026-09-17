package br.com.marcosbassetto.concursos.application.edital;

import br.com.marcosbassetto.concursos.domain.concurso.repository.ConcursoRepository;
import br.com.marcosbassetto.concursos.domain.edital.domain.StatusProcessamento;
import br.com.marcosbassetto.concursos.domain.edital.dto.StatusExtracao;
import br.com.marcosbassetto.concursos.domain.edital.repository.EditalImportacaoRepository;
import br.com.marcosbassetto.concursos.domain.edital.repository.MateriaSugeridaRepository;
import br.com.marcosbassetto.concursos.domain.usuario.entity.UsuarioEntity;
import br.com.marcosbassetto.concursos.domain.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercita o caminho que os testes transacionais não alcançam: o upload
 * commita, o {@code ImportacaoIniciadaEvent} dispara o processamento
 * assíncrono e o concurso evolui sozinho até AGUARDANDO_REVISAO.
 *
 * Deliberadamente NÃO é {@code @Transactional}: dentro de uma transação de
 * teste o evento AFTER_COMMIT nunca dispara, então um listener que roda cedo
 * demais (antes do commit) ou um mapeamento de coluna quebrado passariam
 * despercebidos — foi exatamente o caso do {@code @Lob} em texto extraído e
 * do {@code @EventListener} síncrono.
 */
@SpringBootTest
class ProcessamentoAssincronoIT {

    private static final Duration LIMITE = Duration.ofSeconds(30);

    @Autowired
    private IniciarImportacaoUseCase iniciarImportacao;

    @Autowired
    private ConcursoRepository concursoRepository;

    @Autowired
    private EditalImportacaoRepository importacaoRepository;

    @Autowired
    private MateriaSugeridaRepository materiaSugeridaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    private final List<Long> concursos = new ArrayList<>();
    private final List<Long> usuarios = new ArrayList<>();

    @AfterEach
    void limpar() {
        for (Long concursoId : concursos) {
            // O delete da importação cascateia para o staging sugerido.
            importacaoRepository.findByConcurso_Id(concursoId)
                    .ifPresent(importacaoRepository::delete);
            concursoRepository.findById(concursoId).ifPresent(concursoRepository::delete);
        }
        usuarios.forEach(id ->
                usuarioRepository.findById(id).ifPresent(usuarioRepository::delete));
        concursos.clear();
        usuarios.clear();
    }

    @Test
    @DisplayName("edital sem conteúdo programático não termina como sucesso mudo")
    void deveSinalizarExtracaoNaoIdentificada() throws Exception {
        UsuarioEntity usuario = criarUsuario();

        var resposta = iniciarImportacao.importar(
                usuario, EditalFixture.arquivo(EditalFixture.SEM_CONTEUDO_PROGRAMATICO));
        concursos.add(resposta.concursoId());

        aguardar(() -> importacaoRepository.findByConcurso_Id(resposta.concursoId())
                .map(i -> StatusProcessamento.AGUARDANDO_REVISAO.equals(i.getStatus()))
                .orElse(false));

        var importacao = importacaoRepository
                .findByConcurso_Id(resposta.concursoId()).orElseThrow();

        assertThat(importacao.getStatusExtracao())
                .as("sem bloco de conteúdo programático a extração não é PROCESSADO")
                .isEqualTo(StatusExtracao.NAO_IDENTIFICADO);
        assertThat(materiaSugeridaRepository
                .findByImportacao_IdOrderByOrdemAsc(importacao.getId()))
                .isEmpty();
    }

    @Test
    @DisplayName("o upload commita, dispara o processamento e chega em AGUARDANDO_REVISAO")
    void deveProcessarAposOCommit() throws Exception {
        UsuarioEntity usuario = criarUsuario();

        var resposta = iniciarImportacao.importar(
                usuario, EditalFixture.arquivo(EditalFixture.PADRAO));
        concursos.add(resposta.concursoId());

        assertThat(resposta.status()).isEqualTo(StatusProcessamento.RECEBIDO);

        aguardar(() -> concursoRepository.findById(resposta.concursoId())
                .map(c -> StatusProcessamento.AGUARDANDO_REVISAO
                        .equals(c.getStatusProcessamento()))
                .orElse(false));

        var concurso = concursoRepository.findById(resposta.concursoId()).orElseThrow();
        assertThat(concurso.getStatusProcessamento())
                .isEqualTo(StatusProcessamento.AGUARDANDO_REVISAO);
        assertThat(concurso.getTextoExtraido())
                .as("texto do edital precisa ser legível fora da transação de upload")
                .isNotBlank()
                .contains("CONTEUDO PROGRAMATICO");

        var importacao = importacaoRepository
                .findByConcurso_Id(resposta.concursoId()).orElseThrow();
        assertThat(materiaSugeridaRepository
                .findByImportacao_IdOrderByOrdemAsc(importacao.getId()))
                .as("a estrutura extraída precisa existir no staging de revisão")
                .isNotEmpty();
        assertThat(importacao.getStatusExtracao()).isEqualTo(StatusExtracao.PROCESSADO);
    }

    private void aguardar(BooleanSupplier condicao) throws InterruptedException {
        Instant limite = Instant.now().plus(LIMITE);
        while (Instant.now().isBefore(limite)) {
            if (condicao.getAsBoolean()) {
                return;
            }
            Thread.sleep(200);
        }
        throw new AssertionError(
                "O processamento não concluiu em " + LIMITE.toSeconds() + "s.");
    }

    private UsuarioEntity criarUsuario() {
        UsuarioEntity usuario = usuarioRepository.save(UsuarioEntity.builder()
                .email("async-" + UUID.randomUUID() + "@email.com")
                .nome("Usuário Async")
                .senhaHash("hash")
                .build());
        usuarios.add(usuario.getId());
        return usuario;
    }
}