package br.com.marcosbassetto.concursos.domain.edital.interpretacao.estrategia;

import br.com.marcosbassetto.concursos.domain.edital.interpretacao.perfil.EditalProfile;
import br.com.marcosbassetto.concursos.domain.edital.interpretacao.rascunho.EstruturaRascunho;
import br.com.marcosbassetto.concursos.domain.edital.interpretacao.documento.DocumentModel;

/**
 * Decide "o que extrair e como mapear" para um tipo de edital.
 *
 * É o ponto de extensão que faltava: antes, essa decisão vivia dentro de um
 * único extrator e cresceria junto com ele. Agora, cada formato de edital tem
 * sua própria estratégia, e incluir um formato novo significa criar uma
 * classe nova em vez de adicionar mais um {@code if} no serviço de staging.
 *
 * Implementações devem ser determinísticas e sem estado mutável: a mesma
 * entrada precisa produzir sempre o mesmo rascunho.
 */
public interface EstrategiaInterpretacaoEdital {

    /** Código do perfil principal que esta estratégia atende (ex.: "GENERICO"). */
    String perfilSuportado();

    /**
     * Se esta estratégia atende o perfil informado.
     *
     * Permite que uma estratégia cubra variações do mesmo formato sem
     * duplicar classe — o perfil registra matizes de organização (cargo,
     * área), mas a forma de interpretar é a mesma.
     */
    default boolean suporta(String perfilCodigo) {
        return perfilSuportado().equals(perfilCodigo);
    }

    /** Aplica a estratégia ao documento, produzindo um rascunho auditável. */
    EstruturaRascunho interpretar(DocumentModel documento, EditalProfile perfil);
}