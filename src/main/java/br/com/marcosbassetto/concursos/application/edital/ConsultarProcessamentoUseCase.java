package br.com.marcosbassetto.concursos.application.edital;

import br.com.marcosbassetto.concursos.common.exception.BusinessException;
import br.com.marcosbassetto.concursos.common.exception.ErrorCodes;
import br.com.marcosbassetto.concursos.common.exception.ResourceNotFoundException;
import br.com.marcosbassetto.concursos.config.AsyncConfig;
import br.com.marcosbassetto.concursos.domain.concurso.entity.ConcursoEntity;
import br.com.marcosbassetto.concursos.domain.concurso.repository.ConcursoRepository;
import br.com.marcosbassetto.concursos.domain.edital.domain.StatusProcessamento;
import br.com.marcosbassetto.concursos.domain.edital.dto.StatusProcessamentoResponse;
import br.com.marcosbassetto.concursos.domain.edital.entity.EditalImportacaoEntity;
import br.com.marcosbassetto.concursos.domain.edital.repository.EditalImportacaoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConsultarProcessamentoUseCase {

    private final ConcursoRepository concursoRepository;
    private final EditalImportacaoRepository importacaoRepository;
    private final ProcessarEditalUseCase processarEditalUseCase;

    /**
     * A propriedade é sempre validada pelo concurso do usuário autenticado:
     * um id de terceiro responde 404, nunca o status de outra pessoa.
     */
    public StatusProcessamentoResponse consultar(Long concursoId, Long usuarioId) {
        ConcursoEntity concurso = concursoRepository.findByIdAndUsuario_Id(concursoId, usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Concurso", "id", concursoId));

        String mensagemErro = importacaoRepository.findByConcurso_Id(concursoId)
                .map(EditalImportacaoEntity::getMensagemErro)
                .orElse(null);

        return StatusProcessamentoResponse.de(
                concursoId, concurso.getStatusProcessamento(), mensagemErro);
    }

    /** Dispara o reprocessamento sem bloquear a resposta HTTP. */
    @Async(AsyncConfig.EDITAL_EXECUTOR)
    public void dispararReprocessamento(Long concursoId) {
        processarEditalUseCase.processar(concursoId);
    }

    /**
     * Reprocessar durante o processamento duplicaria trabalho concorrente
     * sobre o mesmo staging.
     */
    public void validarReprocessamento(Long concursoId, Long usuarioId) {
        ConcursoEntity concurso = concursoRepository.findByIdAndUsuario_Id(concursoId, usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Concurso", "id", concursoId));

        if (StatusProcessamento.PROCESSANDO.equals(concurso.getStatusProcessamento())) {
            throw new BusinessException(
                    ErrorCodes.PROCESSAMENTO_EM_ANDAMENTO,
                    "O edital já está sendo processado. Aguarde a conclusão.");
        }
    }
}
