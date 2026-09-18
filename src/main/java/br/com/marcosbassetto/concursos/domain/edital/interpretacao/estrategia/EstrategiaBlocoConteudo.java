package br.com.marcosbassetto.concursos.domain.edital.interpretacao.estrategia;

import br.com.marcosbassetto.concursos.domain.edital.interpretacao.disciplina.ExtratorDisciplinas;
import br.com.marcosbassetto.concursos.domain.edital.interpretacao.documento.DocumentModel;
import br.com.marcosbassetto.concursos.domain.edital.interpretacao.perfil.EditalProfile;
import br.com.marcosbassetto.concursos.domain.edital.interpretacao.rascunho.EstruturaRascunho;
import br.com.marcosbassetto.concursos.domain.edital.interpretacao.rascunho.MateriaRascunho;
import br.com.marcosbassetto.concursos.domain.edital.segmenter.SegmentadorEdital;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Estratégia para o edital organizado em seção "CONTEÚDO PROGRAMÁTICO".
 *
 * Só lê o que está dentro da seção declarada. A versão anterior usava o texto
 * inteiro como reserva quando não encontrava a seção, e o efeito era extrair
 * disciplinas de cronograma, de cláusulas de contrato e de formulários — o
 * documento inteiro virava estrutura. Quando não há seção, a resposta correta
 * é não haver matérias.
 *
 * A leitura das disciplinas dentro do bloco é delegada ao
 * {@link ExtratorDisciplinas}, que reconhece tanto a disciplina em linha
 * própria quanto a disciplina inline. Esta estratégia não sabe a diferença.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EstrategiaBlocoConteudo implements EstrategiaInterpretacaoEdital {

    private static final String PERFIL = "GENERICO";

    private final SegmentadorEdital segmentador;
    private final ExtratorDisciplinas extratorDisciplinas;

    @Override
    public String perfilSuportado() {
        return PERFIL;
    }

    /**
     * Cobre também os perfis com unidade de nível 1: cargo, área e curso
     * mudam o caminho da matéria, não a forma de ler o bloco — a
     * classificação já identifica esses rótulos linha a linha.
     */
    @Override
    public boolean suporta(String perfilCodigo) {
        return PERFIL.equals(perfilCodigo)
                || "CARGO".equals(perfilCodigo)
                || "AREA".equals(perfilCodigo)
                || "CURSO".equals(perfilCodigo)
                || "CONHECIMENTOS".equals(perfilCodigo)
                || "PROVA".equals(perfilCodigo);
    }

    @Override
    public EstruturaRascunho interpretar(DocumentModel documento, EditalProfile perfil) {
        if (documento == null || documento.vazio()) {
            return EstruturaRascunho.vazia(perfil, "Texto do edital está vazio.");
        }

        SegmentadorEdital.SegmentoEdital segmento =
                segmentador.segmentar(documento.textoLimpo());

        // Sem seção declarada o segmentador devolve o texto inteiro como
        // reserva; usá-lo faria a estrutura nascer de cronograma e de
        // cláusulas, então aqui a ausência é definitiva.
        if (!segmento.encontrado()) {
            log.debug("Seção de conteúdo programático ausente");
            return EstruturaRascunho.vazia(perfil,
                    "Não foi encontrada a seção de conteúdo programático deste edital.");
        }

        String bloco = segmento.conteudoProgramatico();

        if (bloco == null || bloco.isBlank()) {
            return EstruturaRascunho.vazia(perfil,
                    "A seção de conteúdo programático está vazia.");
        }

        List<MateriaRascunho> materias = extratorDisciplinas.extrair(documento, bloco);

        log.debug("EstrategiaBlocoConteudo | materias={}", materias.size());

        return new EstruturaRascunho(perfil, perfil.codigo(), materias, null);
    }
}