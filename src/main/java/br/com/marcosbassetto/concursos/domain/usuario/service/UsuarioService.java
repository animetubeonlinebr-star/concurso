package br.com.marcosbassetto.concursos.domain.usuario.service;

import br.com.marcosbassetto.concursos.common.exception.BusinessException;
import br.com.marcosbassetto.concursos.common.exception.ErrorCodes;
import br.com.marcosbassetto.concursos.domain.usuario.dto.UsuarioDTO;
import br.com.marcosbassetto.concursos.domain.usuario.dto.UsuarioListResponse;
import br.com.marcosbassetto.concursos.domain.usuario.entity.UsuarioEntity;
import br.com.marcosbassetto.concursos.domain.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UsuarioDTO criar(UsuarioDTO dto) {
        log.debug("Criando usuário: {}", dto.getEmail());

        if (usuarioRepository.existsByEmail(dto.getEmail())) {
            throw new BusinessException(
                    ErrorCodes.DADOS_INVALIDOS,
                    "E-mail já cadastrado."
            );
        }

        if (dto.getConfirmacaoSenha() != null && !dto.senhasCoincidem()) {
            throw new BusinessException(
                    ErrorCodes.DADOS_INVALIDOS,
                    "As senhas não coincidem."
            );
        }

        UsuarioEntity entity = dto.toEntity();
        entity.setSenhaHash(passwordEncoder.encode(dto.getSenha()));

        UsuarioEntity salvo = usuarioRepository.save(entity);
        log.info("Usuário criado: {} - ID: {}", salvo.getEmail(), salvo.getId());

        return UsuarioDTO.fromEntity(salvo);
    }


    @Transactional
    public UsuarioDTO atualizar(Long id, UsuarioDTO dto) {
        log.debug("Atualizando usuário ID: {}", id);

        UsuarioEntity entity = buscarEntityPorId(id);

        if (dto.getEmail() != null && !dto.getEmail().equals(entity.getEmail())) {
            if (usuarioRepository.existsByEmail(dto.getEmail())) {
                throw new BusinessException(
                        ErrorCodes.DADOS_INVALIDOS,
                        "E-mail já cadastrado."
                );
            }
        }

        if (dto.getNovaSenha() != null) {
            if (!dto.isNovaSenhaValida()) {
                throw new BusinessException(
                        ErrorCodes.DADOS_INVALIDOS,
                        "Senha atual incorreta ou nova senha inválida."
                );
            }
            if (dto.getSenhaAtual() == null ||
                    !passwordEncoder.matches(dto.getSenhaAtual(), entity.getSenhaHash())) {
                throw new BusinessException(
                        ErrorCodes.DADOS_INVALIDOS,
                        "Senha atual incorreta."
                );
            }
            entity.setSenhaHash(passwordEncoder.encode(dto.getNovaSenha()));
        }

        dto.updateEntity(entity);

        UsuarioEntity salvo = usuarioRepository.save(entity);
        log.info("Usuário atualizado: {} - ID: {}", salvo.getEmail(), id);

        return UsuarioDTO.fromEntity(salvo);
    }


    @Transactional(readOnly = true)
    public UsuarioDTO buscarPorId(Long id) {
        return UsuarioDTO.fromEntity(buscarEntityPorId(id));
    }


    @Transactional(readOnly = true)
    public UsuarioDTO buscarPorEmail(String email) {
        UsuarioEntity entity = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(
                        ErrorCodes.RESOURCE_NOT_FOUND,
                        "Usuário não encontrado."
                ));
        return UsuarioDTO.fromEntity(entity);
    }


    @Transactional(readOnly = true)
    public UsuarioListResponse listar(Pageable pageable) {
        Page<UsuarioEntity> page = usuarioRepository.findAll(pageable);
        return UsuarioListResponse.fromEntityPage(page);
    }


    @Transactional
    public UsuarioDTO atualizarStatus(Long id, boolean ativo) {
        UsuarioEntity entity = buscarEntityPorId(id);
        entity.setAtivo(ativo);
        UsuarioEntity salvo = usuarioRepository.save(entity);
        log.info("Usuário {} ID: {}", ativo ? "ativado" : "desativado", id);
        return UsuarioDTO.fromEntity(salvo);
    }


    @Transactional
    public void excluir(Long id) {
        UsuarioEntity entity = buscarEntityPorId(id);
        usuarioRepository.delete(entity);
        log.info("Usuário excluído: {} - ID: {}", entity.getEmail(), id);
    }

    private UsuarioEntity buscarEntityPorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        ErrorCodes.RESOURCE_NOT_FOUND,
                        "Usuário não encontrado."
                ));
    }
}

