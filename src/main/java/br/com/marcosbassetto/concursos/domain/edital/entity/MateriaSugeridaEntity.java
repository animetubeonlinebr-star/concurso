package br.com.marcosbassetto.concursos.domain.edital.entity;

import br.com.marcosbassetto.concursos.common.util.NomeNormalizer;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Matéria extraída do edital, ainda em revisão.
 *
 * Existe separada de {@code MateriaEntity} porque uma sugestão não pode
 * vazar para simulado, desempenho ou navegação antes de confirmada.
 */
@Entity
@Table(name = "materia_sugerida")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class MateriaSugeridaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "importacao_id", nullable = false)
    private EditalImportacaoEntity importacao;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(name = "nome_normalizado", nullable = false, length = 255)
    private String nomeNormalizado;

    @Column(nullable = false)
    @Builder.Default
    private Integer ordem = 0;

    @Column(nullable = false)
    @Builder.Default
    private Boolean selecionada = true;

    @Column(name = "possivel_duplicidade", nullable = false)
    @Builder.Default
    private Boolean possivelDuplicidade = false;

    /**
     * Sugestão com que esta se parece. Apenas sinalização: nenhuma mesclagem
     * acontece sem ação explícita do usuário.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "similar_a_id")
    private MateriaSugeridaEntity similarA;

    @CreatedDate
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @OneToMany(mappedBy = "materiaSugerida", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TopicoSugeridoEntity> topicos = new ArrayList<>();

    @PrePersist
    @PreUpdate
    private void sincronizarNomeNormalizado() {
        if (this.nome != null) {
            this.nomeNormalizado = NomeNormalizer.normalizar(this.nome);
        }
    }

    public Long getSimilarAId() {
        return similarA != null ? similarA.getId() : null;
    }

    public void adicionarTopico(TopicoSugeridoEntity topico) {
        topicos.add(topico);
        topico.setMateriaSugerida(this);
    }
}