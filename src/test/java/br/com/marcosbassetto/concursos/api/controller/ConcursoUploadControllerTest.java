package br.com.marcosbassetto.concursos.api.controller;

import br.com.marcosbassetto.concursos.application.edital.IniciarImportacaoUseCase;
import br.com.marcosbassetto.concursos.common.exception.BusinessException;
import br.com.marcosbassetto.concursos.common.security.UsuarioAutenticadoResolver;
import br.com.marcosbassetto.concursos.domain.edital.domain.StatusProcessamento;
import br.com.marcosbassetto.concursos.domain.edital.dto.EstruturaEditalDTO;
import br.com.marcosbassetto.concursos.domain.edital.dto.IniciarImportacaoResponse;
import br.com.marcosbassetto.concursos.domain.edital.dto.StatusExtracao;
import br.com.marcosbassetto.concursos.domain.edital.interpretacao.InterpretadorEditalFacade;
import br.com.marcosbassetto.concursos.domain.usuario.entity.UsuarioEntity;
import br.com.marcosbassetto.concursos.infrastructure.pdf.PdfTextExtractorService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * A pré-visualização e a importação precisam compartilhar a mesma
 * interpretação. Quando a pré-visualização usava o extrator legado, o usuário
 * via uma estrutura na tela inicial e outra no fluxo persistido — o mesmo
 * arquivo, duas leituras diferentes.
 */
@ExtendWith(MockitoExtension.class)
class ConcursoUploadControllerTest {

    @Mock
    private PdfTextExtractorService pdfExtractor;

    @Mock
    private InterpretadorEditalFacade interpretador;

    @Mock
    private IniciarImportacaoUseCase iniciarImportacao;

    @Mock
    private UsuarioAutenticadoResolver usuarioResolver;

    @Mock
    private Authentication auth;

    @InjectMocks
    private ConcursoUploadController controller;

    @Test
    @DisplayName("pré-visualização usa a mesma interpretação do fluxo persistido")
    void previewUsaInterpretacaoPersistida() throws Exception {
        MockMultipartFile arquivo = pdf("edital.pdf");
        when(pdfExtractor.extractText(any(MultipartFile.class))).thenReturn("CONTEÚDO PROGRAMÁTICO\nLÍNGUA PORTUGUESA");
        when(interpretador.interpretar(any())).thenReturn(vazia());

        var resposta = controller.uploadEdital(arquivo);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(interpretador).interpretar(any());
    }

    @Test
    @DisplayName("importação persiste e responde 202 com o concursoId")
    void importacaoRespondeAceito() {
        MockMultipartFile arquivo = pdf("edital.pdf");
        when(usuarioResolver.resolverUsuarioId(auth)).thenReturn(7L);
        when(iniciarImportacao.buscarUsuario(7L)).thenReturn(new UsuarioEntity());
        when(iniciarImportacao.importar(any(), any())).thenReturn(
                new IniciarImportacaoResponse(42L, 9L, StatusProcessamento.PROCESSANDO, false));

        var resposta = controller.importarEdital(arquivo, auth);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(resposta.getBody().concursoId()).isEqualTo(42L);
    }

    @Test
    @DisplayName("arquivo vazio é recusado sem extrair nada")
    void arquivoVazioRecusado() {
        MockMultipartFile vazio = new MockMultipartFile(
                "arquivo", "vazio.pdf", "application/pdf", new byte[0]);

        assertThatThrownBy(() -> controller.uploadEdital(vazio))
                .isInstanceOf(BusinessException.class);

        verifyNoInteractions(pdfExtractor, interpretador);
    }

    @Test
    @DisplayName("arquivo que não é PDF é recusado")
    void arquivoNaoPdfRecusado() {
        MockMultipartFile texto = new MockMultipartFile(
                "arquivo", "edital.txt", "text/plain", "conteudo".getBytes());

        assertThatThrownBy(() -> controller.uploadEdital(texto))
                .isInstanceOf(BusinessException.class);

        verifyNoInteractions(pdfExtractor);
    }

    private MockMultipartFile pdf(String nome) {
        return new MockMultipartFile("arquivo", nome, "application/pdf", "%PDF-1.7".getBytes());
    }

    private EstruturaEditalDTO vazia() {
        return new EstruturaEditalDTO(null, java.util.List.of(), java.util.List.of(),
                StatusExtracao.NAO_IDENTIFICADO, "sem conteúdo");
    }
}