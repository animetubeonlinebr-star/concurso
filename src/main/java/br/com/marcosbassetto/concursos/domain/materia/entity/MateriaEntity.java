package br.com.marcosbassetto.concursos.domain.materia.entity;

import br.com.marcosbassetto.concursos.common.enums.Status;
import br.com.marcosbassetto.concursos.common.util.NomeNormalizer;
import br.com.marcosbassetto.concursos.domain.curso.entity.CursoEntity;
import br.com.marcosbassetto.concursos.domain.materia.domain.OrigemMateria;
import br.com.marcosbassetto.concursos.domain.questao.entity.QuestaoEntity;
import br.com.marcosbassetto.concursos.domain.topico.entity.TopicoEntity;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "materia",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_materia_curso_nome_norm",
                columnNames = {"curso_id", "nome_normalizado"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class MateriaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "curso_id", nullable = false)
    private CursoEntity curso;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(name = "nome_normalizado", nullable = false, length = 255)
    private String nomeNormalizado;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    @Column(nullable = false)
    private Integer ordem;

    @Column(precision = 5, scale = 2)
    private BigDecimal peso;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.ATIVO;

    @Enumerated(EnumType.STRING)
    @Column(name = "origem", nullable = false, length = 20)
    @Builder.Default
    private OrigemMateria origem = OrigemMateria.USUARIO;

    @CreatedDate
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @LastModifiedDate
    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    @OneToMany(mappedBy = "materia", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TopicoEntity> topicos = new ArrayList<>();

    @OneToMany(mappedBy = "materia")
    @Builder.Default
    private List<QuestaoEntity> questoes = new ArrayList<>();

    @PrePersist
    @PreUpdate
    private void sincronizarNomeNormalizado() {
        if (this.nome != null) {
            this.nomeNormalizado = NomeNormalizer.normalizar(this.nome);
        }
    }

    public Long getCursoId() {
        return curso != null ? curso.getId() : null;
    }

    public String getCursoNome() {
        return curso != null ? curso.getNome() : null;
    }

    public void adicionarTopico(TopicoEntity topico) {
        topicos.add(topico);
        topico.setMateria(this);
    }

    public void removerTopico(TopicoEntity topico) {
        topicos.remove(topico);
        topico.setMateria(null);
    }

    public void adicionarQuestao(QuestaoEntity questao) {
        questoes.add(questao);
        questao.setMateria(this);
    }

    public void removerQuestao(QuestaoEntity questao) {
        questoes.remove(questao);
        questao.setMateria(null);
    }

    public boolean isAtivo() {
        return Status.ATIVO.equals(this.status);
    }

    public void ativar() {
        this.status = Status.ATIVO;
    }

    public void inativar() {
        this.status = Status.INATIVO;
    }

    public void excluir() {
        this.status = Status.EXCLUIDO;
    }

    public void atualizar(String nome, String descricao, Integer ordem, BigDecimal peso) {
        if (nome != null && !nome.isBlank()) {
            this.nome = nome;
        }
        if (descricao != null) {
            this.descricao = descricao;
        }
        if (ordem != null) {
            this.ordem = ordem;
        }
        if (peso != null) {
            this.peso = peso;
        }
    }

    public boolean possuiTopicos() {
        return topicos != null && !topicos.isEmpty();
    }

    public boolean possuiQuestoes() {
        return questoes != null && !questoes.isEmpty();
    }

    public int getQuantidadeTopicos() {
        return topicos != null ? topicos.size() : 0;
    }

    public int getQuantidadeQuestoes() {
        return questoes != null ? questoes.size() : 0;
    }


    public static class MateriaEntityBuilder {


        public MateriaEntityBuilder cursoId(Long cursoId) {
            if (cursoId != null) {
                CursoEntity curso = new CursoEntity();
                curso.setId(cursoId);
                this.curso(curso);
            }
            return this;
        }


        @Deprecated(since = "após V2", forRemoval = true)
        public MateriaEntityBuilder concursoId(Long concursoId) {
            return cursoId(concursoId);
        }
    }
}
