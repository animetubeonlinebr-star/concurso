package br.com.marcosbassetto.concursos.domain.correcao.entity;

import br.com.marcosbassetto.concursos.domain.correcao.domain.ResultadoCorrecao;
import br.com.marcosbassetto.concursos.domain.simulado.entity.SimuladoEntity;
import br.com.marcosbassetto.concursos.domain.simulado.entity.SimuladoQuestaoEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Resultado objetivo da correção de uma questão de simulado.
 *
 * A restrição de unicidade em {@code simulado_questao_id} garante 0 ou 1
 * correção por questão — a idempotência da correção depende disso, além da
 * checagem em service.
 *
 * {@code respostaCorreta} é copiada do gabarito no momento da correção para que
 * a auditoria permaneça estável mesmo se o gabarito for alterado depois.
 */
@Entity
@Table(
        name = "correcao",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_correcao_simulado_questao",
                columnNames = "simulado_questao_id"
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CorrecaoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "simulado_id", nullable = false)
    private SimuladoEntity simulado;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "simulado_questao_id", nullable = false, unique = true)
    private SimuladoQuestaoEntity simuladoQuestao;

    @Column(name = "resposta_usuario", length = 20000)
    private String respostaUsuario;

    @Column(name = "resposta_correta", nullable = false, length = 10)
    private String respostaCorreta;

    @Enumerated(EnumType.STRING)
    @Column(name = "resultado", nullable = false, length = 20)
    private ResultadoCorrecao resultado;

    @Column(name = "corrigida_em", nullable = false)
    private LocalDateTime corrigidaEm;

    public Long getSimuladoId() {
        return simulado != null ? simulado.getId() : null;
    }

    public Long getSimuladoQuestaoId() {
        return simuladoQuestao != null ? simuladoQuestao.getId() : null;
    }

    public boolean isCorreta() {
        return ResultadoCorrecao.CORRETA.equals(this.resultado);
    }

    /**
     * Reaplica a correção sobre um registro existente, preservando o id e a
     * unicidade por questão. Usada pelo recálculo e pela idempotência.
     */
    public void reclassificar(String respostaUsuario, String respostaCorreta,
                              ResultadoCorrecao resultado) {
        this.respostaUsuario = respostaUsuario;
        this.respostaCorreta = respostaCorreta;
        this.resultado = resultado;
        this.corrigidaEm = LocalDateTime.now();
    }
}