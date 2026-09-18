package br.com.marcosbassetto.concursos.domain.edital.segmenter;

import br.com.marcosbassetto.concursos.domain.edital.interpretacao.documento.DocumentModel;

/**
 * Forma de reconhecer onde começa o bloco de conteúdo programático.
 *
 * Existe porque editais reais ancoram o mesmo conteúdo de maneiras diferentes:
 * alguns trazem o título explícito ("ANEXO II – CONTEÚDO PROGRAMÁTICO"), outros
 * dispensam o cabeçalho e declaram apenas "CONHECIMENTOS GERAIS", e outros
 * numeram o título dentro de um capítulo ("15.2.4 CONHECIMENTOS GERAIS").
 *
 * A âncora é uma pergunta semântica — "esta linha declara o início do conteúdo
 * programático?" — e não um teste de forma. Reconhecer "qualquer linha
 * numerada" seria errado: transformaria "1. DISPOSIÇÕES PRELIMINARES" em
 * conteúdo programático.
 *
 * Implementações devem ser determinísticas e sem estado mutável.
 */
public interface AncoraBlocoConteudo {

    /** Identificador estável, para log e auditoria. */
    String nome();

    /**
     * Ordem de preferência: menor valor é tentado primeiro.
     *
     * Uma âncora mais específica (título explícito) deve vencer uma mais
     * genérica (título numerado), porque o título explícito é declaração
     * direta e o numerado depende do contexto do capítulo.
     */
    int ordem();

    /**
     * Posição no texto onde o bloco de conteúdo começa, ou -1 se esta âncora
     * não reconhece o documento.
     */
    int encontrarInicio(DocumentModel documento);
}
