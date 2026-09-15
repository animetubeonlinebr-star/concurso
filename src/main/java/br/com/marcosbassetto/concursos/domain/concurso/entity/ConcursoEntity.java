package br.com.marcosbassetto.concursos.domain.concurso.entity;

import br.com.marcosbassetto.concursos.common.enums.Status;
import br.com.marcosbassetto.concursos.domain.usuario.entity.UsuarioEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "concurso")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class ConcursoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "O usuário é obrigatório")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private UsuarioEntity usuario;

    @NotBlank(message = "O nome do concurso é obrigatório")
    @Size(max = 200, message = "O nome deve ter no máximo 200 caracteres")
    @Column(nullable = false, length = 200)
    private String nome;

    @Size(max = 200)
    private String orgao;

    @Size(max = 100)
    private String cargo;

    @Size(max = 100)
    private String banca;

    private Integer ano;

    @Size(max = 500)
    private String descricao;

    @NotNull(message = "O status é obrigatório")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.ATIVO;

    @CreatedDate
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @LastModifiedDate
    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String textoExtraido;

    @Column(columnDefinition = "boolean default false")
    @Builder.Default
    private Boolean processado = false;

    public Long getUsuarioId() {
        return usuario != null ? usuario.getId() : null;
    }

    public boolean isAtivo() {
        return Status.ATIVO.equals(this.status);
    }

    public void ativar() {
        this.status = Status.ATIVO;
    }

    public void inativar() {
        this.status = Status.INATIVO;
    }

    public void excluir() {
        this.status = Status.EXCLUIDO;
    }
}
