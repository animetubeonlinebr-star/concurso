package br.com.marcosbassetto.concursos.domain.edital.service;

import br.com.marcosbassetto.concursos.domain.edital.dto.DadosExtraidos;
import br.com.marcosbassetto.concursos.domain.edital.dto.MateriaExtraida;
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
        // Bloco de conteúdo em tamanho realista: ExtratorMaterias ignora
        // blocos com menos de 1000 caracteres, para não confundir um sumário
        // curto com o conteúdo programático de verdade.
        String texto = """
                CONCURSO PÚBLICO PARA PROVIMENTO DE VAGAS NO CARGO DE POLÍCIA FEDERAL
                BANCA: CEBRASPE
                ÓRGÃO: Polícia Federal
                ANO: 2026
                CONTEÚDO PROGRAMÁTICO
                LÍNGUA PORTUGUESA
                1. Compreensão e interpretação de textos de gêneros variados
                2. Reconhecimento de tipos e gêneros textuais
                3. Domínio da ortografia oficial
                4. Domínio dos mecanismos de coesão textual
                5. Emprego de elementos de referenciação, substituição e repetição
                6. Emprego de tempos e modos verbais
                7. Emprego das classes de palavras
                8. Domínio da estrutura morfossintática do período
                9. Relações de coordenação entre orações e entre termos da oração
                10. Relações de subordinação entre orações e entre termos da oração
                11. Emprego dos sinais de pontuação
                12. Concordância verbal e nominal
                13. Regência verbal e nominal
                14. Emprego do sinal indicativo de crase
                15. Colocação dos pronomes átonos
                16. Reescrita de frases e parágrafos do texto
                DIREITO CONSTITUCIONAL
                1. Constituição: conceito, classificações e princípios fundamentais
                2. Aplicabilidade das normas constitucionais
                3. Direitos e garantias fundamentais
                4. Direitos e deveres individuais e coletivos
                5. Direitos sociais e direitos políticos
                6. Organização do Estado e repartição de competências
                7. Administração Pública na Constituição
                8. Poder Executivo, Legislativo e Judiciário
                9. Controle de constitucionalidade
                10. Defesa do Estado e das instituições democráticas
                11. Sistema Tributário Nacional
                12. Ordem econômica e financeira
                13. Ordem social e seguridade social
                DIREITO ADMINISTRATIVO
                1. Princípios da Administração Pública
                2. Atos administrativos: conceito, requisitos e atributos
                3. Poderes administrativos
                4. Agentes públicos e regime disciplinar
                5. Licitações e contratos administrativos
                6. Improbidade administrativa
                7. Processo administrativo disciplinar
                8. Responsabilidade civil do Estado
                9. Controle da Administração Pública
                10. Organização administrativa: direta e indireta
                INFORMÁTICA
                1. Conceitos básicos de hardware e software
                2. Sistemas operacionais Windows e Linux
                3. Editores de texto e planilhas eletrônicas
                4. Redes de computadores e internet
                5. Segurança da informação e proteção de dados
                6. Computação em nuvem
                7. Banco de dados: conceitos e linguagem SQL
                8. Noções de programação e algoritmos
                RACIOCÍNIO LÓGICO
                1. Estruturas lógicas e lógica de argumentação
                2. Diagramas lógicos e conjuntos
                3. Análise combinatória e probabilidade
                4. Sequências lógicas e raciocínio sequencial
                5. Problemas aritméticos e geométricos
                6. Porcentagem, juros simples e compostos
                """;

        DadosExtraidos dados = service.extrair(texto);

        assertThat(dados).isNotNull();
        assertThat(dados.getNome()).contains("POLÍCIA FEDERAL");
        assertThat(dados.getBanca()).isEqualTo("CEBRASPE");
        assertThat(dados.getOrgao()).contains("Polícia Federal");
        assertThat(dados.getAno()).isEqualTo(2026);
        assertThat(dados.getMaterias()).hasSize(5);
        assertThat(dados.getMaterias()).extracting(MateriaExtraida::nome)
                .contains("LÍNGUA PORTUGUESA", "DIREITO CONSTITUCIONAL", "INFORMÁTICA");
        assertThat(dados.getMaterias().get(0).topicos()).isNotEmpty();
        assertThat(dados.getMaterias()).allMatch(m -> !m.topicos().isEmpty());
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
