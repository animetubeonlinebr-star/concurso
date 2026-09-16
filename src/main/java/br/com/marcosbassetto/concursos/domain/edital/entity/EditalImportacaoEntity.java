package br.com.marcosbassetto.concursos.domain.edital.entity;

import br.com.marcosbassetto.concursos.domain.concurso.entity.ConcursoEntity;
import br.com.marcosbassetto.concursos.domain.edital.domain.StatusProcessamento;
import br.com.marcosbassetto.concursos.domain.usuario.entity.UsuarioEntity;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Registro de uma importação de edital.
 *
 * A chave (usuario_id, hash_sha256) é única: o mesmo PDF não é processado
 * duas vezes pelo mesmo usuário. O texto extraído do PDF continua em
 * {@code ConcursoEntity.textoExtraido}, que é reutilizado no reprocessamento.
 */
@Entity
@Table(
        name = "edital_importacao",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_importacao_usuario_hash",
                columnNames = {"usuario_id", "hash_sha256"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class EditalImportacaoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "concurso_id", nullable = false)
    private ConcursoEntity concurso;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private UsuarioEntity usuario;

    @Column(name = "nome_arquivo", nullable = false, length = 255)
    private String nomeArquivo;

    @Column(name = "hash_sha256", nullable = false, length = 64)
    private String hashSha256;

    @Column(name = "tamanho_bytes")
    private Long tamanhoBytes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private StatusProcessamento status = StatusProcessamento.RECEBIDO;

    @Column(name = "mensagem_erro", columnDefinition = "TEXT")
    private String mensagemErro;

    @Column(name = "extraido_em")
    private LocalDateTime extraidoEm;

    @Column(name = "confirmado_em")
    private LocalDateTime confirmadoEm;

    @CreatedDate
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @LastModifiedDate
    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    public Long getConcursoId() {
        return concurso != null ? concurso.getId() : null;
    }

    public Long getUsuarioId() {
        return usuario != null ? usuario.getId() : null;
    }

    public boolean aguardandoRevisao() {
        return StatusProcessamento.AGUARDANDO_REVISAO.equals(this.status);
    }

    public boolean emProcessamento() {
        return StatusProcessamento.PROCESSANDO.equals(this.status);
    }
}