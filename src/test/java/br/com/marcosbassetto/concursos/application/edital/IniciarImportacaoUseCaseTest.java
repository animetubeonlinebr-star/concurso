package br.com.marcosbassetto.concursos.application.edital;

import br.com.marcosbassetto.concursos.common.exception.ConflictException;
import br.com.marcosbassetto.concursos.common.exception.ErrorCodes;
import br.com.marcosbassetto.concursos.domain.concurso.entity.ConcursoEntity;
import br.com.marcosbassetto.concursos.domain.concurso.repository.ConcursoRepository;
import br.com.marcosbassetto.concursos.domain.edital.domain.StatusProcessamento;
import br.com.marcosbassetto.concursos.domain.edital.dto.IniciarImportacaoResponse;
import br.com.marcosbassetto.concursos.domain.edital.entity.EditalImportacaoEntity;
import br.com.marcosbassetto.concursos.domain.edital.repository.EditalImportacaoRepository;
import br.com.marcosbassetto.concursos.domain.usuario.entity.UsuarioEntity;
import br.com.marcosbassetto.concursos.domain.usuario.repository.UsuarioRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integração real com PostgreSQL: valida hash, deduplicação e o estado
 * inicial da importação. O listener de processamento é deliberadamente
 * ignorado aqui (o evento é AFTER_COMMIT e a transação de teste não commita).
 */
@SpringBootTest
@Transactional
class IniciarImportacaoUseCaseTest {

    @Autowired
    private IniciarImportacaoUseCase useCase;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ConcursoRepository concursoRepository;

    @Autowired
    private EditalImportacaoRepository importacaoRepository;

    private UsuarioEntity usuario;

    /**
     * Bytes do PDF gerados uma única vez: o hash é calculado sobre o
     * arquivo, e o PDFBox grava data de criação nos metadados, então duas
     * gerações do "mesmo" documento produzem bytes diferentes. Reaproveitar
     * os bytes é o que caracteriza o mesmo arquivo enviado duas vezes.
     */
    private byte[] bytesEdital;

    @BeforeEach
    void setUp() throws Exception {
        bytesEdital = gerarPdf();
        usuario = usuarioRepository.save(UsuarioEntity.builder()
                // Email único por execução: o banco é compartilhado entre os testes.
                .email("importacao-" + UUID.randomUUID() + "@email.com")
                .nome("Usuário Importação")
                .senhaHash("hash")
                .build());
    }

    @Test
    @DisplayName("importa o edital criando concurso e importação em RECEBIDO")
    void deveImportarEEPersistir() throws Exception {
        MockMultipartFile arquivo = pdfComConteudo();

        IniciarImportacaoResponse resposta = useCase.importar(usuario, arquivo);

        assertThat(resposta.jaExistia()).isFalse();
        assertThat(resposta.status()).isEqualTo(StatusProcessamento.RECEBIDO);
        assertThat(resposta.concursoId()).isNotNull();
        assertThat(resposta.importacaoId()).isNotNull();

        ConcursoEntity concurso = concursoRepository.findById(resposta.concursoId()).orElseThrow();
        assertThat(concurso.getStatusProcessamento()).isEqualTo(StatusProcessamento.RECEBIDO);
        assertThat(concurso.getProcessado()).isFalse();
        // O texto é persistido no upload: o PDF original não é armazenado.
        assertThat(concurso.getTextoExtraido()).contains("CONTEUDO PROGRAMATICO");
        assertThat(concurso.getImportacaoId()).isEqualTo(resposta.importacaoId());

        EditalImportacaoEntity importacao =
                importacaoRepository.findById(resposta.importacaoId()).orElseThrow();
        assertThat(importacao.getHashSha256()).hasSize(64);
        assertThat(importacao.getNomeArquivo()).isEqualTo("edital.pdf");
        assertThat(importacao.getTamanhoBytes()).isPositive();
        assertThat(importacao.getStatus()).isEqualTo(StatusProcessamento.RECEBIDO);
    }

    @Test
    @DisplayName("recusa o mesmo arquivo duas vezes para o mesmo usuário, informando o concursoId")
    void deveRecusarEditalDuplicado() throws Exception {
        IniciarImportacaoResponse primeira = useCase.importar(usuario, pdfComConteudo());

        MockMultipartFile mesmoArquivo = pdfComConteudo();

        assertThatThrownBy(() -> useCase.importar(usuario, mesmoArquivo))
                .isInstanceOf(ConflictException.class)
                .satisfies(ex -> {
                    ConflictException conflito = (ConflictException) ex;
                    assertThat(conflito.getCode()).isEqualTo(ErrorCodes.EDITAL_JA_IMPORTADO);
                    // O frontend usa este id para oferecer "abrir concurso existente".
                    assertThat(conflito.getDetails())
                            .containsEntry("concursoId", primeira.concursoId());
                });
    }

    @Test
    @DisplayName("o mesmo arquivo pode ser importado por outro usuário")
    void mesmoArquivoPorOutroUsuarioEhPermitido() throws Exception {
        useCase.importar(usuario, pdfComConteudo());

        UsuarioEntity outro = usuarioRepository.save(UsuarioEntity.builder()
                .email("importacao-outro-" + UUID.randomUUID() + "@email.com")
                .nome("Outro Usuário")
                .senhaHash("hash")
                .build());

        IniciarImportacaoResponse resposta = useCase.importar(outro, pdfComConteudo());

        assertThat(resposta.jaExistia()).isFalse();
        assertThat(resposta.concursoId()).isNotNull();
    }

    private MockMultipartFile pdfComConteudo() {
        return new MockMultipartFile(
                "arquivo", "edital.pdf", "application/pdf", bytesEdital);
    }

    private byte[] gerarPdf() throws Exception {
        try (PDDocument documento = new PDDocument()) {
            PDPage pagina = new PDPage();
            documento.addPage(pagina);

            try (PDPageContentStream conteudo = new PDPageContentStream(documento, pagina)) {
                conteudo.beginText();
                conteudo.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                conteudo.newLineAtOffset(50, 700);
                conteudo.showText("CONTEUDO PROGRAMATICO");
                conteudo.newLineAtOffset(0, -20);
                conteudo.showText("Lingua Portuguesa");
                conteudo.endText();
            }

            ByteArrayOutputStream saida = new ByteArrayOutputStream();
            documento.save(saida);
            return saida.toByteArray();
        }
    }
}
