package br.com.marcosbassetto.concursos.domain.resposta.service;

import br.com.marcosbassetto.concursos.common.exception.BusinessException;
import br.com.marcosbassetto.concursos.common.exception.ConflictException;
import br.com.marcosbassetto.concursos.common.exception.ErrorCodes;
import br.com.marcosbassetto.concursos.common.exception.ResourceNotFoundException;
import br.com.marcosbassetto.concursos.domain.questao.domain.TipoQuestao;
import br.com.marcosbassetto.concursos.domain.questao.entity.QuestaoEntity;
import br.com.marcosbassetto.concursos.domain.questao.repository.QuestaoRepository;
import br.com.marcosbassetto.concursos.domain.resposta.dto.AtualizarRespostaRequest;
import br.com.marcosbassetto.concursos.domain.resposta.dto.RegistrarRespostaRequest;
import br.com.marcosbassetto.concursos.domain.resposta.dto.RespostaResponse;
import br.com.marcosbassetto.concursos.domain.resposta.entity.RespostaEntity;
import br.com.marcosbassetto.concursos.domain.resposta.mapper.RespostaMapper;
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
import java.util.List;
import java.util.Optional;

/**
 * Registra, atualiza e consulta as respostas do usuário.
 *
 * A resposta é sempre vinculada a uma {@link SimuladoQuestaoEntity}, e a
 * propriedade é verificada navegando SimuladoQuestao → Simulado → Usuario.
 * O resultado da correção não é calculado aqui.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RespostaService {

    private final RespostaRepository respostaRepository;
    private final SimuladoQuestaoRepository simuladoQuestaoRepository;
    private final SimuladoRepository simuladoRepository;
    private final QuestaoRepository questaoRepository;
    private final RespostaMapper respostaMapper;

    @Transactional
    public RespostaResponse registrar(Long usuarioId, Long simuladoId, Long simuladoQuestaoId,
                                     RegistrarRespostaRequest request) {
        SimuladoQuestaoEntity simuladoQuestao = buscarSimuladoQuestaoDoUsuario(
                usuarioId, simuladoId, simuladoQuestaoId);
        validarSimuladoAceitaResposta(simuladoQuestao.getSimulado());

        Optional<RespostaEntity> existente =
                respostaRepository.findBySimuladoQuestao_Id(simuladoQuestaoId);

        String alternativa = normalizar(request.alternativaSelecionada());
        String texto = normalizar(request.respostaTexto());
        validarResposta(simuladoQuestao, alternativa, texto);

        if (existente.isPresent()) {
            // Requisições repetidas do frontend (retry de rede) não devem criar
            // um segundo registro — reaproveitamos a resposta existente.
            RespostaEntity resposta = existente.get();
            resposta.alterar(alternativa, texto);
            log.debug("Resposta reaproveitada para simuladoQuestao {}", simuladoQuestaoId);
            return respostaMapper.toResponse(respostaRepository.save(resposta));
        }

        RespostaEntity resposta = RespostaEntity.builder()
                .simuladoQuestao(simuladoQuestao)
                .alternativaSelecionada(alternativa)
                .respostaTexto(texto)
                .respondidaEm(LocalDateTime.now())
                .atualizadaEm(LocalDateTime.now())
                .build();

        log.debug("Registrando resposta para simuladoQuestao {}", simuladoQuestaoId);
        return respostaMapper.toResponse(respostaRepository.save(resposta));
    }

    @Transactional
    public RespostaResponse atualizar(Long usuarioId, Long simuladoId, Long simuladoQuestaoId,
                                     AtualizarRespostaRequest request) {
        SimuladoQuestaoEntity simuladoQuestao = buscarSimuladoQuestaoDoUsuario(
                usuarioId, simuladoId, simuladoQuestaoId);
        validarSimuladoAceitaResposta(simuladoQuestao.getSimulado());

        RespostaEntity resposta = respostaRepository.findBySimuladoQuestao_Id(simuladoQuestaoId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Resposta", "simuladoQuestaoId", simuladoQuestaoId));

        String alternativa = normalizar(request.alternativaSelecionada());
        String texto = normalizar(request.respostaTexto());
        validarResposta(simuladoQuestao, alternativa, texto);

        resposta.alterar(alternativa, texto);
        log.debug("Atualizando resposta do simuladoQuestao {}", simuladoQuestaoId);
        return respostaMapper.toResponse(respostaRepository.save(resposta));
    }

    @Transactional(readOnly = true)
    public Optional<RespostaResponse> buscar(Long usuarioId, Long simuladoId, Long simuladoQuestaoId) {
        buscarSimuladoQuestaoDoUsuario(usuarioId, simuladoId, simuladoQuestaoId);

        return respostaRepository.findBySimuladoQuestao_Id(simuladoQuestaoId)
                .map(respostaMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public List<RespostaResponse> listarPorSimulado(Long usuarioId, Long simuladoId) {
        simuladoRepository.findByIdAndUsuarioId(simuladoId, usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Simulado", "id", simuladoId));

        return respostaRepository.findBySimuladoId(simuladoId)
                .stream()
                .map(respostaMapper::toResponse)
                .toList();
    }

    private SimuladoQuestaoEntity buscarSimuladoQuestaoDoUsuario(
            Long usuarioId, Long simuladoId, Long simuladoQuestaoId) {

        SimuladoQuestaoEntity simuladoQuestao = simuladoQuestaoRepository
                .findById(simuladoQuestaoId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Questão do simulado", "id", simuladoQuestaoId));

        SimuladoEntity simulado = simuladoQuestao.getSimulado();
        if (simulado == null || !simulado.getUsuarioId().equals(usuarioId)) {
            // Não revela a existência do recurso de outro usuário.
            throw new ResourceNotFoundException("Questão do simulado", "id", simuladoQuestaoId);
        }

        if (simuladoId != null && !simulado.getId().equals(simuladoId)) {
            throw new ResourceNotFoundException("Questão do simulado", "id", simuladoQuestaoId);
        }

        return simuladoQuestao;
    }

    private void validarSimuladoAceitaResposta(SimuladoEntity simulado) {
        if (simulado.isFinalizado()) {
            throw new ConflictException(
                    ErrorCodes.SIMULADO_FINALIZADO,
                    "Não é possível alterar uma resposta após a finalização.");
        }

        if (simulado.isCancelado()) {
            throw new ConflictException(
                    ErrorCodes.SIMULADO_CANCELADO,
                    "Não é possível registrar ou alterar respostas em um simulado cancelado.");
        }

        if (!simulado.permiteResposta()) {
            throw new ConflictException(
                    ErrorCodes.ESTADO_INVALIDO,
                    "Somente simulados em andamento aceitam respostas. Status atual: "
                            + simulado.getStatus());
        }
    }

    /**
     * Valida a resposta conforme o tipo da questão.
     *
     * Uma resposta totalmente vazia é permitida (RN-RES-011) — a questão apenas
     * permanece sem resposta efetiva, e o módulo Correcao a trata como
     * NAO_RESPONDIDA.
     */
    private void validarResposta(SimuladoQuestaoEntity simuladoQuestao,
                                 String alternativa,
                                 String texto) {
        if (alternativa == null && texto == null) {
            return;
        }

        QuestaoEntity questao = questaoRepository.findById(simuladoQuestao.getQuestaoId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Questão", "id", simuladoQuestao.getQuestaoId()));

        TipoQuestao tipo = questao.getTipo();

        if (tipo == TipoQuestao.MULTIPLA_ESCOLHA) {
            if (texto != null) {
                throw new BusinessException(
                        ErrorCodes.RESPOSTA_INVALIDA,
                        "Questões de múltipla escolha aceitam somente alternativaSelecionada.");
            }
            if (!questao.validarResposta(alternativa)) {
                throw new BusinessException(
                        ErrorCodes.RESPOSTA_INVALIDA,
                        "Alternativa selecionada inválida: " + alternativa);
            }
            if (!questao.possuiAlternativa(alternativa)) {
                throw new BusinessException(
                        ErrorCodes.ALTERNATIVA_INEXISTENTE,
                        "A alternativa " + alternativa + " não existe nesta questão.");
            }
            return;
        }

        if (tipo == TipoQuestao.CERTO_ERRADO) {
            if (texto != null) {
                throw new BusinessException(
                        ErrorCodes.RESPOSTA_INVALIDA,
                        "Questões de certo/errado aceitam somente alternativaSelecionada.");
            }
            if (!questao.validarResposta(alternativa)) {
                throw new BusinessException(
                        ErrorCodes.RESPOSTA_INVALIDA,
                        "Resposta inválida para questão de certo/errado. Use CERTO ou ERRADO.");
            }
            return;
        }

        if (tipo == TipoQuestao.DISCURSIVA) {
            if (alternativa != null) {
                throw new BusinessException(
                        ErrorCodes.RESPOSTA_INVALIDA,
                        "Questões discursivas aceitam somente respostaTexto.");
            }
            if (texto != null && texto.isBlank()) {
                throw new BusinessException(
                        ErrorCodes.RESPOSTA_INVALIDA,
                        "A resposta textual não pode ser vazia.");
            }
        }
    }

    private String normalizar(String valor) {
        if (valor == null) {
            return null;
        }
        String limpo = valor.trim();
        return limpo.isEmpty() ? null : limpo;
    }
}