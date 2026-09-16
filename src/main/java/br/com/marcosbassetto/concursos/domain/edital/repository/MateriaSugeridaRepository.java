package br.com.marcosbassetto.concursos.domain.edital.repository;

import br.com.marcosbassetto.concursos.domain.edital.entity.MateriaSugeridaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MateriaSugeridaRepository extends JpaRepository<MateriaSugeridaEntity, Long> {

    List<MateriaSugeridaEntity> findByImportacao_IdOrderByOrdemAsc(Long importacaoId);

    void deleteByImportacao_Id(Long importacaoId);
}