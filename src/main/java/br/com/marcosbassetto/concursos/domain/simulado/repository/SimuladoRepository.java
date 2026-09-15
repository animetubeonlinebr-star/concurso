package br.com.marcosbassetto.concursos.domain.simulado.repository;

import br.com.marcosbassetto.concursos.domain.simulado.entity.SimuladoEntity;
import br.com.marcosbassetto.concursos.domain.simulado.domain.StatusSimulado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SimuladoRepository extends JpaRepository<SimuladoEntity, Long> {
    List<SimuladoEntity> findByUsuarioIdOrderByCriadoEmDesc(Long usuarioId);
    Optional<SimuladoEntity> findByIdAndUsuarioId(Long id, Long usuarioId);
    long countByUsuarioIdAndStatus(Long usuarioId, StatusSimulado status);
}