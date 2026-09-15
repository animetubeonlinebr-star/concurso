package br.com.marcosbassetto.concursos.domain.questao.repository;

import br.com.marcosbassetto.concursos.domain.questao.entity.QuestaoEntity;
import br.com.marcosbassetto.concursos.domain.questao.domain.TipoQuestao;
import br.com.marcosbassetto.concursos.domain.questao.domain.OrigemQuestao;
import br.com.marcosbassetto.concursos.domain.questao.domain.DificuldadeQuestao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuestaoRepository extends JpaRepository<QuestaoEntity, Long> {


    @Query("SELECT q FROM QuestaoEntity q " +
            "JOIN q.materia m " +
            "JOIN m.curso cur " +
            "JOIN cur.concurso c " +
            "WHERE q.id = :id AND c.usuario.id = :usuarioId")
    Optional<QuestaoEntity> findByIdAndUsuarioId(@Param("id") Long id, @Param("usuarioId") Long usuarioId);

    @Query("SELECT q FROM QuestaoEntity q " +
            "WHERE q.ativo = true " +
            "AND (:materiaId IS NULL OR q.materia.id = :materiaId) " +
            "AND (:topicoId IS NULL OR q.topico.id = :topicoId) " +
            "AND (:dificuldade IS NULL OR q.dificuldade = :dificuldade) " +
            "AND (:banca IS NULL OR LOWER(q.banca) = LOWER(:banca)) " +
            "AND (:origem IS NULL OR q.origem = :origem) " +
            "AND (:tipo IS NULL OR q.tipo = :tipo)")
    List<QuestaoEntity> findQuestoesAtivasComFiltros(
            @Param("materiaId") Long materiaId,
            @Param("topicoId") Long topicoId,
            @Param("dificuldade") DificuldadeQuestao dificuldade,
            @Param("banca") String banca,
            @Param("origem") OrigemQuestao origem,
            @Param("tipo") TipoQuestao tipo
    );

    @Query("SELECT q FROM QuestaoEntity q " +
            "WHERE q.ativo = true " +
            "AND (:materiaId IS NULL OR q.materia.id = :materiaId) " +
            "AND (:topicoId IS NULL OR q.topico.id = :topicoId) " +
            "AND (:dificuldade IS NULL OR q.dificuldade = :dificuldade) " +
            "AND (:banca IS NULL OR LOWER(q.banca) = LOWER(:banca)) " +
            "AND (:origem IS NULL OR q.origem = :origem) " +
            "AND (:tipo IS NULL OR q.tipo = :tipo)")
    Page<QuestaoEntity> findQuestoesAtivasComFiltrosPaginado(
            @Param("materiaId") Long materiaId,
            @Param("topicoId") Long topicoId,
            @Param("dificuldade") DificuldadeQuestao dificuldade,
            @Param("banca") String banca,
            @Param("origem") OrigemQuestao origem,
            @Param("tipo") TipoQuestao tipo,
            Pageable pageable
    );

    @Query("SELECT q FROM QuestaoEntity q " +
            "WHERE q.materia.id = :materiaId " +
            "AND q.ativo = true " +
            "AND q.id NOT IN :idsExcluidos")
    List<QuestaoEntity> findQuestoesDisponiveisByMateriaId(
            @Param("materiaId") Long materiaId,
            @Param("idsExcluidos") List<Long> idsExcluidos
    );

    @Query("SELECT q FROM QuestaoEntity q " +
            "WHERE q.topico.id = :topicoId " +
            "AND q.ativo = true " +
            "AND q.id NOT IN :idsExcluidos")
    List<QuestaoEntity> findQuestoesDisponiveisByTopicoId(
            @Param("topicoId") Long topicoId,
            @Param("idsExcluidos") List<Long> idsExcluidos
    );

    @Query(value = "SELECT * FROM questao q " +
            "WHERE q.materia_id = :materiaId AND q.ativo = true " +
            "ORDER BY RANDOM() LIMIT :limite",
            nativeQuery = true)
    List<QuestaoEntity> findQuestoesAleatoriasByMateriaId(
            @Param("materiaId") Long materiaId,
            @Param("limite") int limite
    );

    @Query(value = "SELECT * FROM questao q " +
            "WHERE q.topico_id = :topicoId AND q.ativo = true " +
            "ORDER BY RANDOM() LIMIT :limite",
            nativeQuery = true)
    List<QuestaoEntity> findQuestoesAleatoriasByTopicoId(
            @Param("topicoId") Long topicoId,
            @Param("limite") int limite
    );

    @Query(value = "SELECT * FROM questao q " +
            "WHERE q.materia_id = :materiaId " +
            "AND q.ativo = true " +
            "AND q.dificuldade = :dificuldade " +
            "ORDER BY RANDOM() LIMIT :limite",
            nativeQuery = true)
    List<QuestaoEntity> findQuestoesAleatoriasByMateriaIdAndDificuldade(
            @Param("materiaId") Long materiaId,
            @Param("dificuldade") String dificuldade,
            @Param("limite") int limite
    );


    @Query("SELECT q FROM QuestaoEntity q WHERE q.materia.id = :materiaId AND q.ativo = true")
    List<QuestaoEntity> findByMateriaIdAndAtivoTrue(@Param("materiaId") Long materiaId);

    @Query("SELECT q FROM QuestaoEntity q WHERE q.materia.id = :materiaId")
    List<QuestaoEntity> findByMateriaId(@Param("materiaId") Long materiaId);

    @Query("SELECT q FROM QuestaoEntity q WHERE q.topico.id = :topicoId AND q.ativo = true")
    List<QuestaoEntity> findByTopicoIdAndAtivoTrue(@Param("topicoId") Long topicoId);

    @Query("SELECT q FROM QuestaoEntity q WHERE q.topico.id = :topicoId")
    List<QuestaoEntity> findByTopicoId(@Param("topicoId") Long topicoId);

    @Query("SELECT q FROM QuestaoEntity q WHERE LOWER(q.banca) = LOWER(:banca)")
    List<QuestaoEntity> findByBancaIgnoreCase(@Param("banca") String banca);

    @Query("SELECT q FROM QuestaoEntity q WHERE q.materia.id = :materiaId AND q.ativo = true")
    Page<QuestaoEntity> findByMateriaIdAndAtivoTrue(@Param("materiaId") Long materiaId, Pageable pageable);

    @Query("SELECT q FROM QuestaoEntity q WHERE q.topico.id = :topicoId AND q.ativo = true")
    Page<QuestaoEntity> findByTopicoIdAndAtivoTrue(@Param("topicoId") Long topicoId, Pageable pageable);

    @Query("SELECT COUNT(q) FROM QuestaoEntity q WHERE q.materia.id = :materiaId AND q.ativo = true")
    long countByMateriaIdAndAtivoTrue(@Param("materiaId") Long materiaId);

    @Query("SELECT COUNT(q) FROM QuestaoEntity q WHERE q.topico.id = :topicoId AND q.ativo = true")
    long countByTopicoIdAndAtivoTrue(@Param("topicoId") Long topicoId);

    @Query("SELECT COUNT(q) FROM QuestaoEntity q WHERE q.materia.id = :materiaId")
    long countByMateriaId(@Param("materiaId") Long materiaId);

    @Query("SELECT COUNT(q) FROM QuestaoEntity q " +
            "WHERE q.materia.id = :materiaId " +
            "AND q.ativo = true " +
            "AND (:dificuldade IS NULL OR q.dificuldade = :dificuldade) " +
            "AND (:origem IS NULL OR q.origem = :origem)")
    long countQuestoesDisponiveisByMateriaId(
            @Param("materiaId") Long materiaId,
            @Param("dificuldade") DificuldadeQuestao dificuldade,
            @Param("origem") OrigemQuestao origem
    );

    @Query("SELECT q FROM QuestaoEntity q " +
            "WHERE LOWER(q.enunciado) LIKE LOWER(CONCAT('%', :termo, '%')) " +
            "AND q.ativo = true")
    List<QuestaoEntity> findByEnunciadoContainingIgnoreCase(@Param("termo") String termo);

    @Query("SELECT q FROM QuestaoEntity q " +
            "WHERE LOWER(q.enunciado) LIKE LOWER(CONCAT('%', :termo, '%')) " +
            "AND q.materia.id = :materiaId " +
            "AND q.ativo = true")
    List<QuestaoEntity> findByEnunciadoContainingIgnoreCaseAndMateriaId(
            @Param("termo") String termo,
            @Param("materiaId") Long materiaId
    );

    @Query("SELECT q.materia.id as materiaId, " +
            "q.topico.id as topicoId, " +
            "COUNT(q) as quantidade, " +
            "AVG(CASE WHEN q.dificuldade = 'FACIL' THEN 1 ELSE 0 END) as percFacil " +
            "FROM QuestaoEntity q " +
            "WHERE LOWER(q.banca) = LOWER(:banca) AND q.ativo = true " +
            "GROUP BY q.materia.id, q.topico.id")
    List<Object[]> findEstatisticasQuestoesByBanca(@Param("banca") String banca);

    @Query("SELECT q.ativo FROM QuestaoEntity q WHERE q.id = :id")
    Boolean isQuestaoAtiva(@Param("id") Long id);

    @Query("SELECT COUNT(q) > 0 FROM QuestaoEntity q " +
            "WHERE q.id = :questaoId AND q.materia.id = :materiaId")
    boolean existsByIdAndMateriaId(@Param("questaoId") Long questaoId, @Param("materiaId") Long materiaId);

    @Query("SELECT COUNT(q) > 0 FROM QuestaoEntity q " +
            "WHERE q.id = :questaoId AND q.topico.id = :topicoId")
    boolean existsByIdAndTopicoId(@Param("questaoId") Long questaoId, @Param("topicoId") Long topicoId);

    @Query("SELECT q FROM QuestaoEntity q " +
            "WHERE q.materia.id = :materiaId " +
            "AND q.ativo = true " +
            "AND q.dificuldade = :dificuldade " +
            "ORDER BY RANDOM()")
    List<QuestaoEntity> findByMateriaIdAndDificuldadeRandom(
            @Param("materiaId") Long materiaId,
            @Param("dificuldade") DificuldadeQuestao dificuldade
    );
}
