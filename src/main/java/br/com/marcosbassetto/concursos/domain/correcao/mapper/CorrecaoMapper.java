package br.com.marcosbassetto.concursos.domain.correcao.mapper;

import br.com.marcosbassetto.concursos.domain.correcao.domain.ResultadoCorrecao;
import br.com.marcosbassetto.concursos.domain.correcao.dto.CorrecaoResponse;
import br.com.marcosbassetto.concursos.domain.correcao.dto.DesempenhoResponse;
import br.com.marcosbassetto.concursos.domain.correcao.dto.ResultadoQuestaoResponse;
import br.com.marcosbassetto.concursos.domain.correcao.dto.ResultadoSimuladoResponse;
import br.com.marcosbassetto.concursos.domain.correcao.entity.CorrecaoEntity;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class CorrecaoMapper {

    public CorrecaoResponse toResponse(CorrecaoEntity correcao) {
        if (correcao == null) {
            return null;
        }

        return new CorrecaoResponse(
                correcao.getId(),
                correcao.getSimuladoId(),
                correcao.getSimuladoQuestaoId(),
                correcao.getRespostaUsuario(),
                correcao.getRespostaCorreta(),
                correcao.getResultado(),
                correcao.getCorrigidaEm()
        );
    }

    /**
     * Versão sem gabarito, para uso enquanto o simulado não foi finalizado.
     */
    public ResultadoQuestaoResponse toQuestaoResponse(CorrecaoEntity correcao) {
        if (correcao == null) {
            return null;
        }

        return new ResultadoQuestaoResponse(
                correcao.getSimuladoQuestaoId(),
                correcao.getRespostaUsuario(),
                correcao.getResultado()
        );
    }

    public ResultadoSimuladoResponse toSimuladoResponse(Long simuladoId, List<CorrecaoEntity> correcoes) {
        int corretas = 0;
        int incorretas = 0;
        int naoRespondidas = 0;

        for (CorrecaoEntity correcao : correcoes) {
            switch (correcao.getResultado()) {
                case CORRETA -> corretas++;
                case INCORRETA -> incorretas++;
                case NAO_RESPONDIDA -> naoRespondidas++;
            }
        }

        int total = correcoes.size();

        return new ResultadoSimuladoResponse(
                simuladoId,
                total,
                corretas,
                incorretas,
                naoRespondidas,
                calcularPercentual(corretas, total),
                // Primeira versão: 1 ponto por acerto, sem penalizar erro ou branco.
                corretas,
                LocalDateTime.now()
        );
    }

    public DesempenhoResponse toDesempenhoResponse(List<CorrecaoEntity> correcoes) {
        int corretas = 0;
        int incorretas = 0;
        int naoRespondidas = 0;

        for (CorrecaoEntity correcao : correcoes) {
            switch (correcao.getResultado()) {
                case CORRETA -> corretas++;
                case INCORRETA -> incorretas++;
                case NAO_RESPONDIDA -> naoRespondidas++;
            }
        }

        int total = correcoes.size();
        int respondidas = corretas + incorretas;

        return new DesempenhoResponse(
                total,
                respondidas,
                corretas,
                incorretas,
                naoRespondidas,
                calcularPercentual(corretas, total),
                corretas
        );
    }

    public ResultadoCorrecao classificar(String respostaUsuario, String respostaCorreta) {
        // Sem gabarito confiável não é possível decidir: o sistema não deve
        // assumir que a resposta do usuário é o gabarito.
        if (respostaCorreta == null || respostaCorreta.isBlank()) {
            throw new IllegalStateException(
                    "Gabarito ausente: a correção não pode ser realizada.");
        }

        if (respostaUsuario == null || respostaUsuario.isBlank()) {
            return ResultadoCorrecao.NAO_RESPONDIDA;
        }

        return respostaUsuario.trim().equalsIgnoreCase(respostaCorreta.trim())
                ? ResultadoCorrecao.CORRETA
                : ResultadoCorrecao.INCORRETA;
    }

    private double calcularPercentual(int corretas, int total) {
        if (total <= 0) {
            return 0.0;
        }
        return Math.round((corretas * 100.0 / total) * 100.0) / 100.0;
    }
}