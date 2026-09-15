package br.com.marcosbassetto.concursos.api.controller;

import br.com.marcosbassetto.concursos.common.enums.Status;
import br.com.marcosbassetto.concursos.domain.concurso.dto.ConcursoResponse;
import br.com.marcosbassetto.concursos.domain.concurso.service.ConcursoService;
import br.com.marcosbassetto.concursos.domain.usuario.entity.UsuarioEntity;
import br.com.marcosbassetto.concursos.domain.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConcursoControllerTest {

    @Mock
    private ConcursoService concursoService;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private ConcursoController controller;

    private ConcursoResponse concursoResponse;
    private UsuarioEntity usuario;

    @BeforeEach
    void setUp() {
        usuario = UsuarioEntity.builder()
                .id(3L)
                .email("usuario@email.com")
                .senhaHash("hash")
                .build();

        concursoResponse = new ConcursoResponse(
                1L,                         // id
                3L,                         // usuarioId
                "Polícia Federal",          // nome
                "Polícia Federal",          // orgao
                "Agente",                   // cargo
                "CEBRASPE",                 // banca
                2026,                       // ano
                null,                       // descricao
                Status.ATIVO,               // status
                true,                       // processado
                LocalDateTime.now(),        // criadoEm
                LocalDateTime.now()         // atualizadoEm
        );
    }

    @Test
    void deveListarConcursosDoUsuario() {
        when(authentication.getName()).thenReturn("usuario@email.com");
        when(usuarioRepository.findByEmail(anyString())).thenReturn(Optional.of(usuario));
        when(concursoService.listarPorUsuario(anyLong())).thenReturn(List.of(concursoResponse));

        List<ConcursoResponse> resultado = controller.listar(authentication);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).nome()).isEqualTo("Polícia Federal");
        assertThat(resultado.get(0).banca()).isEqualTo("CEBRASPE");
        assertThat(resultado.get(0).usuarioId()).isEqualTo(3L);
        assertThat(resultado.get(0).processado()).isTrue();
    }

    @Test
    void deveBuscarConcursoPorId() {
        when(authentication.getName()).thenReturn("usuario@email.com");
        when(usuarioRepository.findByEmail(anyString())).thenReturn(Optional.of(usuario));
        when(concursoService.buscarPorId(1L, 3L)).thenReturn(concursoResponse);

        ConcursoResponse resultado = controller.buscar(1L, authentication);

        assertThat(resultado).isNotNull();
        assertThat(resultado.id()).isEqualTo(1L);
        assertThat(resultado.nome()).isEqualTo("Polícia Federal");
    }
}
