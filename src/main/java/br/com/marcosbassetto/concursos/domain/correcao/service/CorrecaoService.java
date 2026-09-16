package br.com.marcosbassetto.concursos.domain.correcao.service;

import br.com.marcosbassetto.concursos.common.exception.BusinessException;
import br.com.marcosbassetto.concursos.common.exception.ConflictException;
import br.com.marcosbassetto.concursos.common.exception.ErrorCodes;
import br.com.marcosbassetto.concursos.common.exception.ResourceNotFoundException;
import br.com.marcosbassetto.concursos.domain.correcao.domain.ResultadoCorrecao;
import br.com.marcosbassetto.concursos.domain.correcao.dto.CorrecaoResponse;
import br.com.marcosbassetto.concursos.domain.correcao.dto.ResultadoQuestaoResponse;
import br.com.marcosbassetto.concursos.domain.correcao.dto.ResultadoSimuladoResponse;
import br.com.marcosbassetto.concursos.domain.correcao.entity.CorrecaoEntity;
import br.com.marcosbassetto.concursos.domain.correcao.mapper.CorrecaoMapper;
import br.com.marcosbassetto.concursos.domain.correcao.repository.CorrecaoRepository;
import br.com.marcosbassetto.concursos.domain.questao.entity.QuestaoEntity;
import br.com.marcosbassetto.concursos.domain.questao.repository.QuestaoRepository;
import br.com.marcosbassetto.concursos.domain.resposta.entity.RespostaEntity;
import br.com.marcosbassetto.concursos.domain.resposta.repository.RespostaRepository;
import br.com.marcosbassetto.concursos.domain.simulado.entity.SimuladoEntity;
import br.com.marcosbassetto.concursos.domain.simulado.entity.SimuladoQuestaoEntity;
import br.com.marcosbassetto.concursos.domain.simulado.repository.SimuladoQuestaoRepository;
import br.com.marcosbassetto.concursos.domain.simulado.repository.SimuladoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Corrige simulados comparando as respostas registradas com o gabarito do backend.
 *
 * Toda a apuração é determinística: o frontend nunca informa resultado nem
 * pontuação. O gabarito é sempre lido da entidade Questao.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CorrecaoService {

    private final CorrecaoRepository correcaoRepository;
    private final SimuladoRepository simuladoRepository;
    private final SimuladoQuestaoRepository simuladoQuestaoRepository;
    private final RespostaRepository respostaRepository;
    private final QuestaoRepository questaoRepository;
    private final CorrecaoMapper correcaoMapper;

    /**
     * Corrige o simulado de forma idempotente.
     *
     * Executar novamente reaproveita as correções existentes, atualizando-as se
     * houver divergência (ex.: resposta alterada antes da finalização).
     */
    @Transactional
    public ResultadoSimuladoResponse corrigirSimulado(Long usuarioId, Long simuladoId) {
        SimuladoEntity simulado = buscarSimuladoDoUsuario(usuarioId, simuladoId);
        validarSimuladoFinalizado(simulado);

        List<SimuladoQuestaoEntity> questoes =
                simuladoQuestaoRepository.findBySimuladoIdOrderByOrdemAsc(simuladoId);

        if (questoes.isEmpty()) {
            throw new BusinessException(
                    ErrorCodes.ESTADO_INVALIDO,
                    "O simulado não possui questões para correção.");
        }

        Map<Long, RespostaEntity> respostasPorQuestao = respostaRepository.findBySimuladoId(simuladoId)
                .stream()
                .collect(Collectors.toMap(
                        RespostaEntity::getSimuladoQuestaoId,
                        Function.identity(),
                        // Defensivo: a unicidade é garantida no banco, mas uma
                        // duplicata inesperada não deve derrubar a correção.
                        (a, b) -> a
                ));

        List<CorrecaoEntity> correcoes = new ArrayList<>();

        for (SimuladoQuestaoEntity simuladoQuestao : questoes) {
            correcoes.add(corrigirQuestao(simulado, simuladoQuestao, respostasPorQuestao));
        }

        ResultadoSimuladoResponse resultado = correcaoMapper.toSimuladoResponse(simuladoId, correcoes);
        log.info("Simulado {} corrigido: {}/{} corretas ({}%)",
                simuladoId, resultado.questoesCorretas(), resultado.totalQuestoes(),
                resultado.percentualAcerto());

        return resultado;
    }

    /**
     * Recálculo explícito. Hoje produz o mesmo resultado de
     * {@link #corrigirSimulado}, mas fica separado porque alterar gabarito ou
     * reprocessar em lote são operações administrativas distintas.
     */
    @Transactional
    public ResultadoSimuladoResponse recalcular(Long usuarioId, Long simuladoId) {
        log.info("Recalculando correção do simulado {}", simuladoId);
        return corrigirSimulado(usuarioId, simuladoId);
    }

    @Transactional(readOnly = true)
    public CorrecaoResponse buscarPorQuestao(Long usuarioId, Long simuladoId, Long simuladoQuestaoId) {
        SimuladoEntity simulado = buscarSimuladoDoUsuario(usuarioId, simuladoId);

        SimuladoQuestaoEntity simuladoQuestao = simuladoQuestaoRepository
                .findById(simuladoQuestaoId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Questão do simulado", "id", simuladoQuestaoId));

        if (!simulado.getId().equals(simuladoQuestao.getSimulado().getId())) {
            throw new ResourceNotFoundException("Questão do simulado", "id", simuladoQuestaoId);
        }

        CorrecaoEntity correcao = correcaoRepository.findBySimuladoQuestao_Id(simuladoQuestaoId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Correção", "simuladoQuestaoId", simuladoQuestaoId));

        // O gabarito só é revelado após a finalização.
        if (!simulado.isFinalizado()) {
            throw new ConflictException(
                    ErrorCodes.SIMULADO_NAO_FINALIZADO,
                    "O gabarito só fica disponível após a finalização do simulado.");
        }

        return correcaoMapper.toResponse(correcao);
    }

    @Transactional(readOnly = true)
    public List<CorrecaoResponse> listarPorSimulado(Long usuarioId, Long simuladoId) {
        SimuladoEntity simulado = buscarSimuladoDoUsuario(usuarioId, simuladoId);
        validarSimuladoFinalizado(simulado);

        return correcaoRepository.findBySimuladoId(simuladoId)
                .stream()
                .map(correcaoMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ResultadoSimuladoResponse consultarResultado(Long usuarioId, Long simuladoId) {
        SimuladoEntity simulado = buscarSimuladoDoUsuario(usuarioId, simuladoId);
        validarSimuladoFinalizado(simulado);

        List<CorrecaoEntity> correcoes = correcaoRepository.findBySimuladoId(simuladoId);

        if (correcoes.isEmpty()) {
            throw new ConflictException(
                    ErrorCodes.SIMULADO_NAO_CORRIGIDO,
                    "O simulado ainda não foi corrigido.");
        }

        return correcaoMapper.toSimuladoResponse(simuladoId, correcoes);
    }

    @Transactional(readOnly = true)
    public List<ResultadoQuestaoResponse> listarResultadosPorQuestao(Long usuarioId, Long simuladoId) {
        SimuladoEntity simulado = buscarSimuladoDoUsuario(usuarioId, simuladoId);
        validarSimuladoFinalizado(simulado);

        return correcaoRepository.findBySimuladoId(simuladoId)
                .stream()
                .map(correcaoMapper::toQuestaoResponse)
                .toList();
    }

    private CorrecaoEntity corrigirQuestao(SimuladoEntity simulado,
                                           SimuladoQuestaoEntity simuladoQuestao,
                                           Map<Long, RespostaEntity> respostasPorQuestao) {

        QuestaoEntity questao = questaoRepository.findById(simuladoQuestao.getQuestaoId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Questão", "id", simuladoQuestao.getQuestaoId()));

        String gabarito = questao.getRespostaCorreta();
        if (gabarito == null || gabarito.isBlank()) {
            // Não inferir o gabarito a partir da resposta do usuário.
            throw new BusinessException(
                    ErrorCodes.GABARITO_AUSENTE,
                    "A questão " + questao.getId() + " não possui gabarito confiável. "
                            + "Correção não disponível.");
        }

        String respostaUsuario = extrairResposta(respostasPorQuestao.get(simuladoQuestao.getId()));
        ResultadoCorrecao resultado = correcaoMapper.classificar(respostaUsuario, gabarito);

        Optional<CorrecaoEntity> existente =
                correcaoRepository.findBySimuladoQuestao_Id(simuladoQuestao.getId());

        CorrecaoEntity correcao = existente.orElseGet(() -> CorrecaoEntity.builder()
                .simulado(simulado)
                .simuladoQuestao(simuladoQuestao)
                .corrigidaEm(LocalDateTime.now())
                .build());

        correcao.reclassificar(respostaUsuario, gabarito, resultado);
        return correcaoRepository.save(correcao);
    }

    private String extrairResposta(RespostaEntity resposta) {
        if (resposta == null) {
            return null;
        }
        if (resposta.getAlternativaSelecionada() != null && !resposta.getAlternativaSelecionada().isBlank()) {
            return resposta.getAlternativaSelecionada();
        }
        return resposta.getRespostaTexto();
    }

    private SimuladoEntity buscarSimuladoDoUsuario(Long usuarioId, Long simuladoId) {
        return simuladoRepository.findByIdAndUsuarioId(simuladoId, usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Simulado", "id", simuladoId));
    }

    private void validarSimuladoFinalizado(SimuladoEntity simulado) {
        if (!simulado.isFinalizado()) {
            throw new ConflictException(
                    ErrorCodes.SIMULADO_NAO_FINALIZADO,
                    "Somente simulados finalizados podem ser corrigidos. Status atual: "
                            + simulado.getStatus());
        }
    }
}