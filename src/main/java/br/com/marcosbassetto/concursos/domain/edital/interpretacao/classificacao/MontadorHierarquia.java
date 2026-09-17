package br.com.marcosbassetto.concursos.domain.edital.interpretacao.classificacao;

import br.com.marcosbassetto.concursos.domain.edital.interpretacao.documento.DocumentModel;
import br.com.marcosbassetto.concursos.domain.edital.interpretacao.rascunho.MateriaRascunho;
import br.com.marcosbassetto.concursos.domain.edital.interpretacao.rascunho.TopicoRascunho;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Monta a hierarquia a partir das linhas já classificadas.
 *
 * É o passo "como mapear para a estrutura padrão do sistema?". Substitui a
 * regra antiga, que tratava qualquer linha em maiúsculas como matéria e por
 * isso produzia cabeçalhos institucionais como disciplinas.
 *
 * Regras:
 * - MATERIA abre uma disciplina; os itens seguintes pertencem a ela;
 * - a linha anterior é levada em conta, porque só ela distingue um título de
 *   um parágrafo que o extrator de PDF cortou ao meio;
 * - unidade de nível 1 e agrupamentos compõem o caminho da matéria;
 * - item antes de qualquer disciplina é ruído e é ignorado.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MontadorHierarquia {

    private final ClassificadorUnidadeEdital classificador;

    public List<MateriaRascunho> montar(DocumentModel documento, String bloco) {
        if (bloco == null || bloco.isBlank()) {
            return List.of();
        }

        Map<String, Integer> frequencia = documento == null
                ? Map.of()
                : documento.frequenciaLinhasMaiusculas();

        List<MateriaRascunho> materias = new ArrayList<>();
        List<CaminhoNo> caminho = new ArrayList<>();
        String nomeMateria = null;
        List<TopicoRascunho> topicos = new ArrayList<>();
        String linhaAnterior = null;

        for (String linha : bloco.split("\\R")) {
            String texto = linha.strip();

            if (texto.isEmpty()) {
                // Linha em branco encerra o bloco visual anterior: o que vem
                // depois é começo de assunto, não continuação.
                linhaAnterior = null;
                continue;
            }

            TipoUnidadeEdital tipo = classificador.classificar(texto, linhaAnterior, frequencia);

            switch (tipo) {
                case MATERIA -> {
                    fechar(materias, nomeMateria, caminho, topicos);
                    nomeMateria = limparMarcacao(texto);
                    topicos = new ArrayList<>();
                }
                case TOPICO -> {
                    if (nomeMateria != null) {
                        topicos.add(TopicoRascunho.de(limparMarcacao(texto)));
                    }
                }
                default -> {
                    if (tipo.ehUnidadeNivel1() || tipo.ehAgrupamento()) {
                        fechar(materias, nomeMateria, caminho, topicos);
                        nomeMateria = null;
                        topicos = new ArrayList<>();
                        atualizarCaminho(caminho, limparMarcacao(texto), tipo);
                    }
                }
            }

            linhaAnterior = texto;
        }

        fechar(materias, nomeMateria, caminho, topicos);

        log.debug("Hierarquia montada | linhas={} | materias={}",
                bloco.split("\\R").length, materias.size());

        return materias;
    }

    /**
     * Uma unidade de nível 1 nova substitui a anterior do mesmo tipo; já os
     * agrupamentos acumulam porque são aninhados, não alternativos.
     */
    private void atualizarCaminho(List<CaminhoNo> caminho, String texto, TipoUnidadeEdital tipo) {
        if (!tipo.ehAgrupamento()) {
            caminho.removeIf(no -> no.tipo() == tipo);
        }
        caminho.add(new CaminhoNo(texto, tipo));
    }

    private void fechar(List<MateriaRascunho> materias,
                        String nomeMateria,
                        List<CaminhoNo> caminho,
                        List<TopicoRascunho> topicos) {
        if (nomeMateria == null || nomeMateria.isBlank()) {
            return;
        }
        materias.add(new MateriaRascunho(nomeMateria, nomes(caminho), topicos));
    }

    private List<String> nomes(List<CaminhoNo> caminho) {
        return caminho.stream().map(CaminhoNo::texto).toList();
    }

    /**
     * Remove marcadores de formatação e de lista do texto que vira nome de
     * matéria ou tópico. A classificação precisa vê-los para reconhecer a
     * linha, mas eles não fazem parte do nome.
     */
    private String limparMarcacao(String texto) {
        return texto
                .replaceAll("^\\s*[|#*>]+\\s*", "")
                .replaceAll("\\s*[|#*]+\\s*$", "")
                .replaceAll("^\\s*(?:[▪■◆•\\-]|[IVXLCDM]{1,6}\\.|\\d{1,3}[.)])\\s+", "")
                .strip();
    }

    private record CaminhoNo(String texto, TipoUnidadeEdital tipo) {
    }
}