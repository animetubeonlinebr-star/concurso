package br.com.marcosbassetto.concursos.domain.edital.entity;

import br.com.marcosbassetto.concursos.common.util.NomeNormalizer;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Tópico extraído do edital, ainda em revisão. Par de
 * {@link MateriaSugeridaEntity} no staging.
 */
@Entity
@Table(name = "topico_sugerido")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class TopicoSugeridoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "materia_sugerida_id", nullable = false)
    private MateriaSugeridaEntity materiaSugerida;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(name = "nome_normalizado", nullable = false, length = 255)
    private String nomeNormalizado;

    @Column(nullable = false)
    @Builder.Default
    private Integer ordem = 0;

    @Column(nullable = false)
    @Builder.Default
    private Boolean selecionado = true;

    @Column(name = "possivel_duplicidade", nullable = false)
    @Builder.Default
    private Boolean possivelDuplicidade = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "similar_a_id")
    private TopicoSugeridoEntity similarA;

    @CreatedDate
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @PrePersist
    @PreUpdate
    private void sincronizarNomeNormalizado() {
        if (this.nome != null) {
            this.nomeNormalizado = NomeNormalizer.normalizar(this.nome);
        }
    }

    public Long getMateriaSugeridaId() {
        return materiaSugerida != null ? materiaSugerida.getId() : null;
    }

    public Long getSimilarAId() {
        return similarA != null ? similarA.getId() : null;
    }
}