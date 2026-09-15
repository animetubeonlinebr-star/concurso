package br.com.marcosbassetto.concursos.domain.curso.repository;

import br.com.marcosbassetto.concursos.domain.curso.entity.CursoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CursoRepository extends JpaRepository<CursoEntity, Long> {

    List<CursoEntity> findByConcursoIdOrderByOrdemAsc(Long concursoId);

    Optional<CursoEntity> findByConcursoIdAndNomeNormalizado(
            Long concursoId, String nomeNormalizado);

    boolean existsByConcursoIdAndNomeNormalizado(
            Long concursoId, String nomeNormalizado);
}
