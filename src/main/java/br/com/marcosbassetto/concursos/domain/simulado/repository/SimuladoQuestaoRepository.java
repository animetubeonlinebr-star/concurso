package br.com.marcosbassetto.concursos.domain.simulado.repository;

import br.com.marcosbassetto.concursos.domain.simulado.entity.SimuladoQuestaoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SimuladoQuestaoRepository extends JpaRepository<SimuladoQuestaoEntity, Long> {
    List<SimuladoQuestaoEntity> findBySimuladoIdOrderByOrdemAsc(Long simuladoId);
    boolean existsBySimuladoIdAndQuestaoId(Long simuladoId, Long questaoId);
}