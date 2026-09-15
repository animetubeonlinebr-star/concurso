package br.com.marcosbassetto.concursos.domain.questao.entity;

import br.com.marcosbassetto.concursos.domain.materia.entity.MateriaEntity;
import br.com.marcosbassetto.concursos.domain.questao.domain.DificuldadeQuestao;
import br.com.marcosbassetto.concursos.domain.questao.domain.OrigemQuestao;
import br.com.marcosbassetto.concursos.domain.questao.domain.TipoQuestao;
import br.com.marcosbassetto.concursos.domain.topico.entity.TopicoEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "questao")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class QuestaoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "materia_id", nullable = false)
    private MateriaEntity materia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topico_id")
    private TopicoEntity topico;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String enunciado;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoQuestao tipo;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> alternativas;

    @Column(name = "resposta_correta", nullable = false, length = 10)
    private String respostaCorreta;

    @Column(columnDefinition = "TEXT")
    private String justificativa;

    @Column(length = 100)
    private String banca;

    private Integer ano;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private DificuldadeQuestao dificuldade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private OrigemQuestao origem = OrigemQuestao.USUARIO;

    @Column(length = 255)
    private String referencia;

    @Column(nullable = false)
    @Builder.Default
    private Boolean ativo = true;

    @CreatedDate
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @LastModifiedDate
    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;


    public boolean isAtivo() {
        return ativo != null && ativo;
    }

    public void ativar() {
        this.ativo = true;
    }

    public void desativar() {
        this.ativo = false;
    }

    public boolean isMultiplaEscolha() {
        return TipoQuestao.MULTIPLA_ESCOLHA.equals(this.tipo);
    }

    public boolean isCertoErrado() {
        return TipoQuestao.CERTO_ERRADO.equals(this.tipo);
    }

    public boolean isDiscursiva() {
        return TipoQuestao.DISCURSIVA.equals(this.tipo);
    }

    public boolean isGeradaPorIA() {
        return OrigemQuestao.IA.equals(this.origem);
    }

    public boolean isQuestaoDeBanca() {
        return OrigemQuestao.BANCA.equals(this.origem);
    }

    public Long getMateriaId() {
        return materia != null ? materia.getId() : null;
    }

    public Long getTopicoId() {
        return topico != null ? topico.getId() : null;
    }

    public Long getConcursoId() {
        return materia != null ? materia.getCursoId() : null;
    }

    public String getMateriaNome() {
        return materia != null ? materia.getNome() : null;
    }

    public String getTopicoNome() {
        return topico != null ? topico.getNome() : null;
    }

    public void atualizar(String enunciado, List<String> alternativas, String respostaCorreta,
                          String justificativa, DificuldadeQuestao dificuldade,
                          String referencia, Boolean ativo) {
        if (enunciado != null && !enunciado.isBlank()) {
            this.enunciado = enunciado;
        }
        if (alternativas != null) {
            this.alternativas = alternativas;
        }
        if (respostaCorreta != null && !respostaCorreta.isBlank()) {
            this.respostaCorreta = respostaCorreta;
        }
        if (justificativa != null) {
            this.justificativa = justificativa;
        }
        if (dificuldade != null) {
            this.dificuldade = dificuldade;
        }
        if (referencia != null) {
            this.referencia = referencia;
        }
        if (ativo != null) {
            this.ativo = ativo;
        }
    }

    public boolean validarResposta(String resposta) {
        if (resposta == null || resposta.isBlank()) {
            return false;
        }

        if (isMultiplaEscolha()) {
            return resposta.matches("^[A-E]$");
        }

        if (isCertoErrado()) {
            return "CERTO".equalsIgnoreCase(resposta) || "ERRADO".equalsIgnoreCase(resposta);
        }

        return true;
    }

    public boolean verificarResposta(String respostaUsuario) {
        if (respostaUsuario == null || respostaUsuario.isBlank()) {
            return false;
        }

        if (isCertoErrado()) {
            return respostaUsuario.equalsIgnoreCase(this.respostaCorreta);
        }

        return respostaUsuario.equalsIgnoreCase(this.respostaCorreta);
    }

    public boolean validarAlternativas() {
        if (!isMultiplaEscolha()) {
            return true; // Não precisa de alternativas
        }

        return alternativas != null && !alternativas.isEmpty();
    }

    public static class QuestaoEntityBuilder {
        public QuestaoEntityBuilder materiaId(Long materiaId) {
            if (materiaId != null) {
                MateriaEntity materia = new MateriaEntity();
                materia.setId(materiaId);
                this.materia(materia);
            }
            return this;
        }

        public QuestaoEntityBuilder topicoId(Long topicoId) {
            if (topicoId != null) {
                TopicoEntity topico = new TopicoEntity();
                topico.setId(topicoId);
                this.topico(topico);
            }
            return this;
        }
    }
}
