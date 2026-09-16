package br.com.marcosbassetto.concursos.domain.materia.service;

import br.com.marcosbassetto.concursos.common.exception.BusinessException;
import br.com.marcosbassetto.concursos.common.exception.ErrorCodes;
import br.com.marcosbassetto.concursos.common.exception.ResourceNotFoundException;
import br.com.marcosbassetto.concursos.domain.materia.dto.MateriaResponse;
import br.com.marcosbassetto.concursos.domain.materia.entity.MateriaEntity;
import br.com.marcosbassetto.concursos.domain.materia.repository.MateriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MateriaService {

    private final MateriaRepository materiaRepository;

    @Transactional
    public MateriaEntity criar(MateriaEntity materia) {
        if (materia.getCurso() != null &&
                materiaRepository.existsByCurso_IdAndNomeIgnoreCase(
                        materia.getCurso().getId(),
                        materia.getNome())) {
            throw new BusinessException(
                    ErrorCodes.MATERIA_DUPLICADA,
                    "Já existe uma matéria com o nome '" + materia.getNome() + "' neste curso."
            );
        }
        return materiaRepository.save(materia);
    }

    @Transactional(readOnly = true)
    public MateriaResponse buscarPorId(Long id, Long usuarioId) {
        MateriaEntity materia = materiaRepository.findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Matéria", "id", id));
        return MateriaResponse.from(materia);
    }

    @Transactional(readOnly = true)
    public List<MateriaResponse> listarPorConcurso(Long concursoId, Long usuarioId) {
        return materiaRepository.findByConcursoIdAndUsuarioId(concursoId, usuarioId).stream()
                .map(MateriaResponse::from)
                .toList();
    }
}
