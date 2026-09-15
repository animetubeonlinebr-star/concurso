package br.com.marcosbassetto.concursos.domain.usuario.dto;

import br.com.marcosbassetto.concursos.domain.usuario.domain.PerfilUsuario;
import br.com.marcosbassetto.concursos.domain.usuario.entity.UsuarioEntity;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;


@Data
@Builder
public class UsuarioDTO {

    private Long id;

    @NotBlank(message = "Nome é obrigatório")
    @Size(min = 3, max = 100, message = "Nome deve ter entre 3 e 100 caracteres")
    private String nome;

    @NotBlank(message = "E-mail é obrigatório")
    @Email(message = "E-mail inválido")
    private String email;

    @Size(min = 6, max = 50, message = "Senha deve ter entre 6 e 50 caracteres")
    private String senha;

    private String confirmacaoSenha;

    private String senhaAtual;
    private String novaSenha;

    private PerfilUsuario perfil;
    private Boolean ativo;

    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;


    public UsuarioDTO() {}

    public UsuarioDTO(Long id, String nome, String email, String senha, String confirmacaoSenha,
                      String senhaAtual, String novaSenha, PerfilUsuario perfil, Boolean ativo,
                      LocalDateTime criadoEm, LocalDateTime atualizadoEm) {
        this.id = id;
        this.nome = nome;
        this.email = email;
        this.senha = senha;
        this.confirmacaoSenha = confirmacaoSenha;
        this.senhaAtual = senhaAtual;
        this.novaSenha = novaSenha;
        this.perfil = perfil;
        this.ativo = ativo;
        this.criadoEm = criadoEm;
        this.atualizadoEm = atualizadoEm;
    }


    public boolean senhasCoincidem() {
        if (senha == null || confirmacaoSenha == null) {
            return false;
        }
        return senha.equals(confirmacaoSenha);
    }


    public boolean isNovaSenhaValida() {
        if (novaSenha == null) {
            return true;
        }
        if (novaSenha.length() < 6) {
            return false;
        }
        if (senhaAtual == null || senhaAtual.isBlank()) {
            return false;
        }
        return true;
    }


    public boolean hasChanges() {
        return nome != null || email != null || novaSenha != null ||
                perfil != null || ativo != null;
    }


    public static UsuarioDTO fromEntity(UsuarioEntity entity) {
        if (entity == null) return null;

        return UsuarioDTO.builder()
                .id(entity.getId())
                .nome(entity.getNome())
                .email(entity.getEmail())
                .perfil(entity.getPerfil())
                .ativo(entity.isAtivo())
                .criadoEm(entity.getCriadoEm())
                .atualizadoEm(entity.getAtualizadoEm())
                .build();
    }


    public static List<UsuarioDTO> fromEntityList(List<UsuarioEntity> entities) {
        if (entities == null) return List.of();
        return entities.stream()
                .map(UsuarioDTO::fromEntity)
                .collect(Collectors.toList());
    }


    public static Page<UsuarioDTO> fromEntityPage(Page<UsuarioEntity> page) {
        return page.map(UsuarioDTO::fromEntity);
    }


    public UsuarioEntity toEntity() {
        UsuarioEntity entity = new UsuarioEntity();
        entity.setNome(this.nome);
        entity.setEmail(this.email);
        entity.setPerfil(this.perfil != null ? this.perfil : PerfilUsuario.USER);
        entity.setAtivo(this.ativo != null ? this.ativo : true);
        return entity;
    }


    public void updateEntity(UsuarioEntity entity) {
        if (this.nome != null && !this.nome.isBlank()) {
            entity.setNome(this.nome);
        }
        if (this.email != null && !this.email.isBlank()) {
            entity.setEmail(this.email);
        }
        if (this.perfil != null) {
            entity.setPerfil(this.perfil);
        }
        if (this.ativo != null) {
            entity.setAtivo(this.ativo);
        }
    }


    public static UsuarioDTO forRegister(String nome, String email, String senha, String confirmacaoSenha) {
        return UsuarioDTO.builder()
                .nome(nome)
                .email(email)
                .senha(senha)
                .confirmacaoSenha(confirmacaoSenha)
                .perfil(PerfilUsuario.USER)
                .ativo(true)
                .build();
    }

    public static UsuarioDTO forAdminCreate(String nome, String email, String senha, PerfilUsuario perfil) {
        return UsuarioDTO.builder()
                .nome(nome)
                .email(email)
                .senha(senha)
                .perfil(perfil != null ? perfil : PerfilUsuario.USER)
                .ativo(true)
                .build();
    }


    public static UsuarioDTO forPasswordUpdate(Long id, String senhaAtual, String novaSenha) {
        return UsuarioDTO.builder()
                .id(id)
                .senhaAtual(senhaAtual)
                .novaSenha(novaSenha)
                .build();
    }


    public static UsuarioDTO forStatusUpdate(Long id, boolean ativo) {
        return UsuarioDTO.builder()
                .id(id)
                .ativo(ativo)
                .build();
    }
}