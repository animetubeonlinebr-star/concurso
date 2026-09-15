package br.com.marcosbassetto.concursos.domain.materia.repository;

import br.com.marcosbassetto.concursos.common.enums.Status;
import br.com.marcosbassetto.concursos.domain.materia.domain.OrigemMateria;
import br.com.marcosbassetto.concursos.domain.materia.entity.MateriaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MateriaRepository extends JpaRepository<MateriaEntity, Long> {


    List<MateriaEntity> findByCurso_IdOrderByOrdemAsc(Long cursoId);

    List<MateriaEntity> findByCurso_IdAndStatusOrderByOrdemAsc(Long cursoId, Status status);

    List<MateriaEntity> findByCurso_Id(Long cursoId);


    List<MateriaEntity> findByCurso_IdIn(List<Long> cursoIds);

    Optional<MateriaEntity> findByCurso_IdAndNomeNormalizado(
            Long cursoId, String nomeNormalizado);

    boolean existsByCurso_IdAndNomeNormalizado(Long cursoId, String nomeNormalizado);

    boolean existsByCurso_IdAndNomeIgnoreCase(Long cursoId, String nome);

    boolean existsByCurso_IdAndNomeIgnoreCaseAndStatus(Long cursoId, String nome, Status status);

    long countByCurso_IdAndStatus(Long cursoId, Status status);

    List<MateriaEntity> findByCurso_IdAndOrigem(Long cursoId, OrigemMateria origem);


    @Query("SELECT m FROM MateriaEntity m " +
            "JOIN m.curso c " +
            "JOIN c.concurso co " +
            "WHERE m.id = :id AND co.usuario.id = :usuarioId")
    Optional<MateriaEntity> findByIdAndUsuarioId(
            @Param("id") Long id,
            @Param("usuarioId") Long usuarioId);

    @Query("SELECT m FROM MateriaEntity m " +
            "WHERE m.curso.concurso.id = :concursoId AND m.nome = :nome")
    Optional<MateriaEntity> findByConcursoIdAndNome(
            @Param("concursoId") Long concursoId,
            @Param("nome") String nome);



    @Query("SELECT m FROM MateriaEntity m " +
            "WHERE LOWER(m.nome) LIKE LOWER(CONCAT('%', :nome, '%'))")
    List<MateriaEntity> findByNomeContainingIgnoreCase(@Param("nome") String nome);

    @Query("SELECT DISTINCT m FROM MateriaEntity m " +
            "JOIN m.questoes q " +
            "WHERE m.curso.id = :cursoId AND q.ativo = true")
    List<MateriaEntity> findMateriasComQuestoesByCursoId(@Param("cursoId") Long cursoId);

    @Query("SELECT m FROM MateriaEntity m " +
            "WHERE m.curso.id = :cursoId " +
            "AND NOT EXISTS (SELECT q FROM QuestaoEntity q WHERE q.materia.id = m.id)")
    List<MateriaEntity> findMateriasSemQuestoesByCursoId(@Param("cursoId") Long cursoId);
}