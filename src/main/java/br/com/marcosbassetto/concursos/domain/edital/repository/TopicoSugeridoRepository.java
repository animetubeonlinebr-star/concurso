package br.com.marcosbassetto.concursos.domain.edital.repository;

import br.com.marcosbassetto.concursos.domain.edital.entity.TopicoSugeridoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TopicoSugeridoRepository extends JpaRepository<TopicoSugeridoEntity, Long> {

    List<TopicoSugeridoEntity> findByMateriaSugerida_IdOrderByOrdemAsc(Long materiaSugeridaId);

    void deleteByMateriaSugerida_Id(Long materiaSugeridaId);
}