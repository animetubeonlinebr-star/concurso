package br.com.marcosbassetto.concursos.domain.edital.interpretacao.validacao;

import br.com.marcosbassetto.concursos.domain.edital.dto.StatusExtracao;
import br.com.marcosbassetto.concursos.domain.edital.interpretacao.perfil.EditalProfile;
import br.com.marcosbassetto.concursos.domain.edital.interpretacao.rascunho.EstruturaRascunho;
import br.com.marcosbassetto.concursos.domain.edital.interpretacao.rascunho.MateriaRascunho;
import br.com.marcosbassetto.concursos.domain.edital.interpretacao.rascunho.TopicoRascunho;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * O avaliador decide o que o usuário vê ao fim do processamento. Antes, cinco
 * matérias sem nenhum tópico eram anunciadas como sucesso, e o problema só
 * aparecia na revisão.
 */
class AvaliadorQualidadeExtracaoTest {

    private final AvaliadorQualidadeExtracao avaliador = new AvaliadorQualidadeExtracao();

    @Test
    @DisplayName("rascunho vazio é não identificado")
    void rascunhoVazio() {
        assertThat(avaliar(vazio(), true)).isEqualTo(StatusExtracao.NAO_IDENTIFICADO);
        assertThat(avaliador.avaliar(null, true)).isEqualTo(StatusExtracao.NAO_IDENTIFICADO);
    }

    @Test
    @DisplayName("matérias sem nenhum tópico são baixa confiança")
    void semTopicosEhBaixaConfianca() {
        // É o sintoma dos cabeçalhos institucionais promovidos a disciplina.
        assertThat(avaliar(materias(
                materia("UNIVERSIDADE ESTADUAL PAULISTA"),
                materia("REITORIA")), true)).isEqualTo(StatusExtracao.BAIXA_CONFIANCA);
    }

    @Test
    @DisplayName("uma única matéria completa ainda é baixa confiança")
    void umaMateriaEhBaixaConfianca() {
        assertThat(avaliar(materias(materiaComTopico("LÍNGUA PORTUGUESA")), true))
                .isEqualTo(StatusExtracao.BAIXA_CONFIANCA);
    }

    @Test
    @DisplayName("matéria sem tópico ao lado de Matérias completas é parcial")
    void misturaEhParcial() {
        assertThat(avaliar(materias(
                materiaComTopico("LÍNGUA PORTUGUESA"),
                materiaComTopico("MATEMÁTICA"),
                materia("CONHECIMENTOS ESPECÍFICOS")), true)).isEqualTo(StatusExtracao.PARCIAL);
    }

    @Test
    @DisplayName("extração completa com metadados é processada")
    void extracaoCompleta() {
        assertThat(avaliar(materias(
                materiaComTopico("LÍNGUA PORTUGUESA"),
                materiaComTopico("MATEMÁTICA")), true)).isEqualTo(StatusExtracao.PROCESSADO);
    }

    @Test
    @DisplayName("extração completa sem metadados é parcial")
    void semMetadadosEhParcial() {
        // Órgão e ano em branco obrigam o usuário a completar o cadastro.
        assertThat(avaliar(materias(
                materiaComTopico("LÍNGUA PORTUGUESA"),
                materiaComTopico("MATEMÁTICA")), false)).isEqualTo(StatusExtracao.PARCIAL);
    }

    private StatusExtracao avaliar(EstruturaRascunho rascunho, boolean dadosCompletos) {
        return avaliador.avaliar(rascunho, dadosCompletos);
    }

    private EstruturaRascunho vazio() {
        return EstruturaRascunho.vazia(EditalProfile.generico(), null);
    }

    private EstruturaRascunho materias(MateriaRascunho... materias) {
        return new EstruturaRascunho(EditalProfile.generico(), "GENERICO",
                List.of(materias), null);
    }

    private MateriaRascunho materia(String nome) {
        return new MateriaRascunho(nome, List.of());
    }

    private MateriaRascunho materiaComTopico(String nome) {
        return new MateriaRascunho(nome, List.of(
                new TopicoRascunho("Interpretação de textos.", null, null)));
    }
}