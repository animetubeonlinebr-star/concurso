package br.com.marcosbassetto.concursos.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class NomeNormalizerTest {

    @ParameterizedTest
    @CsvSource({
            "'Língua Portuguesa',           'LINGUA PORTUGUESA'",
            "'LÍNGUA PORTUGUESA',           'LINGUA PORTUGUESA'",
            "'lingua  portuguesa',          'LINGUA PORTUGUESA'",
            "'  Língua   Portuguesa  ',     'LINGUA PORTUGUESA'",
            "'Lingua-Portuguesa',           'LINGUA PORTUGUESA'",
            "'Direito  Constitucional',     'DIREITO CONSTITUCIONAL'",
            "'Raciocínio Lógico-Matemático','RACIOCINIO LOGICO MATEMATICO'",
            "'Cargo 1: Analista',           'CARGO 1 ANALISTA'",
            "'CONTEÚDO PROGRAMÁTICO',       'CONTEUDO PROGRAMATICO'"
    })
    @DisplayName("deve normalizar variações de grafia para a mesma chave")
    void deveNormalizar(String entrada, String esperado) {
        assertThat(NomeNormalizer.normalizar(entrada)).isEqualTo(esperado);
    }

    @Test
    @DisplayName("deve retornar string vazia para null")
    void deveTratarNull() {
        assertThat(NomeNormalizer.normalizar(null)).isEmpty();
        assertThat(NomeNormalizer.normalizar("")).isEmpty();
        assertThat(NomeNormalizer.normalizar("   ")).isEmpty();
    }
}
