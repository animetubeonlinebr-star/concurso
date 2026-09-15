package br.com.marcosbassetto.concursos.domain.curso.entity;

import br.com.marcosbassetto.concursos.common.util.NomeNormalizer;
import br.com.marcosbassetto.concursos.domain.concurso.entity.ConcursoEntity;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "curso",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_curso_concurso_nome_norm",
                columnNames = {"concurso_id", "nome_normalizado"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class CursoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "concurso_id", nullable = false)
    private ConcursoEntity concurso;

    @Column(nullable = false, length = 255)
    private String nome;

    @Column(name = "nome_normalizado", nullable = false, length = 255)
    private String nomeNormalizado;

    @Column(length = 50)
    private String codigo;

    private Integer ordem;

    @CreatedDate
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @LastModifiedDate
    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    @PrePersist
    @PreUpdate
    private void sincronizarNomeNormalizado() {
        if (this.nome != null) {
            this.nomeNormalizado = NomeNormalizer.normalizar(this.nome);
        }
    }
}
