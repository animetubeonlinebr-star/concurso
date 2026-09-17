package br.com.marcosbassetto.concursos.domain.concurso.service;

import br.com.marcosbassetto.concursos.common.enums.Status;
import br.com.marcosbassetto.concursos.domain.concurso.dto.ConcursoResponse;
import br.com.marcosbassetto.concursos.domain.concurso.dto.CriarConcursoRequest;
import br.com.marcosbassetto.concursos.domain.concurso.entity.ConcursoEntity;
import br.com.marcosbassetto.concursos.domain.concurso.repository.ConcursoRepository;
import br.com.marcosbassetto.concursos.domain.edital.domain.StatusProcessamento;
import br.com.marcosbassetto.concursos.domain.usuario.entity.UsuarioEntity;
import br.com.marcosbassetto.concursos.domain.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * O cadastro manual não passa por extração. Se ele nascer com o default
 * RECEBIDO, a tela de processamento fica presa esperando um edital que nunca
 * foi enviado — por isso o estado tem de ser CONFIRMADO desde a criação.
 */
@SpringBootTest
class ConcursoServiceIT {

    @Autowired
    private ConcursoService concursoService;

    @Autowired
    private ConcursoRepository concursoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    private final List<Long> concursos = new ArrayList<>();
    private final List<Long> usuarios = new ArrayList<>();

    @AfterEach
    void limpar() {
        concursos.forEach(id ->
                concursoRepository.findById(id).ifPresent(concursoRepository::delete));
        usuarios.forEach(id ->
                usuarioRepository.findById(id).ifPresent(usuarioRepository::delete));
        concursos.clear();
        usuarios.clear();
    }

    @Test
    @DisplayName("concurso criado manualmente nasce CONFIRMADO")
    void deveNascerConfirmado() {
        Long usuarioId = criarUsuario();

        Long concursoId = criarConcurso(usuarioId, "PF 2026", "Direito Penal");

        assertThat(entidade(concursoId).getStatusProcessamento())
                .isEqualTo(StatusProcessamento.CONFIRMADO);
    }

    @Test
    @DisplayName("processado permanece derivado de statusProcessamento")
    void deveManterCamposSincronizados() {
        Long usuarioId = criarUsuario();

        Long concursoId = criarConcurso(usuarioId, "TRF 2026", "Português");

        ConcursoEntity salvo = entidade(concursoId);
        assertThat(salvo.getProcessado()).isTrue();
        assertThat(salvo.getStatusProcessamento()).isEqualTo(StatusProcessamento.CONFIRMADO);
    }

    @Test
    @DisplayName("concurso manual não fica pendente de processamento")
    void naoDeveFicarPendente() {
        Long usuarioId = criarUsuario();

        ConcursoResponse resposta =
                concursoService.criar(usuarioId, request("TJ 2026", "Informática"));
        concursos.add(resposta.id());

        assertThat(entidade(resposta.id()).getStatusProcessamento())
                .isNotIn(StatusProcessamento.RECEBIDO, StatusProcessamento.PROCESSANDO,
                        StatusProcessamento.AGUARDANDO_REVISAO);
        assertThat(resposta.status()).isEqualTo(Status.ATIVO);
    }

    private Long criarConcurso(Long usuarioId, String nome, String materia) {
        ConcursoResponse resposta = concursoService.criar(usuarioId, request(nome, materia));
        concursos.add(resposta.id());
        return resposta.id();
    }

    private CriarConcursoRequest request(String nome, String materia) {
        return new CriarConcursoRequest(
                nome, "Órgão", "Cargo", null, 2026, null,
                List.of(new CriarConcursoRequest.MateriaRequest(materia, List.of("Tópico"))));
    }

    private ConcursoEntity entidade(Long id) {
        return concursoRepository.findById(id).orElseThrow();
    }

    private Long criarUsuario() {
        UsuarioEntity usuario = usuarioRepository.save(UsuarioEntity.builder()
                .email("it-" + UUID.randomUUID() + "@email.com")
                .nome("Usuário IT")
                .senhaHash("hash")
                .build());
        usuarios.add(usuario.getId());
        return usuario.getId();
    }
}