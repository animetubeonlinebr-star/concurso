package br.com.marcosbassetto.concursos.domain.resposta.repository;

import br.com.marcosbassetto.concursos.domain.resposta.entity.RespostaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RespostaRepository extends JpaRepository<RespostaEntity, Long> {

    // O underscore força a resolução pela associação: sem ele o Spring Data
    // tentaria a propriedade "simuladoQuestaoId", que não existe e só é
    // exposta por um getter derivado da entidade.
    Optional<RespostaEntity> findBySimuladoQuestao_Id(Long simuladoQuestaoId);

    boolean existsBySimuladoQuestao_Id(Long simuladoQuestaoId);

    @Query("SELECT r FROM RespostaEntity r " +
            "WHERE r.simuladoQuestao.simulado.id = :simuladoId " +
            "ORDER BY r.simuladoQuestao.ordem ASC")
    List<RespostaEntity> findBySimuladoId(@Param("simuladoId") Long simuladoId);

    @Query("SELECT r FROM RespostaEntity r " +
            "WHERE r.id = :id " +
            "AND r.simuladoQuestao.simulado.usuarioId = :usuarioId")
    Optional<RespostaEntity> findByIdAndUsuarioId(
            @Param("id") Long id,
            @Param("usuarioId") Long usuarioId);

    @Query("SELECT r FROM RespostaEntity r " +
            "WHERE r.simuladoQuestao.id = :simuladoQuestaoId " +
            "AND r.simuladoQuestao.simulado.usuarioId = :usuarioId")
    Optional<RespostaEntity> findBySimuladoQuestaoIdAndUsuarioId(
            @Param("simuladoQuestaoId") Long simuladoQuestaoId,
            @Param("usuarioId") Long usuarioId);

    @Query("SELECT COUNT(r) FROM RespostaEntity r " +
            "WHERE r.simuladoQuestao.simulado.id = :simuladoId")
    long countBySimuladoId(@Param("simuladoId") Long simuladoId);
}