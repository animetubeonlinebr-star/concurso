package br.com.marcosbassetto.concursos.infrastructure.hash;

import br.com.marcosbassetto.concursos.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * O hash é a chave de deduplicação do upload: um desvio silencioso aqui
 * faria o mesmo edital ser reprocessado (ou arquivos distintos colidirem).
 */
class HashServiceTest {

    private final HashService hashService = new HashService();

    @Test
    @DisplayName("deve calcular o SHA-256 conhecido do vetor 'abc'")
    void deveCalcularSha256DeVetorConhecido() {
        String esperado =
                "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad";

        assertThat(hashService.calcularSha256("abc".getBytes(StandardCharsets.UTF_8)))
                .isEqualTo(esperado);
    }

    @Test
    @DisplayName("deve calcular o SHA-256 da entrada vazia")
    void deveCalcularSha256DeEntradaVazia() {
        String esperado =
                "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

        assertThat(hashService.calcularSha256(new byte[0])).isEqualTo(esperado);
    }

    @Test
    @DisplayName("deve produzir o mesmo hash para o mesmo conteúdo")
    void deveSerDeterministico() {
        byte[] conteudo = "%PDF-1.7 edital de teste".getBytes(StandardCharsets.UTF_8);

        assertThat(hashService.calcularSha256(conteudo))
                .isEqualTo(hashService.calcularSha256(conteudo));
    }

    @Test
    @DisplayName("deve produzir hashes distintos para conteúdos distintos")
    void deveDistinguirConteudosDiferentes() {
        byte[] a = "edital A".getBytes(StandardCharsets.UTF_8);
        byte[] b = "edital B".getBytes(StandardCharsets.UTF_8);

        assertThat(hashService.calcularSha256(a))
                .isNotEqualTo(hashService.calcularSha256(b));
    }

    @Test
    @DisplayName("deve retornar 64 caracteres hexadecimais minúsculos")
    void deveRetornarHexDe64Caracteres() {
        String hash = hashService.calcularSha256("conteudo".getBytes(StandardCharsets.UTF_8));

        assertThat(hash).hasSize(64).matches("[0-9a-f]{64}");
    }

    @Test
    @DisplayName("deve calcular o mesmo hash a partir de InputStream")
    void deveCalcularAPartirDeInputStream() throws IOException {
        byte[] conteudo = "%PDF-1.7 edital de teste".getBytes(StandardCharsets.UTF_8);

        try (InputStream is = new ByteArrayInputStream(conteudo)) {
            assertThat(hashService.calcularSha256(is))
                    .isEqualTo(hashService.calcularSha256(conteudo));
        }
    }

    @Test
    @DisplayName("deve consumir o InputStream por completo antes de calcular")
    void deveConsumirTodoOStream() {
        byte[] conteudo = new byte[8192];
        new Random(42).nextBytes(conteudo);

        String doStream = hashService.calcularSha256(new ByteArrayInputStream(conteudo));

        assertThat(doStream).isEqualTo(hashService.calcularSha256(conteudo));
    }

    @Test
    @DisplayName("deve casar com o MessageDigest da própria JVM")
    void deveCasarComMessageDigest() throws Exception {
        byte[] conteudo = "verificação independente".getBytes(StandardCharsets.UTF_8);
        String esperado = HexFormat.of()
                .formatHex(MessageDigest.getInstance("SHA-256").digest(conteudo));

        assertThat(hashService.calcularSha256(conteudo)).isEqualTo(esperado);
    }

    @Test
    @DisplayName("deve falhar com ARQUIVO_INVALIDO quando o stream não pode ser lido")
    void deveFalharQuandoStreamNaoPodeSerLido() {
        InputStream quebrado = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("disco indisponível");
            }
        };

        assertThatThrownBy(() -> hashService.calcularSha256(quebrado))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("hash");
    }
}