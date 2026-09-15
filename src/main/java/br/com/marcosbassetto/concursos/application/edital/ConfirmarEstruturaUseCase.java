package br.com.marcosbassetto.concursos.application.edital;

import br.com.marcosbassetto.concursos.common.enums.Status;
import br.com.marcosbassetto.concursos.common.exception.BusinessException;
import br.com.marcosbassetto.concursos.common.util.NomeNormalizer;
import br.com.marcosbassetto.concursos.domain.concurso.entity.ConcursoEntity;
import br.com.marcosbassetto.concursos.domain.concurso.service.ConcursoService;
import br.com.marcosbassetto.concursos.domain.curso.entity.CursoEntity;
import br.com.marcosbassetto.concursos.domain.curso.repository.CursoRepository;
import br.com.marcosbassetto.concursos.domain.edital.dto.EstruturaEditalDTO;
import br.com.marcosbassetto.concursos.domain.edital.dto.MateriaExtraida;
import br.com.marcosbassetto.concursos.domain.edital.dto.TopicoExtraido;
import br.com.marcosbassetto.concursos.domain.materia.entity.MateriaEntity;
import br.com.marcosbassetto.concursos.domain.materia.service.MateriaService;
import br.com.marcosbassetto.concursos.domain.topico.entity.TopicoEntity;
import br.com.marcosbassetto.concursos.domain.topico.service.TopicoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Deprecated(since = "migração Template+Strategy")
@Slf4j
@Service
@RequiredArgsConstructor
public class ConfirmarEstruturaUseCase {

    private static final String CURSO_PADRAO_NOME = "Geral";
    private static final String CURSO_PADRAO_CODIGO = "GERAL";

    private final ConcursoService concursoService;
    private final MateriaService materiaService;
    private final TopicoService topicoService;
    private final CursoRepository cursoRepository;

    @Transactional
    public void confirmar(Long concursoId, EstruturaEditalDTO estrutura) {

        ConcursoEntity concurso = concursoService.buscarEntidadePorId(concursoId);

        List<MateriaExtraida> materias = extrairMaterias(estrutura);

        if (materias.isEmpty()) {
            throw new BusinessException(
                    "A estrutura do edital está vazia. Nenhuma matéria encontrada.");
        }

        CursoEntity cursoPadrao = obterOuCriarCursoPadrao(concurso);

        int ordemMateria = 1;
        for (MateriaExtraida mDto : materias) {

            if (mDto.nome() == null || mDto.nome().isBlank()) {
                continue;
            }

            MateriaEntity materia = new MateriaEntity();
            materia.setCurso(cursoPadrao);
            materia.setNome(mDto.nome());
            materia.setOrdem(ordemMateria++);
            materia.setStatus(Status.ATIVO);
            materia = materiaService.criar(materia);

            List<TopicoExtraido> topicos = mDto.topicos();
            if (topicos != null && !topicos.isEmpty()) {
                int ordemTopico = 1;
                for (TopicoExtraido t : topicos) {
                    if (t == null || t.nome() == null || t.nome().isBlank()) {
                        continue;
                    }

                    TopicoEntity topico = new TopicoEntity();
                    topico.setMateria(materia);
                    topico.setNome(t.nome());
                    topico.setOrdem(ordemTopico++);
                    topico.setAtivo(true);
                    topicoService.criar(topico);
                }
            }
        }

        concursoService.marcarComoProcessado(concursoId);
    }

    private List<MateriaExtraida> extrairMaterias(EstruturaEditalDTO estrutura) {
        if (estrutura.cursos() != null && !estrutura.cursos().isEmpty()) {
            return estrutura.materiasDoPrimeiroCurso();
        }
        return estrutura.materias();
    }

    private CursoEntity obterOuCriarCursoPadrao(ConcursoEntity concurso) {
        String chave = NomeNormalizer.normalizar(CURSO_PADRAO_NOME);

        return cursoRepository.findByConcursoIdAndNomeNormalizado(concurso.getId(), chave)
                .orElseGet(() -> {
                    log.debug("Criando curso padrão '{}' para concurso id={} (fluxo legado)",
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
