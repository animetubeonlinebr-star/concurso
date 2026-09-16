package br.com.marcosbassetto.concursos.domain.resposta.entity;

import br.com.marcosbassetto.concursos.domain.simulado.entity.SimuladoQuestaoEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Resposta do usuário para uma questão de um simulado.
 *
 * A entidade registra somente o que o usuário respondeu. Determinar se a
 * resposta está correta é responsabilidade do módulo Correcao.
 */
@Entity
@Table(
        name = "resposta",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_resposta_simulado_questao",
                columnNames = "simulado_questao_id"
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RespostaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "simulado_questao_id", nullable = false, unique = true)
    private SimuladoQuestaoEntity simuladoQuestao;

    @Column(name = "alternativa_selecionada", length = 10)
    private String alternativaSelecionada;

    @Column(name = "resposta_texto", columnDefinition = "TEXT")
    private String respostaTexto;

    @Column(name = "respondida_em", nullable = false)
    private LocalDateTime respondidaEm;

    @Column(name = "atualizada_em", nullable = false)
    private LocalDateTime atualizadaEm;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    public Long getSimuladoQuestaoId() {
        return simuladoQuestao != null ? simuladoQuestao.getId() : null;
    }

    public boolean isEmBranco() {
        return (alternativaSelecionada == null || alternativaSelecionada.isBlank())
                && (respostaTexto == null || respostaTexto.isBlank());
    }

    public void alterar(String alternativaSelecionada, String respostaTexto) {
        this.alternativaSelecionada = alternativaSelecionada;
        this.respostaTexto = respostaTexto;
        this.atualizadaEm = LocalDateTime.now();
    }

    @PrePersist
    public void prePersist() {
        LocalDateTime agora = LocalDateTime.now();
        if (this.respondidaEm == null) {
            this.respondidaEm = agora;
        }
        this.atualizadaEm = agora;
        this.criadoEm = agora;
    }

    @PreUpdate
    public void preUpdate() {
        this.atualizadaEm = LocalDateTime.now();
    }
}