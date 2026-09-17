package br.com.marcosbassetto.concursos.domain.edital.interpretacao.validacao;

import br.com.marcosbassetto.concursos.domain.edital.dto.StatusExtracao;
import br.com.marcosbassetto.concursos.domain.edital.interpretacao.rascunho.EstruturaRascunho;
import br.com.marcosbassetto.concursos.domain.edital.interpretacao.rascunho.MateriaRascunho;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Avalia a qualidade da extração e decide o status a informar ao usuário.
 *
 * Antes o status só olhava a contagem de matérias, então uma extração com 5
 * "matérias" e nenhum tópico era anunciada como PROCESSADO — o usuário só
 * descobria o problema ao abrir a revisão. Aqui a ausência de tópicos e a
 * quantidade de matérias sem conteúdo passam a rebaixar o status.
 */
@Slf4j
@Component
public class AvaliadorQualidadeExtracao {

    /** Abaixo disso, a extração é indício de que o arquivo não era o correto. */
    private static final int MINIMO_MATERIAS_CONFIAVEL = 2;

    public StatusExtracao avaliar(EstruturaRascunho rascunho, boolean dadosCompletos) {
        if (rascunho == null || rascunho.vazia()) {
            return StatusExtracao.NAO_IDENTIFICADO;
        }

        List<MateriaRascunho> materias = rascunho.materias();
        long comTopicos = materias.stream().filter(m -> !m.semTopicos()).count();

        // Nenhuma matéria tem tópico: o documento tem cabeçalhos de disciplina
        // mas o conteúdo não foi reconhecido. Não é sucesso.
        if (comTopicos == 0) {
            return StatusExtracao.BAIXA_CONFIANCA;
        }

        if (materias.size() < MINIMO_MATERIAS_CONFIAVEL) {
            return StatusExtracao.BAIXA_CONFIANCA;
        }

        // Matérias sem tópicos convivem com matérias completas: extração
        // parcial, precisa de conferência.
        if (comTopicos < materias.size()) {
            return StatusExtracao.PARCIAL;
        }

        return dadosCompletos ? StatusExtracao.PROCESSADO : StatusExtracao.PARCIAL;
    }
}