package br.com.marcosbassetto.concursos.domain.edital.interpretacao.estrategia;

import br.com.marcosbassetto.concursos.domain.edital.interpretacao.documento.DocumentModel;
import br.com.marcosbassetto.concursos.domain.edital.interpretacao.perfil.EditalProfile;
import br.com.marcosbassetto.concursos.domain.edital.interpretacao.rascunho.EstruturaRascunho;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Estratégia para documento que não é um edital de conteúdo programático.
 *
 * Resolve o caso VNSP2603: um "COMUNICADO" que lista programas de
 * pós-graduação em tabela. Não há disciplina alguma ali, mas as linhas em
 * maiúsculas ("UNIVERSIDADE ESTADUAL PAULISTA") eram promovidas a matéria e
 * o sistema reportava sucesso com 5 matérias e 0 tópicos.
 *
 * A resposta correta é não extrair nada e dizer por quê — declarar que o
 * documento não tem conteúdo programático é informação útil para o usuário,
 * enquanto 5 "matérias" inventadas são ruído que ele precisa apagar à mão.
 */
@Slf4j
@Component
public class EstrategiaDocumentoSemConteudo implements EstrategiaInterpretacaoEdital {

    private static final String PERFIL = "SEM_CONTEUDO";

    private static final String MENSAGEM =
            "Este arquivo não contém seção de conteúdo programático. "
                    + "Ele parece ser um comunicado, aviso ou documento de instruções, "
                    + "não um edital com disciplinas e tópicos.";

    @Override
    public String perfilSuportado() {
        return PERFIL;
    }

    @Override
    public boolean suporta(String perfilCodigo) {
        return PERFIL.equals(perfilCodigo);
    }

    @Override
    public EstruturaRascunho interpretar(DocumentModel documento, EditalProfile perfil) {
        log.info("Documento sem conteúdo programático | nada a extrair");

        return EstruturaRascunho.vazia(perfil, MENSAGEM);
    }
}