package br.com.marcosbassetto.concursos.domain.simulado.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "simulado_questao",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"simulado_id", "questao_id"}),
                @UniqueConstraint(columnNames = {"simulado_id", "ordem"})
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SimuladoQuestaoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "simulado_id", nullable = false)
    private SimuladoEntity simulado;

    @Column(name = "questao_id", nullable = false)
    private Long questaoId;

    @Column(nullable = false)
    private Integer ordem;

    private BigDecimal valor;

    private LocalDateTime criadoEm;

    @PrePersist
    public void prePersist() {
        this.criadoEm = LocalDateTime.now();
        if (this.valor == null) {
            this.valor = BigDecimal.ONE;
        }
    }
}
