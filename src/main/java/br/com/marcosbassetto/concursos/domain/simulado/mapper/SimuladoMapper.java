package br.com.marcosbassetto.concursos.domain.simulado.mapper;

import br.com.marcosbassetto.concursos.domain.questao.entity.QuestaoEntity;
import br.com.marcosbassetto.concursos.domain.resposta.entity.RespostaEntity;
import br.com.marcosbassetto.concursos.domain.simulado.dto.CriarSimuladoRequest;
import br.com.marcosbassetto.concursos.domain.simulado.dto.SimuladoQuestaoResponse;
import br.com.marcosbassetto.concursos.domain.simulado.dto.SimuladoResponse;
import br.com.marcosbassetto.concursos.domain.simulado.entity.SimuladoEntity;
import br.com.marcosbassetto.concursos.domain.simulado.entity.SimuladoQuestaoEntity;
import org.springframework.stereotype.Component;

@Component
public class SimuladoMapper {

    public SimuladoEntity toEntity(CriarSimuladoRequest request) {
        if (request == null) {
            return null;
        }

        return SimuladoEntity.builder()
                .concursoId(request.concursoId())
                .materiaId(request.materiaId())
                .quantidadeQuestoes(request.quantidadeQuestoes())
                .titulo(request.titulo())
                .build();
    }

    public SimuladoResponse toResponse(SimuladoEntity simulado) {
        if (simulado == null) {
            return null;
        }

        return new SimuladoResponse(
                simulado.getId(),
                simulado.getConcursoId(),
                simulado.getMateriaId(),
                simulado.getTitulo(),
                simulado.getDescricao(),
                simulado.getStatus(),
                simulado.getQuantidadeQuestoes(),
                simulado.getCriadoEm(),
                simulado.getIniciadoEm(),
                simulado.getFinalizadoEm()
        );
    }

    /**
     * Monta a questão para resolução. Não expõe gabarito nem justificativa.
     */
    public SimuladoQuestaoResponse toQuestaoResponse(SimuladoQuestaoEntity simuladoQuestao,
                                                     QuestaoEntity questao,
                                                     RespostaEntity resposta) {
        if (simuladoQuestao == null) {
            return null;
        }

        boolean respondida = resposta != null && !resposta.isEmBranco();

        return new SimuladoQuestaoResponse(
                simuladoQuestao.getId(),
                simuladoQuestao.getOrdem(),
                simuladoQuestao.getQuestaoId(),
                questao != null ? questao.getEnunciado() : null,
                questao != null ? questao.getTipo() : null,
                questao != null ? questao.getAlternativas() : null,
                questao != null ? questao.getBanca() : null,
                questao != null ? questao.getAno() : null,
                questao != null && questao.getDificuldade() != null
                        ? questao.getDificuldade().name()
                        : null,
                respondida,
                resposta != null ? resposta.getAlternativaSelecionada() : null,
                resposta != null ? resposta.getRespostaTexto() : null
        );
    }
}