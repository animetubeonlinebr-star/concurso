package br.com.marcosbassetto.concursos.domain.edital.extractor.impl;

import br.com.marcosbassetto.concursos.domain.edital.domain.PadraoEdital;
import br.com.marcosbassetto.concursos.domain.edital.dto.CursoExtraido;
import br.com.marcosbassetto.concursos.domain.edital.dto.DadosConcurso;
import br.com.marcosbassetto.concursos.domain.edital.dto.EstruturaEditalDTO;
import br.com.marcosbassetto.concursos.domain.edital.dto.MateriaExtraida;
import br.com.marcosbassetto.concursos.domain.edital.extractor.TextoPreProcessador;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ExtratorDisciplinaCabecalhoTest {

    private ExtratorDisciplinaCabecalho extrator;

    @BeforeEach
    void setUp() {
        extrator = new ExtratorDisciplinaCabecalho(new TextoPreProcessador());
    }

    @Test
    @DisplayName("declara o padrão DISCIPLINA_CABECALHO")
    void deveDeclararPadrao() {
        assertThat(extrator.padraoSuportado()).isEqualTo(PadraoEdital.DISCIPLINA_CABECALHO);
    }

    @Test
    @DisplayName("extrai dados do concurso do cabeçalho do UNASP")
    void deveExtrairDadosDoConcurso() {
        String texto = textoUnaspMinimo();
        EstruturaEditalDTO estrutura = extrator.extrair(texto);

        DadosConcurso dados = estrutura.dadosConcurso();
        assertThat(dados).isNotNull();
        assertThat(dados.orgao()).containsIgnoringCase("UNASP");
        assertThat(dados.banca()).isEqualTo("Fundação Carlos Chagas");
        assertThat(dados.ano()).isEqualTo(2027);
        assertThat(dados.cargo()).containsIgnoringCase("Medicina");
    }

    @Test
    @DisplayName("extrai matérias segmentadas por cabeçalho")
    void deveExtrairMateriasSegmentadas() {
        String texto = textoUnaspMinimo();
        EstruturaEditalDTO estrutura = extrator.extrair(texto);

        List<CursoExtraido> cursos = estrutura.cursos();
        assertThat(cursos).isNotEmpty();

        List<MateriaExtraida> materias = cursos.get(0).materias();
        assertThat(materias).extracting(MateriaExtraida::nome)
                .contains("Língua Portuguesa", "Literatura Brasileira");
    }

    @Test
    @DisplayName("extrai tópicos em bullets")
    void deveExtrairTopicosBullet() {
        String texto = textoUnaspMinimo();
        EstruturaEditalDTO estrutura = extrator.extrair(texto);

        List<MateriaExtraida> materias = estrutura.cursos().get(0).materias();
        MateriaExtraida portugues = materias.stream()
                .filter(m -> m.nome().equals("Língua Portuguesa"))
                .findFirst()
                .orElseThrow();

        assertThat(portugues.topicos()).isNotEmpty();
    }

    @Test
    @DisplayName("não confunde subtópico romano ## III. com disciplina")
    void naoDeveConfundirSubtopicoRomanoComDisciplina() {
        String texto = textoUnaspMinimo();
        EstruturaEditalDTO estrutura = extrator.extrair(texto);

        List<String> nomes = estrutura.cursos().get(0).materias()
                .stream().map(MateriaExtraida::nome).toList();

        assertThat(nomes).doesNotContain("III. Os Seres Vivos e o Ambiente");
    }

    @Test
    @DisplayName("não quebra com texto vazio")
    void naoDeveQuebrarComTextoVazio() {
        EstruturaEditalDTO estrutura = extrator.extrair("");
        assertThat(estrutura).isNotNull();
        assertThat(estrutura.cursos()).isEmpty();
    }

    private String textoUnaspMinimo() {
        return """
                CENTRO UNIVERSITÁRIO ADVENTISTA DE SÃO PAULO - UNASP
                Processo Seletivo – 2027
                Edital de Abertura de Inscrições

                O REITOR DO CENTRO UNIVERSITÁRIO ADVENTISTA DE SÃO PAULO- UNASP,
                tendo em vista o contrato celebrado com a Fundação Carlos Chagas,
                torna público o presente Edital para o PROCESSO SELETIVO – 2027
                do Curso de Medicina.

                ## ANEXO II

                ## CONTEÚDO PROGRAMÁTICO

                ## Língua Portuguesa
                ▪ Compreensão e interpretação de texto.
                ▪ Tipologia textual: dissertação, narração, descrição.
                ▪ Morfologia. Classes de palavras.

                ## Literatura Brasileira
                ▪ Manifestações literárias do período colonial.
                ▪ Romantismo. Autores e obras representativos.

                ## Matemática
                I. Conjuntos numéricos:
                ▪ Números naturais e inteiros.
                ▪ Números racionais e irracionais.
                II. Polinômios:
                ▪ Conceito, grau e propriedades.

                ## III. Os Seres Vivos e o Ambiente
                ▪ Populações, comunidades e ecossistemas.

                ## ANEXO III
                CRONOGRAMA DAS PROVAS E PUBLICAÇÕES
                """;
    }
}
