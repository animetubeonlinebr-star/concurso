package br.com.marcosbassetto.concursos.domain.concurso.entity;

import br.com.marcosbassetto.concursos.common.enums.Status;
import br.com.marcosbassetto.concursos.domain.edital.domain.StatusProcessamento;
import br.com.marcosbassetto.concursos.domain.edital.entity.EditalImportacaoEntity;
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

    // Sem @Lob: a coluna é `text`, não um large object. Com @Lob o Hibernate
    // grava/leria um OID, e a leitura ainda estoura fora de transação
    // (open-in-view=false) — o texto do edital vira inacessível.
    @Column(columnDefinition = "TEXT")
    private String textoExtraido;

    /**
     * @deprecated substituído por {@link #statusProcessamento}. Mantido
     * sincronizado com {@code CONFIRMADO} enquanto houver consumidores.
     */
    @Deprecated(since = "fluxo de importação de edital")
    @Column(columnDefinition = "boolean default false")
    @Builder.Default
    private Boolean processado = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_processamento", nullable = false, length = 30)
    @Builder.Default
    private StatusProcessamento statusProcessamento = StatusProcessamento.RECEBIDO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "importacao_id")
    private EditalImportacaoEntity importacao;

    public Long getUsuarioId() {
        return usuario != null ? usuario.getId() : null;
    }

    public Long getImportacaoId() {
        return importacao != null ? importacao.getId() : null;
    }

    /**
     * Atualiza os dois campos de controle juntos: {@code processado} é
     * derivado de {@code statusProcessamento}, nunca fonte independente.
     */
    public void definirStatusProcessamento(StatusProcessamento novoStatus) {
        this.statusProcessamento = novoStatus;
        this.processado = StatusProcessamento.CONFIRMADO.equals(novoStatus);
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
