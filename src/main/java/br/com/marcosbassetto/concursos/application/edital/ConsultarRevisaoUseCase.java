package br.com.marcosbassetto.concursos.application.edital;

import br.com.marcosbassetto.concursos.common.exception.BusinessException;
import br.com.marcosbassetto.concursos.common.exception.ErrorCodes;
import br.com.marcosbassetto.concursos.common.exception.ResourceNotFoundException;
import br.com.marcosbassetto.concursos.domain.concurso.repository.ConcursoRepository;
import br.com.marcosbassetto.concursos.domain.edital.dto.RevisaoEstruturaResponse;
import br.com.marcosbassetto.concursos.domain.edital.entity.EditalImportacaoEntity;
import br.com.marcosbassetto.concursos.domain.edital.entity.MateriaSugeridaEntity;
import br.com.marcosbassetto.concursos.domain.edital.entity.TopicoSugeridoEntity;
import br.com.marcosbassetto.concursos.domain.edital.repository.EditalImportacaoRepository;
import br.com.marcosbassetto.concursos.domain.edital.repository.MateriaSugeridaRepository;
import br.com.marcosbassetto.concursos.domain.edital.repository.TopicoSugeridoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ConsultarRevisaoUseCase {

    private final ConcursoRepository concursoRepository;
    private final EditalImportacaoRepository importacaoRepository;
    private final MateriaSugeridaRepository materiaSugeridaRepository;
    private final TopicoSugeridoRepository topicoSugeridoRepository;

    @Transactional(readOnly = true)
    public RevisaoEstruturaResponse consultar(Long concursoId, Long usuarioId) {
        validarPropriedade(concursoId, usuarioId);

        EditalImportacaoEntity importacao = buscarImportacao(concursoId);

        if (!importacao.aguardandoRevisao()) {
            throw new BusinessException(
                    ErrorCodes.ESTRUTURA_NAO_REVISAVEL,
                    "A estrutura deste edital não está disponível para revisão. "
                            + "Status atual: " + importacao.getStatus() + ".");
        }

        List<MateriaSugeridaEntity> materias =
                materiaSugeridaRepository.findByImportacao_IdOrderByOrdemAsc(importacao.getId());

        List<RevisaoEstruturaResponse.MateriaRevisaoResponse> materiasResponse = materias.stream()
                .map(this::toMateriaResponse)
                .toList();

        return new RevisaoEstruturaResponse(
                concursoId,
                importacao.getId(),
                importacao.getStatus(),
                null,
                materiasResponse,
                materias.isEmpty()
                        ? "Nenhuma matéria foi extraída deste edital. Revise o arquivo enviado."
                        : null);
    }

    private RevisaoEstruturaResponse.MateriaRevisaoResponse toMateriaResponse(
            MateriaSugeridaEntity materia) {

        List<TopicoSugeridoEntity> topicos =
                topicoSugeridoRepository.findByMateriaSugerida_IdOrderByOrdemAsc(materia.getId());

        return new RevisaoEstruturaResponse.MateriaRevisaoResponse(
                materia.getId(),
                materia.getNome(),
                materia.getOrdem(),
                materia.getSelecionada(),
                materia.getPossivelDuplicidade(),
                materia.getSimilarAId(),
                nomeSimilar(materia),
                topicos.stream().map(this::toTopicoResponse).toList());
    }

    private RevisaoEstruturaResponse.TopicoRevisaoResponse toTopicoResponse(
            TopicoSugeridoEntity topico) {

        return new RevisaoEstruturaResponse.TopicoRevisaoResponse(
                topico.getId(),
                topico.getNome(),
                topico.getOrdem(),
                topico.getSelecionado(),
                topico.getPossivelDuplicidade(),
                topico.getSimilarAId(),
                topico.getSimilarA() != null ? topico.getSimilarA().getNome() : null);
    }

    private String nomeSimilar(MateriaSugeridaEntity materia) {
        return materia.getSimilarA() != null ? materia.getSimilarA().getNome() : null;
    }

    private EditalImportacaoEntity buscarImportacao(Long concursoId) {
        return importacaoRepository.findByConcurso_Id(concursoId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCodes.IMPORTACAO_NAO_ENCONTRADA,
                        "Nenhuma importação encontrada para o concurso " + concursoId + "."));
    }

    private void validarPropriedade(Long concursoId, Long usuarioId) {
        if (!concursoRepository.existsByIdAndUsuario_Id(concursoId, usuarioId)) {
            throw new ResourceNotFoundException("Concurso", "id", concursoId);
        }
    }
}
