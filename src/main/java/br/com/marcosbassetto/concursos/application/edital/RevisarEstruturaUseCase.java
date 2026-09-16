package br.com.marcosbassetto.concursos.application.edital;

import br.com.marcosbassetto.concursos.common.exception.BusinessException;
import br.com.marcosbassetto.concursos.common.exception.ErrorCodes;
import br.com.marcosbassetto.concursos.common.exception.ResourceNotFoundException;
import br.com.marcosbassetto.concursos.domain.concurso.repository.ConcursoRepository;
import br.com.marcosbassetto.concursos.domain.edital.dto.RevisaoEstruturaRequest;
import br.com.marcosbassetto.concursos.domain.edital.entity.EditalImportacaoEntity;
import br.com.marcosbassetto.concursos.domain.edital.entity.MateriaSugeridaEntity;
import br.com.marcosbassetto.concursos.domain.edital.entity.TopicoSugeridoEntity;
import br.com.marcosbassetto.concursos.domain.edital.repository.EditalImportacaoRepository;
import br.com.marcosbassetto.concursos.domain.edital.repository.MateriaSugeridaRepository;
import br.com.marcosbassetto.concursos.domain.edital.repository.TopicoSugeridoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * Aplica as alterações da revisão sobre o staging.
 *
 * Tudo aqui é consequência de uma ação do usuário: renomear, (des)selecionar,
 * remover ou mesclar. Nenhuma mesclagem é inferida — quando o pedido traz
 * {@code mesclarEmId}, os itens de origem são removidos e o conteúdo deles
 * passa para o destino, mas só porque o usuário pediu.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RevisarEstruturaUseCase {

    private final ConcursoRepository concursoRepository;
    private final EditalImportacaoRepository importacaoRepository;
    private final MateriaSugeridaRepository materiaSugeridaRepository;
    private final TopicoSugeridoRepository topicoSugeridoRepository;

    @Transactional
    public void aplicar(Long concursoId, RevisaoEstruturaRequest request, Long usuarioId) {
        validarPropriedade(concursoId, usuarioId);

        EditalImportacaoEntity importacao = buscarImportacao(concursoId);
        validarRevisavel(importacao);

        if (request == null || request.materias() == null) {
            throw new BusinessException(
                    ErrorCodes.DADOS_INVALIDOS,
                    "A revisão precisa informar a lista de matérias.");
        }

        List<MateriaSugeridaEntity> materias =
                materiaSugeridaRepository.findByImportacao_IdOrderByOrdemAsc(importacao.getId());

        aplicarMaterias(materias, request.materias(), importacao);
    }

    private void aplicarMaterias(List<MateriaSugeridaEntity> existentes,
                                 List<RevisaoEstruturaRequest.MateriaRevisaoRequest> pedidos,
                                 EditalImportacaoEntity importacao) {

        // Remoção por omissão: o que o usuário não enviou deixa de existir.
        for (MateriaSugeridaEntity materia : existentes) {
            boolean presente = pedidos.stream()
                    .anyMatch(p -> Objects.equals(p.id(), materia.getId()));

            if (!presente) {
                log.debug("Removendo matéria sugerida id={} por omissão na revisão", materia.getId());
                materiaSugeridaRepository.delete(materia);
            }
        }

        for (RevisaoEstruturaRequest.MateriaRevisaoRequest pedido : pedidos) {
            MateriaSugeridaEntity materia = exigirMateria(existentes, pedido.id());

            if (Boolean.TRUE.equals(pedido.remover())) {
                materiaSugeridaRepository.delete(materia);
                continue;
            }

            if (pedido.mesclarEmId() != null) {
                mesclarMateria(materia, pedido.mesclarEmId(), existentes);
                continue;
            }

            aplicarEdicao(materia, pedido);
            aplicarTopicos(materia, pedido.topicos());
            materiaSugeridaRepository.save(materia);
        }

        reordenar(materiasRestantes(importacao));
    }

    private void aplicarEdicao(MateriaSugeridaEntity materia,
                               RevisaoEstruturaRequest.MateriaRevisaoRequest pedido) {

        if (pedido.nome() != null && !pedido.nome().isBlank()) {
            materia.setNome(pedido.nome().strip());
        }
        if (pedido.selecionada() != null) {
            materia.setSelecionada(pedido.selecionada());
        }
    }

    /**
     * Move o conteúdo da origem para o destino e descarta a origem.
     *
     * O destino precisa aceitar os tópicos: um tópico já existente lá seria
     * barrado pelo unique (materia_id, nome_normalizado) lá adiante.
     */
    private void mesclarMateria(MateriaSugeridaEntity origem,
                                Long destinoId,
                                List<MateriaSugeridaEntity> materias) {

        MateriaSugeridaEntity destino = exigirMateria(materias, destinoId);

        if (Objects.equals(origem.getId(), destino.getId())) {
            throw new BusinessException(
                    ErrorCodes.DADOS_INVALIDOS,
                    "Não é possível mesclar uma matéria com ela mesma.");
        }

        List<TopicoSugeridoEntity> topicosOrigem = topicoSugeridoRepository
                .findByMateriaSugerida_IdOrderByOrdemAsc(origem.getId());

        // Conta no banco em vez de na coleção do destino: ela é lazy e
        // estaria vazia fora de uma sessão inicializada.
        int ordemDestino = topicoSugeridoRepository
                .findByMateriaSugerida_IdOrderByOrdemAsc(destino.getId()).size();

        for (TopicoSugeridoEntity topico : topicosOrigem) {
            topico.setMateriaSugerida(destino);
            topico.setOrdem(ordemDestino + topico.getOrdem());
            topicoSugeridoRepository.save(topico);
        }

        // O destino herda a seleção: mesclar e manter desmarcado apagaria
        // silenciosamente o conteúdo que o usuário acabou de juntar.
        destino.setSelecionada(true);
        destino.setPossivelDuplicidade(false);
        destino.setSimilarA(null);
        materiaSugeridaRepository.save(destino);

        log.info("Matéria sugerida id={} mesclada em id={} por ação do usuário",
                origem.getId(), destino.getId());

        materiaSugeridaRepository.delete(origem);
    }

    private void aplicarTopicos(MateriaSugeridaEntity materia,
                                List<RevisaoEstruturaRequest.TopicoRevisaoRequest> pedidos) {

        if (pedidos == null) return;

        List<TopicoSugeridoEntity> existentes =
                topicoSugeridoRepository.findByMateriaSugerida_IdOrderByOrdemAsc(materia.getId());

        for (TopicoSugeridoEntity topico : existentes) {
            boolean presente = pedidos.stream()
                    .anyMatch(p -> Objects.equals(p.id(), topico.getId()));

            if (!presente) {
                topicoSugeridoRepository.delete(topico);
            }
        }

        for (RevisaoEstruturaRequest.TopicoRevisaoRequest pedido : pedidos) {
            TopicoSugeridoEntity topico = existentes.stream()
                    .filter(t -> Objects.equals(t.getId(), pedido.id()))
                    .findFirst()
                    .orElseThrow(() -> new BusinessException(
                            ErrorCodes.DADOS_INVALIDOS,
                            "Tópico sugerido " + pedido.id() + " não pertence a esta matéria."));

            if (Boolean.TRUE.equals(pedido.remover())) {
                topicoSugeridoRepository.delete(topico);
                continue;
            }

            if (pedido.nome() != null && !pedido.nome().isBlank()) {
                topico.setNome(pedido.nome().strip());
            }
            if (pedido.selecionado() != null) {
                topico.setSelecionado(pedido.selecionado());
            }
            topicoSugeridoRepository.save(topico);
        }
    }

    /** Reordena por posição na resposta, para a confirmação seguir o que a tela mostra. */
    private void reordenar(List<MateriaSugeridaEntity> materias) {
        int ordem = 1;
        for (MateriaSugeridaEntity materia : materias) {
            materia.setOrdem(ordem++);
            materiaSugeridaRepository.save(materia);
        }
    }

    private List<MateriaSugeridaEntity> materiasRestantes(EditalImportacaoEntity importacao) {
        return materiaSugeridaRepository.findByImportacao_IdOrderByOrdemAsc(importacao.getId());
    }

    private MateriaSugeridaEntity exigirMateria(List<MateriaSugeridaEntity> materias, Long id) {
        return materias.stream()
                .filter(m -> Objects.equals(m.getId(), id))
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        ErrorCodes.DADOS_INVALIDOS,
                        "Matéria sugerida " + id + " não pertence a esta importação."));
    }

    private EditalImportacaoEntity buscarImportacao(Long concursoId) {
        return importacaoRepository.findByConcurso_Id(concursoId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCodes.IMPORTACAO_NAO_ENCONTRADA,
                        "Nenhuma importação encontrada para o concurso " + concursoId + "."));
    }

    private void validarRevisavel(EditalImportacaoEntity importacao) {
        if (!importacao.aguardandoRevisao()) {
            throw new BusinessException(
                    ErrorCodes.ESTRUTURA_NAO_REVISAVEL,
                    "A estrutura deste edital não está disponível para revisão. "
                            + "Status atual: " + importacao.getStatus() + ".");
        }
    }

    private void validarPropriedade(Long concursoId, Long usuarioId) {
        if (!concursoRepository.existsByIdAndUsuario_Id(concursoId, usuarioId)) {
            throw new ResourceNotFoundException("Concurso", "id", concursoId);
        }
    }
}
