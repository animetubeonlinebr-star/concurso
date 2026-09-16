package br.com.marcosbassetto.concursos.domain.edital.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DetectorDuplicidadeTest {

    private final DetectorDuplicidade detector = new DetectorDuplicidade(0.7);

    @Test
    @DisplayName("detecta duplicidade exata ignorando acentos e caixa")
    void detectaDuplicidadeExata() {
        List<DetectorDuplicidade.OcorrenciaDuplicada> ocorrencias =
                detector.detectar(List.of("Língua Portuguesa", "Lingua portuguesa"));

        assertThat(ocorrencias).hasSize(1);
        assertThat(ocorrencias.get(0).tipo())
                .isEqualTo(DetectorDuplicidade.TipoDuplicidade.EXATA);
        assertThat(ocorrencias.get(0).indiceReferencia()).isZero();
        assertThat(ocorrencias.get(0).indiceDuplicado()).isEqualTo(1);
    }

    @Test
    @DisplayName("detecta duplicidade aproximada por tokens")
    void detectaDuplicidadeAproximada() {
        List<DetectorDuplicidade.OcorrenciaDuplicada> ocorrencias =
                detector.detectar(List.of("Direito Constitucional", "Direito Constitucional"));

        assertThat(ocorrencias).hasSize(1);
        assertThat(ocorrencias.get(0).similaridade()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("não sinaliza nomes claramente distintos")
    void naoSinalizaNomesDistintos() {
        List<DetectorDuplicidade.OcorrenciaDuplicada> ocorrencias =
                detector.detectar(List.of("Matemática", "História do Brasil", "Biologia"));

        assertThat(ocorrencias).isEmpty();
    }

    @Test
    @DisplayName("Jaccard sinaliza sobreposição parcial acima do limiar")
    void jaccardDetectaSobreposicaoParcial() {
        // Intersecao = {DIREITO, CONSTITUCIONAL} (2 tokens); uniao =
        // {NOCOES, DE, DIREITO, CONSTITUCIONAL} (4 tokens) => 0.5. Fica
        // abaixo do limiar padrao de 0.7, por isso nao e sinalizado.
        double similaridade = detector.similaridade(
                "NOCOES DE DIREITO CONSTITUCIONAL", "DIREITO CONSTITUCIONAL");

        assertThat(similaridade).isCloseTo(0.5, org.assertj.core.data.Offset.offset(0.001));
    }

    @Test
    @DisplayName("Jaccard é 1.0 para conjuntos de tokens idênticos")
    void jaccardIdentico() {
        assertThat(detector.similaridade("DIREITO ADMINISTRATIVO", "DIREITO ADMINISTRATIVO"))
                .isEqualTo(1.0);
    }

    @Test
    @DisplayName("lista vazia ou nula não gera ocorrências")
    void listaVaziaNaoGeraOcorrencias() {
        assertThat(detector.detectar(List.of())).isEmpty();
        assertThat(detector.detectar(null)).isEmpty();
        assertThat(detector.detectar(List.of("Só uma"))).isEmpty();
    }

    @Test
    @DisplayName("limiar configurável controla a sinalização aproximada")
    void limiarConfiguravel() {
        // Mesmo par do teste anterior: com limiar mais brando, e sinalizado.
        DetectorDuplicidade brando = new DetectorDuplicidade(0.5);

        List<DetectorDuplicidade.OcorrenciaDuplicada> ocorrencias = brando.detectar(
                List.of("Noções de Direito Constitucional", "Direito Constitucional"));

        assertThat(ocorrencias).hasSize(1);
        assertThat(ocorrencias.get(0).tipo())
                .isEqualTo(DetectorDuplicidade.TipoDuplicidade.APROXIMADA);
    }
}
