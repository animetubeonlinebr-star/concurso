package br.com.marcosbassetto.concursos.domain.edital.service;

import br.com.marcosbassetto.concursos.common.util.NomeNormalizer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Detecta possíveis duplicidades em nomes de matérias/tópicos.
 *
 * Duas camadas, ambas apenas de sinalização — nunca remove ou mescla nada
 * automaticamente, porque a decisão é sempre do usuário:
 *
 * 1. exata: mesma chave normalizada;
 * 2. aproximada: similaridade de tokens (Jaccard) acima do limiar.
 */
@Slf4j
@Component
public class DetectorDuplicidade {

    private final double limiarSimilaridade;

    public DetectorDuplicidade(
            @Value("${edital.duplicidade.limiar:0.7}") double limiarSimilaridade) {
        this.limiarSimilaridade = limiarSimilaridade;
    }

    /**
     * Índices repetidos na mesma lista. O primeiro índice de cada grupo é a
     * referência; os demais são marcados como possíveis duplicidades dele.
     */
    public List<OcorrenciaDuplicada> detectar(List<String> nomes) {
        List<OcorrenciaDuplicada> encontradas = new ArrayList<>();
        if (nomes == null || nomes.size() < 2) {
            return encontradas;
        }

        List<String> normalizados = nomes.stream()
                .map(NomeNormalizer::normalizar)
                .toList();

        for (int i = 0; i < nomes.size(); i++) {
            if (normalizados.get(i).isBlank()) continue;
            for (int j = i + 1; j < nomes.size(); j++) {
                if (normalizados.get(j).isBlank()) continue;

                if (normalizados.get(i).equals(normalizados.get(j))) {
                    encontradas.add(new OcorrenciaDuplicada(i, j, 1.0, TipoDuplicidade.EXATA));
                    continue;
                }

                double similaridade = similaridade(normalizados.get(i), normalizados.get(j));
                if (similaridade >= limiarSimilaridade) {
                    encontradas.add(new OcorrenciaDuplicada(
                            i, j, similaridade, TipoDuplicidade.APROXIMADA));
                }
            }
        }

        log.debug("DetectorDuplicidade | itens={} ocorrencias={}", nomes.size(), encontradas.size());
        return encontradas;
    }

    /**
     * Coeficiente de Jaccard sobre tokens normalizados: interseção dividida
     * pela união. Determinístico e barato, adequado a nomes curtos como
     * "Direito Constitucional" e "Noções de Direito Constitucional".
     */
    public double similaridade(String normalizadoA, String normalizadoB) {
        Set<String> a = tokens(normalizadoA);
        Set<String> b = tokens(normalizadoB);

        if (a.isEmpty() || b.isEmpty()) {
            return 0.0;
        }

        Set<String> intersecao = new LinkedHashSet<>(a);
        intersecao.retainAll(b);

        Set<String> uniao = new LinkedHashSet<>(a);
        uniao.addAll(b);

        return (double) intersecao.size() / uniao.size();
    }

    private Set<String> tokens(String normalizado) {
        if (normalizado == null || normalizado.isBlank()) {
            return Set.of();
        }
        return new LinkedHashSet<>(Arrays.asList(normalizado.split("\\s+")));
    }

    public record OcorrenciaDuplicada(
            int indiceReferencia,
            int indiceDuplicado,
            double similaridade,
            TipoDuplicidade tipo
    ) {
    }

    public enum TipoDuplicidade {
        EXATA,
        APROXIMADA
    }
}
