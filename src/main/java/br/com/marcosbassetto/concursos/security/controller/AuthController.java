package br.com.marcosbassetto.concursos.security.controller;

import br.com.marcosbassetto.concursos.common.exception.BusinessException;
import br.com.marcosbassetto.concursos.common.exception.ErrorCodes;
import br.com.marcosbassetto.concursos.domain.usuario.dto.UsuarioDTO;
import br.com.marcosbassetto.concursos.domain.usuario.entity.UsuarioEntity;
import br.com.marcosbassetto.concursos.domain.usuario.repository.UsuarioRepository;
import br.com.marcosbassetto.concursos.domain.usuario.service.UsuarioService;
import br.com.marcosbassetto.concursos.security.dto.LoginRequest;
import br.com.marcosbassetto.concursos.security.dto.LoginResponse;
import br.com.marcosbassetto.concursos.security.service.JwtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final UsuarioService usuarioService; // ← ADICIONE ESTA LINHA


    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        log.debug("Tentativa de login para email: {}", request.email());

        UsuarioEntity usuario = usuarioRepository.findByEmail(request.email())
                .orElseThrow(() -> {
                    log.warn("Login falhou - usuário não encontrado: {}", request.email());
                    return new BusinessException(
                            ErrorCodes.INVALID_CREDENTIALS,
                            "Usuário ou senha inválidos."
                    );
                });

        if (!passwordEncoder.matches(request.senha(), usuario.getSenhaHash())) {
            log.warn("Login falhou - senha inválida para: {}", request.email());
            throw new BusinessException(
                    ErrorCodes.INVALID_CREDENTIALS,
                    "Usuário ou senha inválidos."
            );
        }

        if (!usuario.isAtivo()) {
            log.warn("Login falhou - usuário inativo: {}", request.email());
            throw new BusinessException(
                    ErrorCodes.ESTADO_INVALIDO,
                    "Usuário inativo. Entre em contato com o suporte."
            );
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.email());

        String token = jwtService.generateToken(userDetails);

        log.info("Login bem-sucedido para: {}", request.email());

        return new LoginResponse(
                token,
                "Bearer",
                usuario.getNome(),
                jwtService.getExpiration()
        );

    }


    @PostMapping("/register")
    public ResponseEntity<UsuarioDTO> register(@Valid @RequestBody UsuarioDTO dto) {
        log.info("Registrando novo usuário: {}", dto.getEmail());
        UsuarioDTO novoUsuario = usuarioService.criar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(novoUsuario);
    }
}