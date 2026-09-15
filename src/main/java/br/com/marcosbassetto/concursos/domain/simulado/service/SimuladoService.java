package br.com.marcosbassetto.concursos.domain.simulado.service;

import br.com.marcosbassetto.concursos.common.exception.BusinessException;
import br.com.marcosbassetto.concursos.common.exception.ErrorCodes;
import br.com.marcosbassetto.concursos.common.exception.ResourceNotFoundException;
import br.com.marcosbassetto.concursos.domain.concurso.entity.ConcursoEntity;
import br.com.marcosbassetto.concursos.domain.concurso.repository.ConcursoRepository;
import br.com.marcosbassetto.concursos.domain.materia.entity.MateriaEntity;
import br.com.marcosbassetto.concursos.domain.materia.repository.MateriaRepository;
import br.com.marcosbassetto.concursos.domain.questao.entity.QuestaoEntity;
import br.com.marcosbassetto.concursos.domain.questao.repository.QuestaoRepository;
import br.com.marcosbassetto.concursos.domain.simulado.domain.StatusSimulado;
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
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SimuladoService {

    private final SimuladoRepository simuladoRepository;
    private final SimuladoQuestaoRepository simuladoQuestaoRepository;
    private final ConcursoRepository concursoRepository;
    private final MateriaRepository materiaRepository;
    private final QuestaoRepository questaoRepository;

    @Transactional
    public SimuladoEntity criar(Long usuarioId, SimuladoEntity novoSimulado) {
        log.debug("Criando simulado para usuário: {}, concurso: {}", usuarioId, novoSimulado.getConcursoId());

        ConcursoEntity concurso = concursoRepository
                .findByIdAndUsuario_Id(novoSimulado.getConcursoId(), usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Concurso",
                        "id",
                        novoSimulado.getConcursoId()
                ));

        MateriaEntity materia = materiaRepository
                .findById(novoSimulado.getMateriaId())
                .filter(m -> m.getCurso().equals(concurso.getId()))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Matéria",
                        "id",
                        novoSimulado.getMateriaId()
                ));

        int quantidadeDesejada = novoSimulado.getQuantidadeQuestoes() != null
                ? novoSimulado.getQuantidadeQuestoes()
                : 10;

        if (quantidadeDesejada <= 0 || quantidadeDesejada > 100) {
            throw new BusinessException(
                    ErrorCodes.ESTADO_INVALIDO,
                    "A quantidade de questões deve ser entre 1 e 100."
            );
        }

        List<QuestaoEntity> questoesDisponiveis = questaoRepository
                .findByMateriaIdAndAtivoTrue(materia.getId());

        if (questoesDisponiveis.size() < quantidadeDesejada) {
            throw new BusinessException(
                    ErrorCodes.QUESTOES_INSUFICIENTES,
                    "Existem apenas " + questoesDisponiveis.size() +
                            " questões disponíveis para a quantidade solicitada de " + quantidadeDesejada + "."
            );
        }

        Collections.shuffle(questoesDisponiveis);
        List<QuestaoEntity> selecionadas = questoesDisponiveis.subList(0, quantidadeDesejada);

        novoSimulado.setUsuarioId(usuarioId);
        novoSimulado.setStatus(StatusSimulado.CRIADO);
        novoSimulado.setConcursoId(concurso.getId());
        novoSimulado.setMateriaId(materia.getId());
        novoSimulado.setQuantidadeQuestoes(quantidadeDesejada);
        novoSimulado.setCriadoEm(LocalDateTime.now());
        novoSimulado.setAtualizadoEm(LocalDateTime.now());

        SimuladoEntity simuladoSalvo = simuladoRepository.save(novoSimulado);
        log.debug("Simulado criado com ID: {}", simuladoSalvo.getId());

        int ordem = 1;
        List<SimuladoQuestaoEntity> questoesSalvas = new ArrayList<>();
        for (QuestaoEntity questao : selecionadas) {
            SimuladoQuestaoEntity sq = SimuladoQuestaoEntity.builder()
                    .simulado(simuladoSalvo)
                    .questaoId(questao.getId())
                    .ordem(ordem++)
                    .build();
            questoesSalvas.add(simuladoQuestaoRepository.save(sq));
        }

        simuladoSalvo.setQuestoes(questoesSalvas);
        log.debug("Simulado finalizado com {} questões", questoesSalvas.size());
        return simuladoSalvo;
    }

    @Transactional(readOnly = true)
    public List<SimuladoEntity> listar(Long usuarioId) {
        log.debug("Listando simulados do usuário: {}", usuarioId);
        return simuladoRepository.findByUsuarioIdOrderByCriadoEmDesc(usuarioId);
    }

    @Transactional(readOnly = true)
    public SimuladoEntity buscarPorId(Long id, Long usuarioId) {
        log.debug("Buscando simulado ID: {} para usuário: {}", id, usuarioId);
        return simuladoRepository.findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Simulado", "id", id));
    }

    @Transactional
    public SimuladoEntity iniciar(Long id, Long usuarioId) {
        log.debug("Iniciando simulado ID: {} para usuário: {}", id, usuarioId);
        SimuladoEntity simulado = buscarPorId(id, usuarioId);

        if (simulado.getStatus() != StatusSimulado.CRIADO) {
            throw new BusinessException(
                    ErrorCodes.ESTADO_INVALIDO,
                    "Somente simulados com status CRIADO podem ser iniciados. Status atual: " + simulado.getStatus()
            );
        }

        simulado.setStatus(StatusSimulado.EM_ANDAMENTO);
        simulado.setIniciadoEm(LocalDateTime.now());
        simulado.setAtualizadoEm(LocalDateTime.now());
        return simuladoRepository.save(simulado);
    }

    @Transactional
    public SimuladoEntity finalizar(Long id, Long usuarioId) {
        log.debug("Finalizando simulado ID: {} para usuário: {}", id, usuarioId);
        SimuladoEntity simulado = buscarPorId(id, usuarioId);

        if (simulado.getStatus() != StatusSimulado.EM_ANDAMENTO) {
            throw new BusinessException(
                    ErrorCodes.ESTADO_INVALIDO,
                    "Somente simulados em andamento podem ser finalizados. Status atual: " + simulado.getStatus()
            );
        }

        simulado.setStatus(StatusSimulado.FINALIZADO);
        simulado.setFinalizadoEm(LocalDateTime.now());
        simulado.setAtualizadoEm(LocalDateTime.now());

        log.info("Simulado {} finalizado para usuário {}", id, usuarioId);
        return simuladoRepository.save(simulado);
    }

    @Transactional
    public SimuladoEntity cancelar(Long id, Long usuarioId) {
        log.debug("Cancelando simulado ID: {} para usuário: {}", id, usuarioId);
        SimuladoEntity simulado = buscarPorId(id, usuarioId);

        if (simulado.getStatus() == StatusSimulado.FINALIZADO) {
            throw new BusinessException(
                    ErrorCodes.ESTADO_INVALIDO,
                    "Simulados finalizados não podem ser cancelados."
            );
        }

        simulado.setStatus(StatusSimulado.CANCELADO);
        simulado.setAtualizadoEm(LocalDateTime.now());
        return simuladoRepository.save(simulado);
    }
}
