package br.com.marcosbassetto.concursos.domain.topico.entity;

import br.com.marcosbassetto.concursos.common.util.NomeNormalizer;
import br.com.marcosbassetto.concursos.domain.materia.entity.MateriaEntity;
import br.com.marcosbassetto.concursos.domain.questao.entity.QuestaoEntity;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "topico",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_topico_materia_nome_norm",
                columnNames = {"materia_id", "nome_normalizado"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class TopicoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "materia_id", nullable = false)
    private MateriaEntity materia;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(name = "nome_normalizado", nullable = false, length = 255)
    private String nomeNormalizado;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    @Column(nullable = false)
    @Builder.Default
    private Integer ordem = 0;

    @Column(nullable = false)
    @Builder.Default
    private Boolean ativo = true;

    @CreatedDate
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @LastModifiedDate
    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    @OneToMany(mappedBy = "topico")
    @Builder.Default
    private List<QuestaoEntity> questoes = new ArrayList<>();

    @PrePersist
    @PreUpdate
    private void sincronizarNomeNormalizado() {
        if (this.nome != null) {
            this.nomeNormalizado = NomeNormalizer.normalizar(this.nome);
        }
    }

    public void adicionarQuestao(QuestaoEntity questao) {
        questoes.add(questao);
        questao.setTopico(this);
    }

    public void removerQuestao(QuestaoEntity questao) {
        questoes.remove(questao);
        questao.setTopico(null);
    }

    public boolean isAtivo() {
        return ativo != null && ativo;
    }

    public void ativar() {
        this.ativo = true;
    }

    public void desativar() {
        this.ativo = false;
    }

    public void atualizar(String nome, String descricao, Integer ordem) {
        if (nome != null && !nome.isBlank()) {
            this.nome = nome;
        }
        if (descricao != null) {
            this.descricao = descricao;
        }
        if (ordem != null) {
            this.ordem = ordem;
        }
    }

    public boolean possuiQuestoes() {
        return questoes != null && !questoes.isEmpty();
    }

    public int getQuantidadeQuestoes() {
        return questoes != null ? questoes.size() : 0;
    }

    public Long getMateriaId() {
        return materia != null ? materia.getId() : null;
    }

    /**
     * Retorna o ID do CURSO (pai direto da matéria na hierarquia
     * Concurso → Curso → Materia → Topico).
     *
     * ⚠️ ANTES era getConcursoId() — renomeado porque o pai direto
     *    subiu um nível. Cuidado com LazyInitializationException
     *    se chamado fora de @Transactional.
     */
    public Long getCursoId() {
        return materia != null ? materia.getCursoId() : null;
    }

    public static class TopicoEntityBuilder {
        public TopicoEntityBuilder materiaId(Long materiaId) {
            if (materiaId != null) {
                MateriaEntity materia = new MateriaEntity();
                materia.setId(materiaId);
                this.materia(materia);
            }
            return this;
        }
    }
}
