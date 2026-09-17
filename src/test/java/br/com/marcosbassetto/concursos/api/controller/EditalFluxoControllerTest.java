package br.com.marcosbassetto.concursos.api.controller;

import br.com.marcosbassetto.concursos.application.edital.ConfirmarEstruturaUseCase;
import br.com.marcosbassetto.concursos.application.edital.ConsultarConteudoUseCase;
import br.com.marcosbassetto.concursos.application.edital.ConsultarProcessamentoUseCase;
import br.com.marcosbassetto.concursos.application.edital.ConsultarRevisaoUseCase;
import br.com.marcosbassetto.concursos.application.edital.RevisarEstruturaUseCase;
import br.com.marcosbassetto.concursos.common.security.UsuarioAutenticadoResolver;
import br.com.marcosbassetto.concursos.common.exception.ResourceNotFoundException;
import br.com.marcosbassetto.concursos.domain.edital.domain.StatusProcessamento;
import br.com.marcosbassetto.concursos.domain.edital.dto.ConfirmacaoEstruturaResponse;
import br.com.marcosbassetto.concursos.domain.edital.dto.ConteudoConcursoResponse;
import br.com.marcosbassetto.concursos.domain.edital.dto.RevisaoEstruturaRequest;
import br.com.marcosbassetto.concursos.domain.edital.dto.RevisaoEstruturaResponse;
import br.com.marcosbassetto.concursos.domain.edital.dto.StatusProcessamentoResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * O usuarioId vem sempre do {@link UsuarioAutenticadoResolver}: estes testes
 * fixam que nenhum controller aceita identidade da requisição.
 */
@ExtendWith(MockitoExtension.class)
class EditalFluxoControllerTest {

    @Mock
    private ConsultarProcessamentoUseCase consultarProcessamento;

    @Mock
    private ConsultarRevisaoUseCase consultarRevisao;

    @Mock
    private ConsultarConteudoUseCase consultarConteudo;

    @Mock
    private RevisarEstruturaUseCase revisarEstrutura;

    @Mock
    private ConfirmarEstruturaUseCase confirmarEstrutura;

    @Mock
    private UsuarioAutenticadoResolver usuarioResolver;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private EditalImportacaoController importacaoController;

    @InjectMocks
    private EditalRevisaoController revisaoController;

    @BeforeEach
    void setUp() {
        lenient().when(usuarioResolver.resolverUsuarioId(authentication)).thenReturn(3L);
    }

    @Test
    @DisplayName("GET /processamento devolve o status resolvendo o usuário do token")
    void deveConsultarProcessamento() {
        StatusProcessamentoResponse esperado =
                StatusProcessamentoResponse.de(1L, StatusProcessamento.PROCESSANDO, null);
        when(consultarProcessamento.consultar(1L, 3L)).thenReturn(esperado);

        ResponseEntity<StatusProcessamentoResponse> resposta =
                importacaoController.status(1L, authentication);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody()).isEqualTo(esperado);
    }

    @Test
    @DisplayName("POST /reprocessar valida a posse antes de disparar")
    void deveValidarAntesDeReprocessar() {
        ResponseEntity<StatusProcessamentoResponse> resposta =
                importacaoController.reprocessar(1L, authentication);

        verify(consultarProcessamento).validarReprocessamento(1L, 3L);
        verify(consultarProcessamento).dispararReprocessamento(1L);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(resposta.getBody().status()).isEqualTo(StatusProcessamento.PROCESSANDO);
    }

    @Test
    @DisplayName("GET /revisao resolve o usuário autenticado")
    void deveConsultarRevisao() {
        RevisaoEstruturaResponse esperado = new RevisaoEstruturaResponse(
                1L, 42L, StatusProcessamento.AGUARDANDO_REVISAO, null, List.of(), null);
        when(consultarRevisao.consultar(1L, 3L)).thenReturn(esperado);

        assertThat(revisaoController.revisao(1L, authentication).getBody())
                .isEqualTo(esperado);
    }

    @Test
    @DisplayName("PUT /revisao aplica as edições e devolve o estado persistido")
    void deveAtualizarRevisao() {
        RevisaoEstruturaRequest request = new RevisaoEstruturaRequest(List.of());
        RevisaoEstruturaResponse esperado = new RevisaoEstruturaResponse(
                1L, 42L, StatusProcessamento.AGUARDANDO_REVISAO, null, List.of(), null);
        when(consultarRevisao.consultar(1L, 3L)).thenReturn(esperado);

        ResponseEntity<RevisaoEstruturaResponse> resposta =
                revisaoController.atualizarRevisao(1L, request, authentication);

        verify(revisarEstrutura).aplicar(eq(1L), any(), eq(3L));
        assertThat(resposta.getBody()).isEqualTo(esperado);
    }

    @Test
    @DisplayName("POST /confirmar promove a estrutura do dono do concurso")
    void deveConfirmarEstrutura() {
        ConfirmacaoEstruturaResponse esperado =
                new ConfirmacaoEstruturaResponse(1L, 4, 12, List.of());
        when(confirmarEstrutura.confirmar(1L, 3L)).thenReturn(esperado);

        assertThat(revisaoController.confirmar(1L, authentication).getBody())
                .isEqualTo(esperado);
    }

    @Test
    @DisplayName("GET /conteudo devolve a árvore confirmada")
    void deveConsultarConteudo() {
        ConteudoConcursoResponse esperado = new ConteudoConcursoResponse(
                1L,
                "PF 2026",
                StatusProcessamento.CONFIRMADO,
                new ConteudoConcursoResponse.ResumoConteudo(1, 2, 0),
                List.of());
        when(consultarConteudo.consultar(1L, 3L)).thenReturn(esperado);

        assertThat(revisaoController.conteudo(1L, authentication).getBody())
                .isEqualTo(esperado);
    }

    @Test
    @DisplayName("nenhum endpoint é acessível sem autenticação resolvível")
    void deveFalharSemAutenticacao() {
        when(usuarioResolver.resolverUsuarioId(null))
                .thenThrow(new ResourceNotFoundException("Usuário", "autenticação", "ausente"));

        assertThatThrownBy(() -> importacaoController.status(1L, null))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(consultarProcessamento);
    }
}