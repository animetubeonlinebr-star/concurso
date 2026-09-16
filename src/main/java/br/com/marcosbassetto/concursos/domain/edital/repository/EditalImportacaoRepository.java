package br.com.marcosbassetto.concursos.domain.edital.repository;

import br.com.marcosbassetto.concursos.domain.edital.entity.EditalImportacaoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EditalImportacaoRepository extends JpaRepository<EditalImportacaoEntity, Long> {

    /**
     * Chave de deduplicação: o mesmo arquivo (hash) não é importado duas
     * vezes pelo mesmo usuário. Usuários distintos não interferem entre si.
     */
    Optional<EditalImportacaoEntity> findByUsuario_IdAndHashSha256(Long usuarioId, String hashSha256);

    Optional<EditalImportacaoEntity> findByConcurso_Id(Long concursoId);

    boolean existsByUsuario_IdAndHashSha256(Long usuarioId, String hashSha256);
}