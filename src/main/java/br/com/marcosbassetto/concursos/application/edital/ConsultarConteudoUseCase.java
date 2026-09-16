package br.com.marcosbassetto.concursos.application.edital;

import br.com.marcosbassetto.concursos.common.exception.ResourceNotFoundException;
import br.com.marcosbassetto.concursos.domain.concurso.entity.ConcursoEntity;
import br.com.marcosbassetto.concursos.domain.concurso.repository.ConcursoRepository;
import br.com.marcosbassetto.concursos.domain.edital.dto.ConteudoConcursoResponse;
import br.com.marcosbassetto.concursos.domain.materia.entity.MateriaEntity;
import br.com.marcosbassetto.concursos.domain.materia.repository.MateriaRepository;
import br.com.marcosbassetto.concursos.domain.questao.repository.QuestaoRepository;
import br.com.marcosbassetto.concursos.domain.topico.entity.TopicoEntity;
import br.com.marcosbassetto.concursos.domain.topico.repository.TopicoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Monta a árvore matéria → tópico já confirmada no concurso.
 *
 * Só conteúdo persistido aparece aqui: o que está no staging de revisão
 * continua invisível até a confirmação.
 */
@Service
@RequiredArgsConstructor
public class ConsultarConteudoUseCase {

    private final ConcursoRepository concursoRepository;
    private final MateriaRepository materiaRepository;
    private final TopicoRepository topicoRepository;
    private final QuestaoRepository questaoRepository;

    @Transactional(readOnly = true)
    public ConteudoConcursoResponse consultar(Long concursoId, Long usuarioId) {
        ConcursoEntity concurso = concursoRepository.findByIdAndUsuario_Id(concursoId, usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Concurso", "id", concursoId));

        List<MateriaEntity> materias =
                materiaRepository.findByConcursoIdAndUsuarioId(concursoId, usuarioId);

        List<ConteudoConcursoResponse.MateriaConteudo> materiasConteudo = new ArrayList<>();

        int totalTopicos = 0;
        long totalQuestoes = 0;

        for (MateriaEntity materia : materias) {
            List<TopicoEntity> topicos =
                    topicoRepository.findByMateriaIdAndUsuarioId(materia.getId(), usuarioId);

            long questoesMateria = questaoRepository.countByMateriaIdAndAtivoTrue(materia.getId());

            List<ConteudoConcursoResponse.TopicoConteudo> topicosConteudo = topicos.stream()
                    .map(topico -> new ConteudoConcursoResponse.TopicoConteudo(
                            topico.getId(),
                            topico.getNome(),
                            topico.getOrdem(),
                            topico.getAtivo(),
                            questaoRepository.countByTopicoIdAndAtivoTrue(topico.getId())))
                    .toList();

            materiasConteudo.add(new ConteudoConcursoResponse.MateriaConteudo(
                    materia.getId(),
                    materia.getNome(),
                    materia.getOrdem(),
                    materia.getOrigem() != null ? materia.getOrigem().name() : null,
                    topicosConteudo.size(),
                    questoesMateria,
                    topicosConteudo));

            totalTopicos += topicosConteudo.size();
            totalQuestoes += questoesMateria;
        }

        return new ConteudoConcursoResponse(
                concurso.getId(),
                concurso.getNome(),
                concurso.getStatusProcessamento(),
                new ConteudoConcursoResponse.ResumoConteudo(
                        materiasConteudo.size(), totalTopicos, totalQuestoes),
                materiasConteudo);
    }
}
