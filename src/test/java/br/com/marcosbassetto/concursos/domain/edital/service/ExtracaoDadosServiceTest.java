package br.com.marcosbassetto.concursos.domain.edital.service;

import br.com.marcosbassetto.concursos.domain.edital.dto.DadosExtraidos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExtracaoDadosServiceTest {

    private ExtracaoDadosService service;

    @BeforeEach
    void setUp() {

        ExtratorDadosConcurso extratorConcurso = new ExtratorDadosConcurso();
        ExtratorTopicos extratorTopicos = new ExtratorTopicos();
        ExtratorMaterias extratorMaterias = new ExtratorMaterias(extratorTopicos);

        service = new ExtracaoDadosService(extratorConcurso, extratorMaterias);
    }

    @Test
    void deveExtrairDadosDoTexto() {
        String texto = """
                CONCURSO PÚBLICO PARA PROVIMENTO DE VAGAS NO CARGO DE POLÍCIA FEDERAL
                BANCA: CEBRASPE
                ÓRGÃO: Polícia Federal
                ANO: 2026
                CONTEÚDO PROGRAMÁTICO
                LÍNGUA PORTUGUESA
                1. Interpretação de Texto
                2. Concordância
                3. Regência
                DIREITO CONSTITUCIONAL
                1. Princípios Fundamentais
                2. Direitos Fundamentais
                3. Organização do Estado
                INFORMÁTICA
                1. Conceitos Básicos
                2. Segurança da Informação
                """;

        DadosExtraidos dados = service.extrair(texto);

        assertThat(dados).isNotNull();
        assertThat(dados.getNome()).contains("POLÍCIA FEDERAL");
        assertThat(dados.getBanca()).isEqualTo("CEBRASPE");
        assertThat(dados.getOrgao()).contains("Polícia Federal");
        assertThat(dados.getAno()).isEqualTo(2026);
        assertThat(dados.getMaterias()).hasSize(3);
        dados.getMaterias().get(0).topicos();
    }

    @Test
    void deveRetornarDadosParcialmenteVaziosParaTextoInvalido() {
        String texto = "Texto sem informações de concurso";

        DadosExtraidos dados = service.extrair(texto);

        assertThat(dados).isNotNull();
        assertThat(dados.getNome()).isNull();
        assertThat(dados.getBanca()).isNull();
        assertThat(dados.getOrgao()).isNull();
        assertThat(dados.getAno()).isNull();
        assertThat(dados.getMaterias()).isEmpty();
    }

    @Test
    void deveExtrairBancaAVANCASPComPrioridade() {
        String texto = "BANCA EXAMINADORA: AVANÇASP";

        DadosExtraidos dados = service.extrair(texto);

        assertThat(dados.getBanca()).isEqualTo("AVANÇASP");
    }
}
