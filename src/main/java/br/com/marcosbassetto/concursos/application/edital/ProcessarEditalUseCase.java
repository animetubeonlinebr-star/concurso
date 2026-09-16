package br.com.marcosbassetto.concursos.application.edital;

import br.com.marcosbassetto.concursos.common.exception.BusinessException;
import br.com.marcosbassetto.concursos.config.AsyncConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Orquestra o processamento do edital em background.
 *
 * Determinístico: nenhuma etapa usa IA. A extração é feita por extratores
 * baseados em regex e o resultado vai para o staging de revisão; a decisão
 * final (selecionar, renomear, mesclar) é sempre do usuário.
 *
 * Não é transacional de propósito: quem controla a transação é
 * {@link EditalStagingService}, para que a falha reverta o staging mas
 * ainda permita gravar o status ERRO.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessarEditalUseCase {

    private final EditalStagingService stagingService;

    @Async(AsyncConfig.EDITAL_EXECUTOR)
    @EventListener(ImportacaoIniciadaEvent.class)
    public void aoIniciarImportacao(ImportacaoIniciadaEvent evento) {
        processar(evento.concursoId());
    }

    public void processar(Long concursoId) {
        try {
            stagingService.processar(concursoId);
        } catch (Exception e) {
            log.error("Falha ao processar edital | concursoId={}", concursoId, e);
            registrarErro(concursoId, e);
        }
    }

    /**
     * O status ERRO é gravado em transação própria: se usasse a transação
     * revertida do processamento, o registro que explica a falha ao usuário
     * desapareceria junto.
     */
    private void registrarErro(Long concursoId, Exception e) {
        try {
            stagingService.registrarErro(concursoId, mensagemDe(e));
        } catch (Exception falhaAoRegistrar) {
            log.error("Não foi possível registrar o erro do processamento | concursoId={}",
                    concursoId, falhaAoRegistrar);
        }
    }

    private String mensagemDe(Exception e) {
        if (e instanceof BusinessException be) {
            return be.getMessage();
        }
        return "Falha ao processar o edital. Tente reprocessar; "
                + "se persistir, verifique a qualidade do PDF.";
    }
}
