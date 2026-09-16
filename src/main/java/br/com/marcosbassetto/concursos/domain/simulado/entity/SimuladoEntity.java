package br.com.marcosbassetto.concursos.domain.simulado.entity;

import br.com.marcosbassetto.concursos.domain.simulado.domain.StatusSimulado;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "simulado")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SimuladoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "concurso_id", nullable = false)
    private Long concursoId;

    @Column(name = "materia_id", nullable = false)
    private Long materiaId;

    private String titulo;
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusSimulado status;

    @Column(name = "quantidade_questoes")
    private Integer quantidadeQuestoes;

    private LocalDateTime iniciadoEm;
    private LocalDateTime finalizadoEm;
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;

    @OneToMany(mappedBy = "simulado", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SimuladoQuestaoEntity> questoes = new ArrayList<>();

    public void adicionarQuestao(SimuladoQuestaoEntity questao) {
        questoes.add(questao);
        questao.setSimulado(this);
    }

    public void removerQuestao(SimuladoQuestaoEntity questao) {
        questoes.remove(questao);
        questao.setSimulado(null);
    }

    public boolean isCriado() {
        return StatusSimulado.CRIADO.equals(this.status);
    }

    public boolean isEmAndamento() {
        return StatusSimulado.EM_ANDAMENTO.equals(this.status);
    }

    public boolean isFinalizado() {
        return StatusSimulado.FINALIZADO.equals(this.status);
    }

    public boolean isCancelado() {
        return StatusSimulado.CANCELADO.equals(this.status);
    }

    /**
     * Somente simulados em andamento aceitam criação ou alteração de respostas.
     */
    public boolean permiteResposta() {
        return isEmAndamento();
    }

    @Version
    @Column(nullable = false)
    @Builder.Default
    private Long versao = 0L;

    @PrePersist
    public void prePersist() {
        this.criadoEm = LocalDateTime.now();
        this.atualizadoEm = LocalDateTime.now();
        if (this.status == null) {
            this.status = StatusSimulado.CRIADO;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.atualizadoEm = LocalDateTime.now();
    }
}
