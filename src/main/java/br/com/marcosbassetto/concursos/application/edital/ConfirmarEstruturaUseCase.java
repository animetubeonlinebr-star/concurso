package br.com.marcosbassetto.concursos.application.edital;

import br.com.marcosbassetto.concursos.common.enums.Status;
import br.com.marcosbassetto.concursos.common.exception.BusinessException;
import br.com.marcosbassetto.concursos.common.exception.ErrorCodes;
import br.com.marcosbassetto.concursos.common.exception.ResourceNotFoundException;
import br.com.marcosbassetto.concursos.common.util.NomeNormalizer;
import br.com.marcosbassetto.concursos.domain.concurso.entity.ConcursoEntity;
import br.com.marcosbassetto.concursos.domain.concurso.repository.ConcursoRepository;
import br.com.marcosbassetto.concursos.domain.curso.entity.CursoEntity;
import br.com.marcosbassetto.concursos.domain.curso.repository.CursoRepository;
import br.com.marcosbassetto.concursos.domain.edital.domain.StatusProcessamento;
import br.com.marcosbassetto.concursos.domain.edital.dto.ConfirmacaoEstruturaResponse;
import br.com.marcosbassetto.concursos.domain.edital.entity.EditalImportacaoEntity;
import br.com.marcosbassetto.concursos.domain.edital.entity.MateriaSugeridaEntity;
import br.com.marcosbassetto.concursos.domain.edital.entity.TopicoSugeridoEntity;
import br.com.marcosbassetto.concursos.domain.edital.repository.EditalImportacaoRepository;
import br.com.marcosbassetto.concursos.domain.edital.repository.MateriaSugeridaRepository;
import br.com.marcosbassetto.concursos.domain.edital.repository.TopicoSugeridoRepository;
import br.com.marcosbassetto.concursos.domain.materia.domain.OrigemMateria;
import br.com.marcosbassetto.concursos.domain.materia.entity.MateriaEntity;
import br.com.marcosbassetto.concursos.domain.materia.repository.MateriaRepository;
import br.com.marcosbassetto.concursos.domain.materia.service.MateriaService;
import br.com.marcosbassetto.concursos.domain.topico.entity.TopicoEntity;
import br.com.marcosbassetto.concursos.domain.topico.repository.TopicoRepository;
import br.com.marcosbassetto.concursos.domain.topico.service.TopicoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Promove o staging revisado para a hierarquia real ({@code materia} e
 * {@code topico}) e encerra a importação.
 *
 * Só matérias selecionadas são persistidas. O unique constraint de matéria e
 * o erro MATERIA_DUPLICADA do {@link MateriaService} continuam sendo a última
 * barreira: se algo escapou da detecção, a confirmação falha em vez de
 * gravar estrutura inconsistente.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConfirmarEstruturaUseCase {

    private static final String CURSO_PADRAO_NOME = "Geral";
    private static final String CURSO_PADRAO_CODIGO = "GERAL";

    private final ConcursoRepository concursoRepository;
    private final EditalImportacaoRepository importacaoRepository;
    private final MateriaSugeridaRepository materiaSugeridaRepository;
    private final TopicoSugeridoRepository topicoSugeridoRepository;
    private final CursoRepository cursoRepository;
    private final MateriaRepository materiaRepository;
    private final TopicoRepository topicoRepository;
    private final MateriaService materiaService;
    private final TopicoService topicoService;

    @Transactional
    public ConfirmacaoEstruturaResponse confirmar(Long concursoId, Long usuarioId) {
        ConcursoEntity concurso = concursoRepository.findByIdAndUsuario_Id(concursoId, usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Concurso", "id", concursoId));

        EditalImportacaoEntity importacao = importacaoRepository.findByConcurso_Id(concursoId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCodes.IMPORTACAO_NAO_ENCONTRADA,
                        "Nenhuma importação encontrada para o concurso " + concursoId + "."));

        // Idempotência: importação já promovida não pode ser promovida de novo.
        if (StatusProcessamento.CONFIRMADO.equals(importacao.getStatus())) {
            throw new BusinessException(
                    ErrorCodes.CONFLITO_ESTADO,
                    "Esta importação já foi confirmada.");
        }

        if (!importacao.aguardandoRevisao()) {
            throw new BusinessException(
                    ErrorCodes.ESTRUTURA_NAO_REVISAVEL,
                    "A estrutura deste edital não está pronta para confirmação. "
                            + "Status atual: " + importacao.getStatus() + ".");
        }

        List<MateriaSugeridaEntity> sugeridas =
                materiaSugeridaRepository.findByImportacao_IdOrderByOrdemAsc(importacao.getId());

        List<MateriaSugeridaEntity> selecionadas = sugeridas.stream()
                .filter(m -> Boolean.TRUE.equals(m.getSelecionada()))
                .toList();

        if (selecionadas.isEmpty()) {
            throw new BusinessException(
                    ErrorCodes.DADOS_INVALIDOS,
                    "Nenhuma matéria foi selecionada. Selecione ao menos uma para confirmar.");
        }

        // O unique constraint de materia/topico seria a barreira final, mas
        // falharia no meio da promoção. Abortar antes deixa o staging intacto
        // e diz exatamente qual sugestão conflita.
        validarColisoesComConteudoConfirmado(concursoId, selecionadas);

        CursoEntity curso = obterOuCriarCursoPadrao(concurso);

        List<String> ignoradas = new ArrayList<>();
        int materiasPersistidas = 0;
        int topicosPersistidos = 0;

        int ordemMateria = 1;
        for (MateriaSugeridaEntity sugerida : selecionadas) {

            MateriaEntity materia = new MateriaEntity();
            materia.setCurso(curso);
            materia.setNome(sugerida.getNome());
            materia.setOrdem(ordemMateria++);
            materia.setStatus(Status.ATIVO);
            materia.setOrigem(OrigemMateria.EDITAL);
            materia = materiaService.criar(materia);

            materiasPersistidas++;

            List<TopicoSugeridoEntity> topicos = topicoSugeridoRepository
                    .findByMateriaSugerida_IdOrderByOrdemAsc(sugerida.getId());

            int ordemTopico = 1;
            for (TopicoSugeridoEntity sugerido : topicos) {
                if (!Boolean.TRUE.equals(sugerido.getSelecionado())) {
                    ignoradas.add(materia.getNome() + " > " + sugerido.getNome());
                    continue;
                }

                TopicoEntity topico = new TopicoEntity();
                topico.setMateria(materia);
                topico.setNome(sugerido.getNome());
                topico.setOrdem(ordemTopico++);
                topico.setAtivo(true);
                topicoService.criar(topico);

                topicosPersistidos++;
            }
        }

        importacao.setStatus(StatusProcessamento.CONFIRMADO);
        importacao.setConfirmadoEm(LocalDateTime.now());
        importacaoRepository.save(importacao);

        concurso.definirStatusProcessamento(StatusProcessamento.CONFIRMADO);
        concursoRepository.save(concurso);

        log.info("Estrutura confirmada | concursoId={} | materias={} | topicos={} | ignorados={}",
                concursoId, materiasPersistidas, topicosPersistidos, ignoradas.size());

        return new ConfirmacaoEstruturaResponse(
                concursoId, materiasPersistidas, topicosPersistidos, ignoradas);
    }

    private void validarColisoesComConteudoConfirmado(
            Long concursoId, List<MateriaSugeridaEntity> selecionadas) {

        Set<String> materiasGravadas = new HashSet<>(
                materiaRepository.findNomeNormalizadoByConcursoId(concursoId));
        Set<String> topicosGravados = new HashSet<>(
                topicoRepository.findNomeNormalizadoByConcursoId(concursoId));

        List<String> conflitos = new ArrayList<>();

        for (MateriaSugeridaEntity materia : selecionadas) {
            if (materiasGravadas.contains(materia.getNomeNormalizado())) {
                conflitos.add(materia.getNome());
                continue;
            }

            List<TopicoSugeridoEntity> topicos = topicoSugeridoRepository
                    .findByMateriaSugerida_IdOrderByOrdemAsc(materia.getId());

            topicos.stream()
                    .filter(t -> Boolean.TRUE.equals(t.getSelecionado()))
                    .filter(t -> topicosGravados.contains(t.getNomeNormalizado()))
                    .forEach(t -> conflitos.add(materia.getNome() + " > " + t.getNome()));
        }

        if (!conflitos.isEmpty()) {
            throw new BusinessException(
                    ErrorCodes.DADOS_INVALIDOS,
                    "Estes itens já existem no conteúdo confirmado do concurso: "
                            + String.join("; ", conflitos)
                            + ". Desmarque-os na revisão antes de confirmar.");
        }
    }

    /**
     * O fluxo de edital não tem noção de cargo/módulo, então todas as
     * matérias confirmadas vão para o curso "Geral" do concurso.
     */
    private CursoEntity obterOuCriarCursoPadrao(ConcursoEntity concurso) {
        String chave = NomeNormalizer.normalizar(CURSO_PADRAO_NOME);

        return cursoRepository.findByConcursoIdAndNomeNormalizado(concurso.getId(), chave)
                .orElseGet(() -> cursoRepository.save(
                        CursoEntity.builder()
                                .concurso(concurso)
                                .nome(CURSO_PADRAO_NOME)
                                .nomeNormalizado(chave)
                                .codigo(CURSO_PADRAO_CODIGO)
                                .ordem(1)
                                .build()));
    }
}
