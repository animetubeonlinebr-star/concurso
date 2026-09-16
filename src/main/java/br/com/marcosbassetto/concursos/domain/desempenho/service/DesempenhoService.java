package br.com.marcosbassetto.concursos.domain.desempenho.service;

import br.com.marcosbassetto.concursos.domain.correcao.domain.ResultadoCorrecao;
import br.com.marcosbassetto.concursos.domain.correcao.dto.DesempenhoResponse;
import br.com.marcosbassetto.concursos.domain.correcao.entity.CorrecaoEntity;
import br.com.marcosbassetto.concursos.domain.correcao.mapper.CorrecaoMapper;
import br.com.marcosbassetto.concursos.domain.correcao.repository.CorrecaoRepository;
import br.com.marcosbassetto.concursos.domain.desempenho.dto.DesempenhoMateriaResponse;
import br.com.marcosbassetto.concursos.domain.desempenho.dto.EvolucaoDesempenhoResponse;
import br.com.marcosbassetto.concursos.domain.materia.entity.MateriaEntity;
import br.com.marcosbassetto.concursos.domain.materia.repository.MateriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Agrega o histórico de correções para os painéis de desempenho.
 *
 * O denominador é sempre o número de correções (questões efetivamente
 * apuradas), nunca a quantidade de questões criadas: um simulado iniciado e não
 * finalizado não deve derrubar o percentual do usuário.
 */
@Service
@RequiredArgsConstructor
public class DesempenhoService {

    private final CorrecaoRepository correcaoRepository;
    private final MateriaRepository materiaRepository;
    private final CorrecaoMapper correcaoMapper;

    @Transactional(readOnly = true)
    public DesempenhoResponse consultar(Long usuarioId) {
        return correcaoMapper.toDesempenhoResponse(correcoesDoUsuario(usuarioId));
    }

    @Transactional(readOnly = true)
    public DesempenhoResponse consultarPorConcurso(Long usuarioId, Long concursoId) {
        List<CorrecaoEntity> correcoes = correcoesDoUsuario(usuarioId).stream()
                .filter(correcao -> concursoId.equals(correcao.getSimulado().getConcursoId()))
                .toList();

        return correcaoMapper.toDesempenhoResponse(correcoes);
    }

    @Transactional(readOnly = true)
    public List<DesempenhoMateriaResponse> consultarPorMateria(Long usuarioId, Long concursoId) {
        Map<Long, List<CorrecaoEntity>> porMateria = correcoesDoUsuario(usuarioId).stream()
                .filter(correcao -> concursoId.equals(correcao.getSimulado().getConcursoId()))
                .collect(Collectors.groupingBy(
                        correcao -> correcao.getSimulado().getMateriaId(),
                        LinkedHashMap::new,
                        Collectors.toList()));

        Map<Long, String> nomesPorMateria = consultarNomesMaterias(porMateria.keySet());

        List<DesempenhoMateriaResponse> resposta = new ArrayList<>();
        porMateria.forEach((materiaId, correcoes) -> {
            int total = correcoes.size();
            int corretas = contar(correcoes, ResultadoCorrecao.CORRETA);
            int incorretas = contar(correcoes, ResultadoCorrecao.INCORRETA);

            resposta.add(new DesempenhoMateriaResponse(
                    materiaId,
                    nomesPorMateria.get(materiaId),
                    total,
                    corretas,
                    incorretas,
                    percentual(corretas, total)));
        });

        return resposta;
    }

    @Transactional(readOnly = true)
    public EvolucaoDesempenhoResponse consultarEvolucao(Long usuarioId) {
        Map<LocalDate, List<CorrecaoEntity>> porDia = correcoesDoUsuario(usuarioId).stream()
                .collect(Collectors.groupingBy(
                        correcao -> correcao.getCorrigidaEm().toLocalDate(),
                        TreeMap::new,
                        Collectors.toList()));

        List<EvolucaoDesempenhoResponse.PontoEvolucao> pontos = porDia.entrySet().stream()
                .map(dia -> {
                    int total = dia.getValue().size();
                    int corretas = contar(dia.getValue(), ResultadoCorrecao.CORRETA);
                    return new EvolucaoDesempenhoResponse.PontoEvolucao(
                            dia.getKey(), total, corretas, percentual(corretas, total));
                })
                .toList();

        return new EvolucaoDesempenhoResponse(pontos);
    }

    private List<CorrecaoEntity> correcoesDoUsuario(Long usuarioId) {
        return correcaoRepository.findByUsuarioId(usuarioId);
    }

    private int contar(List<CorrecaoEntity> correcoes, ResultadoCorrecao resultado) {
        return (int) correcoes.stream()
                .filter(correcao -> resultado == correcao.getResultado())
                .count();
    }

    /** Carrega os nomes em uma única query, evitando uma busca por matéria. */
    private Map<Long, String> consultarNomesMaterias(Set<Long> materiaIds) {
        if (materiaIds.isEmpty()) {
            return Map.of();
        }

        return materiaRepository.findAllById(materiaIds).stream()
                .collect(Collectors.toMap(MateriaEntity::getId, MateriaEntity::getNome));
    }

    private double percentual(int corretas, int total) {
        if (total <= 0) {
            return 0.0;
        }
        return Math.round((corretas * 100.0 / total) * 100.0) / 100.0;
    }
}