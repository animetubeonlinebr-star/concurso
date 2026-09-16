package br.com.marcosbassetto.concursos.domain.topico.repository;

import br.com.marcosbassetto.concursos.domain.topico.entity.TopicoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface TopicoRepository extends JpaRepository<TopicoEntity, Long> {

    /**
     * Tópicos da matéria, restritos ao dono. O tópico não guarda usuário
     * direto: o vínculo é topico -> materia -> curso -> concurso.
     */
    @Query("SELECT t FROM TopicoEntity t " +
            "JOIN t.materia m " +
            "JOIN m.curso c " +
            "JOIN c.concurso co " +
            "WHERE m.id = :materiaId AND co.usuario.id = :usuarioId " +
            "ORDER BY t.ordem ASC")
    List<TopicoEntity> findByMateriaIdAndUsuarioId(
            @Param("materiaId") Long materiaId,
            @Param("usuarioId") Long usuarioId);

    /**
     * Nomes normalizados dos tópicos já persistidos no concurso. Usado
     * para sinalizar sugestões do edital que colidem com o conteúdo real.
     */
    @Query("SELECT t.nomeNormalizado FROM TopicoEntity t " +
            "JOIN t.materia m " +
            "JOIN m.curso c " +
            "WHERE c.concurso.id = :concursoId")
    List<String> findNomeNormalizadoByConcursoId(@Param("concursoId") Long concursoId);
}