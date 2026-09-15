package br.com.marcosbassetto.concursos.domain.concurso.repository;

import br.com.marcosbassetto.concursos.common.enums.Status;
import br.com.marcosbassetto.concursos.domain.concurso.entity.ConcursoEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConcursoRepository extends JpaRepository<ConcursoEntity, Long> {

    List<ConcursoEntity> findByUsuario_Id(Long usuarioId);

    List<ConcursoEntity> findByUsuario_IdAndStatus(Long usuarioId, Status status);

    Optional<ConcursoEntity> findByIdAndUsuario_Id(Long id, Long usuarioId);

    Optional<ConcursoEntity> findByIdAndUsuario_IdAndStatus(Long id, Long usuarioId, Status status);

    List<ConcursoEntity> findByUsuario_IdOrderByCriadoEmDesc(Long usuarioId);

    long countByUsuario_IdAndStatus(Long usuarioId, Status status);

    boolean existsByIdAndUsuario_Id(Long id, Long usuarioId);

    @Query("SELECT c FROM ConcursoEntity c WHERE c.usuario.id = :usuarioId AND c.nome = :nome")
    Optional<ConcursoEntity> findByUsuarioIdAndNome(@Param("usuarioId") Long usuarioId, @Param("nome") String nome);

    @Query("SELECT c FROM ConcursoEntity c " +
            "WHERE c.usuario.id = :usuarioId " +
            "AND LOWER(c.nome) LIKE LOWER(CONCAT('%', :nome, '%'))")
    List<ConcursoEntity> findByUsuarioIdAndNomeContainingIgnoreCase(
            @Param("usuarioId") Long usuarioId,
            @Param("nome") String nome
    );

    @Query("SELECT c FROM ConcursoEntity c WHERE c.usuario.id = :usuarioId")
    List<ConcursoEntity> findByUsuarioId(@Param("usuarioId") Long usuarioId, Pageable pageable);
}
