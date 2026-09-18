package br.com.marcosbassetto.concursos.domain.edital.interpretacao.disciplina;

import br.com.marcosbassetto.concursos.domain.edital.interpretacao.classificacao.MontadorHierarquia;
import br.com.marcosbassetto.concursos.domain.edital.interpretacao.documento.DocumentModel;
import br.com.marcosbassetto.concursos.domain.edital.interpretacao.rascunho.MateriaRascunho;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Escolhe como ler as disciplinas de um bloco de conteúdo.
 *
 * O edital escreve as disciplinas de uma das duas formas, e a escolha é feita
 * por qual leitura reconhece mais disciplinas no próprio documento — não por
 * banca, órgão ou nome de arquivo.
 *
 * <ul>
 *   <li><b>linha própria</b> — a disciplina ocupa a linha e o conteúdo vem
 *       abaixo (a forma da maioria dos editais, lida por
 *       {@link MontadorHierarquia});</li>
 *   <li><b>inline</b> — a disciplina é prefixo na mesma linha do conteúdo, ou
 *       é a linha sozinha seguida de dois-pontos (lida por
 *       {@link ExtratorDisciplinasInline}).</li>
 * </ul>
 *
 * O empate vai para a leitura por linha própria, que é a já validada. Uma
 * leitura só é preterida quando a outra reconhece mais disciplinas — e a
 * contagem de disciplinas é o critério porque é justamente o que as duas
 * tentam produzir.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExtratorDisciplinas {

    private final MontadorHierarquia leituraPorLinhaPropria;
    private final ExtratorDisciplinasInline leituraInline;

    /**
     * Devolve as matérias do bloco na forma de rascunho.
     *
     * @param documento usado para a frequência de linhas (mobiliário de página)
     * @param bloco     conteúdo programático já recortado pelo segmentador
     */
    public List<MateriaRascunho> extrair(DocumentModel documento, String bloco) {
        if (bloco == null || bloco.isBlank()) {
            return List.of();
        }

        List<MateriaRascunho> porLinha = leituraPorLinhaPropria.montar(documento, bloco);
        List<MateriaRascunho> inline = leituraInline.extrair(bloco);

        if (inline.size() > porLinha.size()) {
            log.debug("Leitura inline reconheceu mais disciplinas | inline={} linhaPropria={}",
                    inline.size(), porLinha.size());
            return inline;
        }

        return porLinha;
    }
}
