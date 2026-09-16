package br.com.marcosbassetto.concursos.infrastructure.config;

import br.com.marcosbassetto.concursos.common.enums.Status;
import br.com.marcosbassetto.concursos.common.util.NomeNormalizer;
import br.com.marcosbassetto.concursos.domain.concurso.entity.ConcursoEntity;
import br.com.marcosbassetto.concursos.domain.concurso.repository.ConcursoRepository;
import br.com.marcosbassetto.concursos.domain.curso.entity.CursoEntity;
import br.com.marcosbassetto.concursos.domain.curso.repository.CursoRepository;
import br.com.marcosbassetto.concursos.domain.materia.entity.MateriaEntity;
import br.com.marcosbassetto.concursos.domain.materia.repository.MateriaRepository;
import br.com.marcosbassetto.concursos.domain.questao.domain.Alternativa;
import br.com.marcosbassetto.concursos.domain.questao.domain.OrigemQuestao;
import br.com.marcosbassetto.concursos.domain.questao.domain.TipoQuestao;
import br.com.marcosbassetto.concursos.domain.questao.entity.QuestaoEntity;
import br.com.marcosbassetto.concursos.domain.questao.repository.QuestaoRepository;
import br.com.marcosbassetto.concursos.domain.usuario.domain.PerfilUsuario;
import br.com.marcosbassetto.concursos.domain.usuario.entity.UsuarioEntity;
import br.com.marcosbassetto.concursos.domain.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    private static final String CURSO_PADRAO_NOME = "Geral";
    private static final String CURSO_PADRAO_CODIGO = "GERAL";

    private final UsuarioRepository usuarioRepository;
    private final ConcursoRepository concursoRepository;
    private final CursoRepository cursoRepository;
    private final MateriaRepository materiaRepository;
    private final QuestaoRepository questaoRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("🚀 Iniciando carga de dados de desenvolvimento...");

        UsuarioEntity usuario = loadUsuario();
        ConcursoEntity concurso = loadConcurso(usuario);
        CursoEntity curso = loadCursoPadrao(concurso);
        MateriaEntity materia = loadMateria(curso);
        loadQuestoes(materia);

        log.info("✅ Carga de dados de desenvolvimento concluída!");
    }

    private UsuarioEntity loadUsuario() {
        String email = "marcos@email.com";
        return usuarioRepository.findByEmail(email).orElseGet(() -> {
            log.info("👤 Criando usuário padrão: {}", email);
            UsuarioEntity usuario = UsuarioEntity.builder()
                    .email(email)
                    .senhaHash(passwordEncoder.encode("123456"))
                    .nome("Marcos Bassetto")
                    .ativo(true)
                    .perfil(PerfilUsuario.USER)
                    .criadoEm(LocalDateTime.now())
                    .build();
            return usuarioRepository.save(usuario);
        });
    }

    private ConcursoEntity loadConcurso(UsuarioEntity usuario) {
        String nome = "Concurso TJ-SP 2026";
        return concursoRepository.findByUsuarioIdAndNome(usuario.getId(), nome).orElseGet(() -> {
            log.info("📁 Criando concurso: {}", nome);
            ConcursoEntity concurso = ConcursoEntity.builder()
                    .usuario(usuario)
                    .nome(nome)
                    .orgao("Tribunal de Justiça de São Paulo")
                    .cargo("Técnico Judiciário")
                    .banca("VUNESP")
                    .ano(2026)
                    .status(Status.ATIVO)
                    .criadoEm(LocalDateTime.now())
                    .build();
            return concursoRepository.save(concurso);
        });
    }

    private CursoEntity loadCursoPadrao(ConcursoEntity concurso) {
        String chave = NomeNormalizer.normalizar(CURSO_PADRAO_NOME);

        return cursoRepository.findByConcursoIdAndNomeNormalizado(concurso.getId(), chave)
                .orElseGet(() -> {
                    log.info("📦 Criando curso padrão: {} (concurso id={})",
                            CURSO_PADRAO_NOME, concurso.getId());
                    CursoEntity curso = CursoEntity.builder()
                            .concurso(concurso)
                            .nome(CURSO_PADRAO_NOME)
                            .codigo(CURSO_PADRAO_CODIGO)
                            .ordem(1)
                            .build();
                    return cursoRepository.save(curso);
                });
    }

    private MateriaEntity loadMateria(CursoEntity curso) {
        String nome = "Direito Constitucional";
        String chave = NomeNormalizer.normalizar(nome);

        // ⚠️ MUDANÇA: findByCurso_IdAndNomeNormalizado (com underscore)
        // Motivo: MateriaEntity tem getter derivado getCursoId(), então
        // findByCursoId é ambíguo. Underscore desambigua explicitamente.
        return materiaRepository.findByCurso_IdAndNomeNormalizado(curso.getId(), chave)
                .orElseGet(() -> {
                    log.info("📚 Criando matéria: {}", nome);
                    MateriaEntity materia = MateriaEntity.builder()
                            .curso(curso)
                            .nome(nome)
                            .descricao("Matéria de Direito Constitucional para testes")
                            .ordem(1)
                            .status(Status.ATIVO)
                            .build();
                    return materiaRepository.save(materia);
                });
    }

    private void loadQuestoes(MateriaEntity materia) {
        if (questaoRepository.countByMateriaId(materia.getId()) > 0) {
            log.info("⏭️ Questões já existem para a matéria {}. Pulando...", materia.getNome());
            return;
        }

        log.info("📝 Criando 5 questões para a matéria: {}", materia.getNome());

        saveQuestao(materia, "A Constituição Federal de 1988 estabelece que são direitos e garantias fundamentais:",
                TipoQuestao.MULTIPLA_ESCOLHA, "C",
                List.of(
                        new Alternativa("A", "Somente os direitos individuais expressos no Art. 5º."),
                        new Alternativa("B", "Somente os direitos sociais do Art. 6º."),
                        new Alternativa("C", "Os direitos individuais, coletivos, sociais e políticos previstos na Constituição."),
                        new Alternativa("D", "Apenas os direitos previstos em tratados internacionais.")
                ));

        saveQuestao(materia, "O princípio da legalidade na Administração Pública está previsto no Art. 37 da CF/88.",
                TipoQuestao.CERTO_ERRADO, "CERTO", null);

        saveQuestao(materia, "O Brasil adota a forma de governo monárquica.",
                TipoQuestao.CERTO_ERRADO, "ERRADO", null);

        saveQuestao(materia, "A separação dos Poderes no Brasil é uma cláusula pétrea?",
                TipoQuestao.CERTO_ERRADO, "CERTO", null);

        saveQuestao(materia, "O STF é o guardião da Constituição.",
                TipoQuestao.CERTO_ERRADO, "CERTO", null);

        log.info("✅ 5 questões criadas para a matéria {}", materia.getNome());
    }

    private void saveQuestao(MateriaEntity materia, String enunciado, TipoQuestao tipo,
                             String respostaCorreta, List<Alternativa> alternativas) {
        QuestaoEntity questao = QuestaoEntity.builder()
                .materia(materia)
                .enunciado(enunciado)
                .tipo(tipo)
                .alternativas(alternativas)
                .respostaCorreta(respostaCorreta)
                .origem(OrigemQuestao.USUARIO)
                .ativo(true)
                .criadoEm(LocalDateTime.now())
                .build();
        questaoRepository.save(questao);
    }
}
