package br.com.marcosbassetto.concursos.domain.edital.interpretacao.estrategia;

import br.com.marcosbassetto.concursos.domain.edital.interpretacao.documento.DocumentModel;
import br.com.marcosbassetto.concursos.domain.edital.interpretacao.perfil.EditalProfile;
import br.com.marcosbassetto.concursos.domain.edital.interpretacao.rascunho.EstruturaRascunho;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * A factory é o ponto único que decide "como interpretar". Ela precisa falhar
 * alto na subida em caso de perfil duplicado, porque a escolha por ordem de
 * injeção seria imprevisível em produção.
 */
class EstrategiaFactoryTest {

    private static final EstrategiaInterpretacaoEdital GENERICA = estrategia("GENERICO");
    private static final EstrategiaInterpretacaoEdital SEM_CONTEUDO = estrategia("SEM_CONTEUDO");

    @Test
    @DisplayName("escolhe a estratégia do perfil informado")
    void escolhePeloPerfil() {
        EstrategiaFactory factory = new EstrategiaFactory(List.of(GENERICA, SEM_CONTEUDO));

        assertThat(factory.criar("SEM_CONTEUDO")).isSameAs(SEM_CONTEUDO);
        assertThat(factory.criar("GENERICO")).isSameAs(GENERICA);
    }

    @Test
    @DisplayName("perfil desconhecido cai na estratégia padrão")
    void perfilDesconhecidoUsaPadrao() {
        EstrategiaFactory factory = new EstrategiaFactory(List.of(GENERICA, SEM_CONTEUDO));

        assertThat(factory.criar("PERFIL_QUE_NAO_EXISTE")).isSameAs(GENERICA);
    }

    @Test
    @DisplayName("dois registros para o mesmo perfil falham na subida")
    void perfisDuplicadosFalham() {
        assertThatThrownBy(() -> new EstrategiaFactory(
                List.of(GENERICA, estrategia("GENERICO"))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("GENERICO");
    }

    @Test
    @DisplayName("sem estratégia padrão a factory não sobe")
    void semPadraoNaoSobe() {
        assertThatThrownBy(() -> new EstrategiaFactory(List.of(SEM_CONTEUDO)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("GENERICO");
    }

    private static EstrategiaInterpretacaoEdital estrategia(String perfil) {
        return new EstrategiaInterpretacaoEdital() {
            @Override
            public String perfilSuportado() {
                return perfil;
            }

            @Override
            public EstruturaRascunho interpretar(DocumentModel documento, EditalProfile p) {
                return EstruturaRascunho.vazia(p, null);
            }
        };
    }
}