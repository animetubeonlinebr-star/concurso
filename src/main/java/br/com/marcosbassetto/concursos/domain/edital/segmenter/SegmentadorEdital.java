package br.com.marcosbassetto.concursos.domain.edital.segmenter;

import br.com.marcosbassetto.concursos.domain.edital.interpretacao.documento.DocumentModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Recorta o bloco de conteúdo programático do edital.
 *
 * O bloco é a fronteira do que pode virar matéria: fora dele o texto trata de
 * inscrição, prova, cronograma e contrato, e extrair dali produz estruturas
 * falsas.
 *
 * O começo do bloco é decidido por {@link AncoraBlocoConteudo}: os editais
 * reais ancoram o mesmo conteúdo de maneiras diferentes (título de anexo,
 * agrupamento de conhecimentos, título numerado dentro de um capítulo), e cada
 * forma tem sua própria implementação em vez de um único regex que tenta
 * adivinhar todas. O fim também varia: o bloco ancorado em título numerado
 * termina na mudança de capítulo, não em um anexo.
 */
@Slf4j
@Component
public class SegmentadorEdital {

    /**
     * Seções que encerram o conteúdo. Um novo anexo, o cronograma ou as
     * disposições finais marcam o fim do bloco.
     */
    private static final Pattern FIM_CONTEUDO = Pattern.compile(
            "(?im)^\\s*"
                    + "(?:"
                    + "ANEXO\\s+[IVXL]+\\b"
                    + "|DISPOSI[ÇC][ÕO]ES\\s+FINAIS"
                    + "|CRONOGRAMA"
                    + "|REFER[ÊE]NCIAS?\\s+BIBLIOGR[ÁA]FICAS?"
                    + "|BIBLIOGRAFIA"
                    + "|CALEND[ÁA]RIO"
                    + ")"
                    + "\\s*$");

    /**
     * Fim alternativo para o bloco ancorado em título numerado.
     *
     * Esses editais não fecham o conteúdo com um anexo: o bloco termina quando
     * o capítulo muda de assunto, em um título numerado que <i>não</i> é de
     * conhecimentos — "16 DAS DISPOSIÇÕES GERAIS", "17 DO RESULTADO". Sem esta
     * regra o bloco seguiria até o fim do documento e o capítulo seguinte
     * viraria conteúdo programático.
     *
     * O nível é raso de propósito: exige um único número ("16"), não uma
     * hierarquia ("15.2.4"). Um item de conteúdo é numerado em profundidade
     * ("2.5.3"), então não é confundido com a mudança de capítulo.
     */
    private static final Pattern FIM_CAPITULO_NUMERADO = Pattern.compile(
            "(?im)^\\s*\\d{1,2}(?:\\.\\d{1,2})?\\s+(?:D[AEOS]\\s+)?[A-ZÁÉÍÓÚÂÊÔÃÕÇ][A-ZÁÉÍÓÚÂÊÔÃÕÇ\\s]{3,}$");

    private final List<AncoraBlocoConteudo> ancoras;

    public SegmentadorEdital(List<AncoraBlocoConteudo> ancoras) {
        this.ancoras = ancoras.stream()
                .sorted(Comparator.comparingInt(AncoraBlocoConteudo::ordem))
                .toList();
    }

    /**
     * Segmentador com as âncoras que a aplicação registra, para uso fora do
     * contexto Spring (testes e ferramentas de diagnóstico).
     */
    public static SegmentadorEdital comAncorasPadrao() {
        return new SegmentadorEdital(List.of(
                new AncoraTituloExplicito(),
                new AncoraAgrupamentoConhecimentos(),
                new AncoraTituloNumerado()));
    }

    public SegmentoEdital segmentar(String textoCompleto) {
        if (textoCompleto == null || textoCompleto.isBlank()) {
            return new SegmentoEdital("", "", false);
        }

        String cabecalho = extrairCabecalho(textoCompleto);

        DocumentModel documento = DocumentModel.de(textoCompleto);

        Inicio inicio = encontrarInicio(documento);

        if (inicio.posicao() < 0) {
            return new SegmentoEdital(cabecalho, "", false);
        }

        int posicaoFim = encontrarFim(textoCompleto, inicio.posicao(), inicio.ancora());

        String conteudo = textoCompleto.substring(inicio.posicao(), posicaoFim).trim();

        return new SegmentoEdital(cabecalho, conteudo, true);
    }

    /**
     * Tenta as âncoras na ordem de preferência e devolve a primeira que
     * reconhece o documento, junto do seu nome para orientar o critério de fim.
     */
    private Inicio encontrarInicio(DocumentModel documento) {
        for (AncoraBlocoConteudo ancora : ancoras) {
            int posicao = ancora.encontrarInicio(documento);
            if (posicao >= 0) {
                log.debug("Âncora de conteúdo encontrada | ancora={} | posicao={}",
                        ancora.nome(), posicao);
                return new Inicio(posicao, ancora.nome());
            }
        }
        return new Inicio(-1, null);
    }

    private int encontrarFim(String texto, int posicaoInicio, String ancora) {
        Matcher fim = FIM_CONTEUDO.matcher(texto);
        int posicao = fim.find(posicaoInicio) ? fim.start() : texto.length();

        // O bloco ancorado em título numerado não é fechado por anexo; ele
        // termina na mudança de capítulo.
        if (AncoraTituloNumerado.NOME.equals(ancora)) {
            Matcher capitulo = FIM_CAPITULO_NUMERADO.matcher(texto);
            if (capitulo.find(posicaoInicio) && capitulo.start() < posicao) {
                posicao = capitulo.start();
            }
        }

        return posicao;
    }

    private String extrairCabecalho(String texto) {
        int limite = Math.min(texto.length(), 3000);
        return texto.substring(0, limite);
    }

    private record Inicio(int posicao, String ancora) {
    }

    public record SegmentoEdital(
            String cabecalho,
            String conteudoProgramatico,
            boolean encontrado
    ) {}
}