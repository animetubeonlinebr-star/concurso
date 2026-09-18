package br.com.marcosbassetto.concursos.domain.edital.interpretacao;

import br.com.marcosbassetto.concursos.domain.edital.interpretacao.documento.DocumentModel;
import br.com.marcosbassetto.concursos.domain.edital.interpretacao.estrategia.EstrategiaFactory;
import br.com.marcosbassetto.concursos.domain.edital.interpretacao.estrategia.EstrategiaInterpretacaoEdital;
import br.com.marcosbassetto.concursos.domain.edital.interpretacao.metadados.ExtratorMetadadosEdital;
import br.com.marcosbassetto.concursos.domain.edital.interpretacao.perfil.EditalProfile;
import br.com.marcosbassetto.concursos.domain.edital.interpretacao.perfil.EditalProfileClassifier;
import br.com.marcosbassetto.concursos.domain.edital.interpretacao.rascunho.EstruturaRascunho;
import br.com.marcosbassetto.concursos.domain.edital.interpretacao.validacao.AvaliadorQualidadeExtracao;
import org.springframework.stereotype.Service;

/**
 * Ponto de entrada da interpretação de edital.
 *
 * Herda o esqueleto de {@link InterpretadorEditalTemplate} e implementa o
 * único passo que varia: a leitura do conteúdo, delegada à
 * {@link EstrategiaInterpretacaoEdital} escolhida pelo perfil.
 *
 * A classe responde, na ordem fixada pelo template, às perguntas que antes
 * estavam misturadas dentro do serviço de staging:
 *
 * <ol>
 *   <li>"que tipo de edital é este?" — {@link EditalProfileClassifier};</li>
 *   <li>"o que extrair e como?" — a estratégia do perfil;</li>
 *   <li>"como mapear para a estrutura do sistema?" — o montador da estratégia;</li>
 *   <li>"o resultado é confiável?" — {@link AvaliadorQualidadeExtracao}.</li>
 * </ol>
 */
@Service
public class InterpretadorEditalFacade extends InterpretadorEditalTemplate {

    private final EstrategiaFactory estrategiaFactory;

    public InterpretadorEditalFacade(EditalProfileClassifier classificador,
                                     EstrategiaFactory estrategiaFactory,
                                     ExtratorMetadadosEdital extratorMetadados,
                                     AvaliadorQualidadeExtracao avaliador) {
        super(classificador, extratorMetadados, avaliador);
        this.estrategiaFactory = estrategiaFactory;
    }

    /**
     * A especialização do esqueleto é só isto: escolher a estratégia do perfil
     * e aplicá-la. Não há {@code if} por formato aqui — incluir um formato novo
     * é criar a estratégia e anotá-la, sem editar esta classe.
     */
    @Override
    protected EstruturaRascunho interpretarConteudo(DocumentModel documento, EditalProfile perfil) {
        return estrategiaFactory.criar(perfil.codigo()).interpretar(documento, perfil);
    }
}
