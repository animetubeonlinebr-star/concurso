package br.com.marcosbassetto.concursos.domain.topico.repository;

import br.com.marcosbassetto.concursos.domain.topico.entity.TopicoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface TopicoRepository extends JpaRepository<TopicoEntity, Long> {
    //TODO - adicionar métodos específicos depois

}