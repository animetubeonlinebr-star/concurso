package br.com.marcosbassetto.concursos.domain.edital.extractor.impl;

import br.com.marcosbassetto.concursos.domain.edital.domain.PadraoEdital;
import br.com.marcosbassetto.concursos.domain.edital.dto.DadosConcurso;
import br.com.marcosbassetto.concursos.domain.edital.dto.EstruturaEditalDTO;
import br.com.marcosbassetto.concursos.domain.edital.dto.MateriaExtraida;
import br.com.marcosbassetto.concursos.domain.edital.dto.StatusExtracao;
import br.com.marcosbassetto.concursos.domain.edital.extractor.TextoPreProcessador;
import br.com.marcosbassetto.concursos.domain.edital.segmenter.SegmentadorEdital;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ExtratorPadraoTest {

    private ExtratorPadrao extrator;

    @BeforeEach
    void setUp() {
        extrator = new ExtratorPadrao(new TextoPreProcessador(), new SegmentadorEdital());
    }

    @Test
    @DisplayName("declara o padrão ENFASE_BLOCOS")
    void deveDeclararPadrao() {
        assertThat(extrator.padraoSuportado()).isEqualTo(PadraoEdital.ENFASE_BLOCOS);
    }

    @Test
    @DisplayName("extrai matérias com cabeçalho Markdown em bloco de conteúdo programático")
    void deveExtrairMateriasDeBloco() {
        EstruturaEditalDTO estrutura = extrator.extrair(textoPadrao());

        List<MateriaExtraida> materias = estrutura.materiasDoPrimeiroCurso();

        assertThat(materias).extracting(MateriaExtraida::nome)
                .contains("Língua Portuguesa", "Direito Constitucional");
    }

    @Test
    @DisplayName("agrupa os tópicos sob a matéria correspondente")
    void deveExtrairTopicosPorMateria() {
        EstruturaEditalDTO estrutura = extrator.extrair(textoPadrao());

        MateriaExtraida portugues = estrutura.materiasDoPrimeiroCurso().stream()
                .filter(m -> m.nome().equals("Língua Portuguesa"))
                .findFirst()
                .orElseThrow();

        assertThat(portugues.topicos()).extracting(t -> t.nome())
                .anyMatch(n -> n.contains("Concordância"))
                .noneMatch(n -> n.contains("Controle de constitucionalidade"));
    }

    @Test
    @DisplayName("extrai dados do concurso do cabeçalho")
    void deveExtrairDadosDoConcurso() {
        EstruturaEditalDTO estrutura = extrator.extrair(textoPadrao());

        DadosConcurso dados = estrutura.dadosConcurso();
        assertThat(dados).isNotNull();
        assertThat(dados.orgao()).contains("Tribunal de Justiça");
        assertThat(dados.banca()).isEqualTo("VUNESP");
        assertThat(dados.ano()).isEqualTo(2026);
    }

    @Test
    @DisplayName("classifica como NAO_IDENTIFICADO quando não há matérias")
    void semMateriasRetornaNaoIdentificado() {
        EstruturaEditalDTO estrutura = extrator.extrair("Apenas um texto qualquer sem estrutura.");

        assertThat(estrutura.status()).isEqualTo(StatusExtracao.NAO_IDENTIFICADO);
        assertThat(estrutura.materiasDoPrimeiroCurso()).isEmpty();
    }

    @Test
    @DisplayName("não quebra com texto vazio ou nulo")
    void naoQuebraComTextoVazio() {
        assertThat(extrator.extrair("")).isNotNull();
        assertThat(extrator.extrair(null)).isNotNull();
    }

    @Test
    @DisplayName("encontra disciplinas listadas em maiúsculas sem Markdown")
    void deveExtrairCabecalhoEmMaiusculas() {
        String texto = """
                CONCURSO PÚBLICO - PREFEITURA DE EXEMPLO - 2025
                BANCA: INSTITUTO EXEMPLO

                CONTEÚDO PROGRAMÁTICO

                LÍNGUA PORTUGUESA
                ▪ Compreensão de texto.
                ▪ Ortografia oficial.

                MATEMÁTICA
                ▪ Razão e proporção.
                ▪ Porcentagem.
                """;

        EstruturaEditalDTO estrutura = extrator.extrair(texto);
        List<MateriaExtraida> materias = estrutura.materiasDoPrimeiroCurso();

        assertThat(materias).extracting(MateriaExtraida::nome)
                .contains("LÍNGUA PORTUGUESA", "MATEMÁTICA");
    }

    private String textoPadrao() {
        return """
                CONCURSO PÚBLICO PARA TÉCNICO JUDICIÁRIO - 2026

                ÓRGÃO: Tribunal de Justiça de São Paulo
                BANCA: VUNESP
                CARGO: Técnico Judiciário

                ANEXO I - CONTEÚDO PROGRAMÁTICO

                ## Língua Portuguesa
                ▪ Interpretação de texto.
                ▪ Concordância verbal e nominal.

                ## Direito Constitucional
                ▪ Princípios fundamentais.
                ▪ Controle de constitucionalidade.

                ANEXO II - CRONOGRAMA
                """;
    }
}
