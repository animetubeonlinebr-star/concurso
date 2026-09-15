package br.com.marcosbassetto.concursos.domain.usuario.repository;

import br.com.marcosbassetto.concursos.domain.usuario.entity.UsuarioEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<UsuarioEntity, Long> {

    Optional<UsuarioEntity> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<UsuarioEntity> findByEmailAndAtivoTrue(String email);
}
