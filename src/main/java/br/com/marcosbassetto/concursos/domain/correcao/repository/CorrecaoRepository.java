package br.com.marcosbassetto.concursos.domain.correcao.repository;

import br.com.marcosbassetto.concursos.domain.correcao.domain.ResultadoCorrecao;
import br.com.marcosbassetto.concursos.domain.correcao.entity.CorrecaoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CorrecaoRepository extends JpaRepository<CorrecaoEntity, Long> {

    // Ver RespostaRepository: o underscore evita colidir com o getter
    // derivado getSimuladoQuestaoId() da entidade.
    Optional<CorrecaoEntity> findBySimuladoQuestao_Id(Long simuladoQuestaoId);

    boolean existsBySimuladoQuestao_Id(Long simuladoQuestaoId);

    @Query("SELECT c FROM CorrecaoEntity c " +
            "WHERE c.simulado.id = :simuladoId " +
            "ORDER BY c.simuladoQuestao.ordem ASC")
    List<CorrecaoEntity> findBySimuladoId(@Param("simuladoId") Long simuladoId);

    @Query("SELECT c FROM CorrecaoEntity c " +
            "WHERE c.simulado.usuarioId = :usuarioId")
    List<CorrecaoEntity> findByUsuarioId(@Param("usuarioId") Long usuarioId);

    @Query("SELECT COUNT(c) FROM CorrecaoEntity c " +
            "WHERE c.simulado.id = :simuladoId AND c.resultado = :resultado")
    long countBySimuladoIdAndResultado(
            @Param("simuladoId") Long simuladoId,
            @Param("resultado") ResultadoCorrecao resultado);

    @Query("SELECT COUNT(c) > 0 FROM CorrecaoEntity c " +
            "WHERE c.id = :id AND c.simulado.usuarioId = :usuarioId")
    boolean existsByIdAndUsuarioId(
            @Param("id") Long id,
            @Param("usuarioId") Long usuarioId);

    @Query("SELECT c FROM CorrecaoEntity c " +
            "WHERE c.id = :id AND c.simulado.usuarioId = :usuarioId")
    Optional<CorrecaoEntity> findByIdAndUsuarioId(
            @Param("id") Long id,
            @Param("usuarioId") Long usuarioId);
}