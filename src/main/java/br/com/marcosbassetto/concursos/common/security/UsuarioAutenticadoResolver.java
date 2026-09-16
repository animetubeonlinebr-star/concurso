package br.com.marcosbassetto.concursos.common.security;

import br.com.marcosbassetto.concursos.common.exception.ResourceNotFoundException;
import br.com.marcosbassetto.concursos.domain.usuario.entity.UsuarioEntity;
import br.com.marcosbassetto.concursos.domain.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Resolve o usuário autenticado a partir do {@link Authentication}.
 *
 * A identidade nunca vem do corpo da requisição: o frontend não pode informar
 * o usuarioId para agir em nome de outra pessoa.
 */
@Component
@RequiredArgsConstructor
public class UsuarioAutenticadoResolver {

    private final UsuarioRepository usuarioRepository;

    public Long resolverUsuarioId(Authentication auth) {
        if (auth == null || auth.getName() == null) {
            throw new ResourceNotFoundException("Usuário", "autenticação", "ausente");
        }

        String email = auth.getName();
        UsuarioEntity usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", "email", email));

        return usuario.getId();
    }
}