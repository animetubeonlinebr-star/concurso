package br.com.marcosbassetto.concursos.domain.concurso.service;

import br.com.marcosbassetto.concursos.common.enums.Status;
import br.com.marcosbassetto.concursos.common.exception.BusinessException;
import br.com.marcosbassetto.concursos.common.exception.ResourceNotFoundException;
import br.com.marcosbassetto.concursos.common.util.NomeNormalizer;
import br.com.marcosbassetto.concursos.domain.concurso.dto.AtualizarConcursoRequest;
import br.com.marcosbassetto.concursos.domain.concurso.dto.ConcursoResponse;
import br.com.marcosbassetto.concursos.domain.concurso.dto.CriarConcursoRequest;
import br.com.marcosbassetto.concursos.domain.concurso.entity.ConcursoEntity;
import br.com.marcosbassetto.concursos.domain.concurso.mapper.ConcursoMapper;
import br.com.marcosbassetto.concursos.domain.concurso.repository.ConcursoRepository;
import br.com.marcosbassetto.concursos.domain.curso.entity.CursoEntity;
import br.com.marcosbassetto.concursos.domain.curso.repository.CursoRepository;
import br.com.marcosbassetto.concursos.domain.edital.domain.StatusProcessamento;
import br.com.marcosbassetto.concursos.domain.materia.domain.OrigemMateria;
import br.com.marcosbassetto.concursos.domain.materia.entity.MateriaEntity;
import br.com.marcosbassetto.concursos.domain.materia.repository.MateriaRepository;
import br.com.marcosbassetto.concursos.domain.topico.entity.TopicoEntity;
import br.com.marcosbassetto.concursos.domain.topico.repository.TopicoRepository;
import br.com.marcosbassetto.concursos.domain.usuario.entity.UsuarioEntity;
import br.com.marcosbassetto.concursos.domain.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConcursoService {

    private static final String CURSO_PADRAO_NOME = "Geral";
    private static final String CURSO_PADRAO_CODIGO = "GERAL";

    private final ConcursoRepository concursoRepository;
    private final UsuarioRepository usuarioRepository;
    private final CursoRepository cursoRepository;
    private final MateriaRepository materiaRepository;
    private final TopicoRepository topicoRepository;
    private final ConcursoMapper concursoMapper;

    // ─────────────────────────────────────────────────────────
    // CRIAR (manual — matérias + tópicos digitados pelo usuário)
    // ─────────────────────────────────────────────────────────

    @Transactional
    public ConcursoResponse criar(Long usuarioId, CriarConcursoRequest request) {
        UsuarioEntity usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", "id", usuarioId));

        ConcursoEntity concurso = ConcursoEntity.builder()
                .nome(request.nome())
                .orgao(request.orgao())
                .cargo(request.cargo())
                .banca(request.banca())
                .ano(request.ano())
                .descricao(request.descricao())
                .usuario(usuario)
                .status(Status.ATIVO)
                .criadoEm(LocalDateTime.now())
                .atualizadoEm(LocalDateTime.now())
                .build();

        // Cadastro manual não passa por extração: já nasce confirmado.
        // O setter mantém `processado` derivado de `statusProcessamento`.
        concurso.definirStatusProcessamento(StatusProcessamento.CONFIRMADO);

        ConcursoEntity salvo = concursoRepository.save(concurso);

        // Curso "Geral" implícito — balde único para matérias manuais
        CursoEntity cursoPadrao = obterOuCriarCursoPadrao(salvo);

        int ordemMateria = 1;
        for (CriarConcursoRequest.MateriaRequest m : request.materias()) {

            MateriaEntity materia = new MateriaEntity();
            materia.setCurso(cursoPadrao);
            materia.setNome(m.nome());
            materia.setOrdem(ordemMateria++);
            materia.setStatus(Status.ATIVO);
            materia.setOrigem(OrigemMateria.USUARIO);
            MateriaEntity materiaSalva = materiaRepository.save(materia);

            int ordemTopico = 1;
            for (String nomeTopico : m.topicos()) {
                TopicoEntity topico = new TopicoEntity();
                topico.setMateria(materiaSalva);
                topico.setNome(nomeTopico);
                topico.setOrdem(ordemTopico++);
                topico.setAtivo(true);
                topicoRepository.save(topico);
            }
        }

        return concursoMapper.toResponse(salvo);
    }

    // ─────────────────────────────────────────────────────────
    // Consultas
    // ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ConcursoResponse> listarPorUsuario(Long usuarioId) {
        return concursoRepository.findByUsuario_Id(usuarioId)
                .stream()
                .map(concursoMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ConcursoResponse buscarPorId(Long id) {
        ConcursoEntity concurso = concursoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Concurso", "id", id));
        return concursoMapper.toResponse(concurso);
    }

    @Transactional(readOnly = true)
    public ConcursoResponse buscarPorId(Long id, Long usuarioId) {
        ConcursoEntity concurso = concursoRepository.findById(id)
                .filter(c -> c.getUsuario().getId().equals(usuarioId))
                .orElseThrow(() -> new ResourceNotFoundException("Concurso", "id", id));
        return concursoMapper.toResponse(concurso);
    }

    // ─────────────────────────────────────────────────────────
    // Atualização
    // ─────────────────────────────────────────────────────────

    @Transactional
    public ConcursoResponse atualizar(Long id, AtualizarConcursoRequest request, Long usuarioId) {
        ConcursoEntity concurso = concursoRepository.findById(id)
                .filter(c -> c.getUsuario().getId().equals(usuarioId))
                .orElseThrow(() -> new ResourceNotFoundException("Concurso", "id", id));

        concurso.setNome(request.nome());
        concurso.setOrgao(request.orgao());
        concurso.setCargo(request.cargo());
        concurso.setBanca(request.banca());
        concurso.setAno(request.ano());
        concurso.setDescricao(request.descricao());
        concurso.setAtualizadoEm(LocalDateTime.now());

        ConcursoEntity atualizado = concursoRepository.save(concurso);
        return concursoMapper.toResponse(atualizado);
    }

    // ─────────────────────────────────────────────────────────
    // Métodos internos (retornam Entity — para uso em outros services)
    // ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ConcursoEntity buscarEntidadePorId(Long id) {
        return concursoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Concurso", "id", id));
    }

    @Transactional
    public void marcarComoProcessado(Long id) {
        ConcursoEntity concurso = buscarEntidadePorId(id);
        // Delega ao setter: `processado` é derivado de `statusProcessamento`,
        // então mexer só no primeiro deixaria o concurso num estado impossível.
        concurso.definirStatusProcessamento(StatusProcessamento.CONFIRMADO);
        concurso.setAtualizadoEm(LocalDateTime.now());
        concursoRepository.save(concurso);
    }

    @Transactional
    public ConcursoEntity atualizarEntidade(ConcursoEntity concurso) {
        if (!concursoRepository.existsById(concurso.getId())) {
            throw new ResourceNotFoundException("Concurso", "id", concurso.getId());
        }
        concurso.setAtualizadoEm(LocalDateTime.now());
        return concursoRepository.save(concurso);
    }

    @Transactional(readOnly = true)
    public String buscarTextoExtraido(Long id) {
        ConcursoEntity concurso = concursoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Concurso", "id", id));

        String texto = concurso.getTextoExtraido();
        if (texto == null || texto.isBlank()) {
            throw new BusinessException("O edital ainda não foi importado ou o texto não foi extraído.");
        }
        return texto;
    }

    // ─────────────────────────────────────────────────────────
    // Helper: curso "Geral" implícito (idempotente)
    // ─────────────────────────────────────────────────────────

    /**
     * Recupera (ou cria) o curso "Geral" do concurso.
     *
     * Por que existe: a criação manual de concurso não tem noção de
     * cargo/módulo. Todas as matérias digitadas vão para este curso balde.
     *
     * Idempotente: chamado N vezes, retorna sempre o mesmo registro.
     */
    private CursoEntity obterOuCriarCursoPadrao(ConcursoEntity concurso) {
        String chave = NomeNormalizer.normalizar(CURSO_PADRAO_NOME);

        return cursoRepository.findByConcursoIdAndNomeNormalizado(concurso.getId(), chave)
                .orElseGet(() -> {
                    log.debug("Criando curso padrão '{}' para concurso id={}",
                            CURSO_PADRAO_NOME, concurso.getId());
                    return cursoRepository.save(
                            CursoEntity.builder()
                                    .concurso(concurso)
                                    .nome(CURSO_PADRAO_NOME)
                                    .nomeNormalizado(chave)
                                    .codigo(CURSO_PADRAO_CODIGO)
                                    .ordem(1)
                                    .build()
                    );
                });
    }
}
