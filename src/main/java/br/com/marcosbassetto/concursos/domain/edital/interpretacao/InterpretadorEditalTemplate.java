package br.com.marcosbassetto.concursos.domain.edital.interpretacao;

import br.com.marcosbassetto.concursos.domain.edital.dto.CursoExtraido;
import br.com.marcosbassetto.concursos.domain.edital.dto.DadosConcurso;
import br.com.marcosbassetto.concursos.domain.edital.dto.EstruturaEditalDTO;
import br.com.marcosbassetto.concursos.domain.edital.dto.MateriaExtraida;
import br.com.marcosbassetto.concursos.domain.edital.dto.StatusExtracao;
import br.com.marcosbassetto.concursos.domain.edital.dto.TopicoExtraido;
import br.com.marcosbassetto.concursos.domain.edital.interpretacao.documento.DocumentModel;
import br.com.marcosbassetto.concursos.domain.edital.interpretacao.metadados.ExtratorMetadadosEdital;
import br.com.marcosbassetto.concursos.domain.edital.interpretacao.perfil.EditalProfile;
import br.com.marcosbassetto.concursos.domain.edital.interpretacao.perfil.EditalProfileClassifier;
import br.com.marcosbassetto.concursos.domain.edital.interpretacao.rascunho.EstruturaRascunho;
import br.com.marcosbassetto.concursos.domain.edital.interpretacao.rascunho.MateriaRascunho;
import br.com.marcosbassetto.concursos.domain.edital.interpretacao.rascunho.TopicoRascunho;
import br.com.marcosbassetto.concursos.domain.edital.interpretacao.validacao.AvaliadorQualidadeExtracao;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Esqueleto invariante da interpretação de edital (Template Method).
 *
 * A medição de 23 editais reais mostrou que a interpretação tem uma parte que
 * nunca muda e uma que muda. Esta classe fixa a primeira:
 *
 * <ol>
 *   <li>texto vazio → {@code NAO_IDENTIFICADO}, sem tocar no documento;</li>
 *   <li>o documento vira {@link DocumentModel};</li>
 *   <li>o perfil é classificado;</li>
 *   <li><b>o conteúdo é interpretado</b> — único passo variável;</li>
 *   <li>os metadados cadastrais são extraídos;</li>
 *   <li>a qualidade da extração é avaliada;</li>
 *   <li>a resposta é montada com o caminho de curso implícito.</li>
 * </ol>
 *
 * A ordem é {@code final} de propósito. Um perfil novo não pode pular a
 * avaliação de qualidade nem deixar de extrair metadados — foi exatamente o
 * que aconteceu antes, quando o status calculado pelo extrator era descartado
 * e um edital sem conteúdo chegava à revisão como sucesso vazio.
 *
 * O único ponto que se especializa é {@link #interpretarConteudo}. É onde os
 * formatos de edital realmente divergem: a hierarquia (cargo, área, curso,
 * conhecimentos) é <i>dado</i> carregado pelo {@link EditalProfile}, não
 * código; o que muda em código é como o bloco de conteúdo é recortado e lido.
 */
@Slf4j
public abstract class InterpretadorEditalTemplate {

    private static final String TEXTO_VAZIO = "Texto do edital está vazio.";

    private final EditalProfileClassifier classificador;
    private final ExtratorMetadadosEdital extratorMetadados;
    private final AvaliadorQualidadeExtracao avaliador;

    protected InterpretadorEditalTemplate(EditalProfileClassifier classificador,
                                          ExtratorMetadadosEdital extratorMetadados,
                                          AvaliadorQualidadeExtracao avaliador) {
        this.classificador = classificador;
        this.extratorMetadados = extratorMetadados;
        this.avaliador = avaliador;
    }

    public final EstruturaEditalDTO interpretar(String texto) {
        if (texto == null || texto.isBlank()) {
            return EstruturaEditalDTO.naoIdentificado(TEXTO_VAZIO);
        }

        DocumentModel documento = DocumentModel.de(texto);

        EditalProfile perfil = classificador.classificar(documento);

        EstruturaRascunho rascunho = interpretarConteudo(documento, perfil);

        DadosConcurso dados = extratorMetadados.extrair(documento);

        StatusExtracao status = avaliador.avaliar(rascunho, dadosCompletos(dados));

        log.info("Edital interpretado | perfil={} | status={} | materias={} | topicos={}",
                perfil.codigo(), status, rascunho.materias().size(), rascunho.totalTopicos());

        return montarResposta(dados, rascunho, status);
    }

    /**
     * Passo variável: como este documento escreve disciplinas e tópicos.
     *
     * Quem implementa escolhe a leitura adequada ao perfil. Um formato novo de
     * edital entra aqui — e só aqui.
     */
    protected abstract EstruturaRascunho interpretarConteudo(DocumentModel documento,
                                                             EditalProfile perfil);

    protected EditalProfile classificar(DocumentModel documento) {
        return classificador.classificar(documento);
    }

    protected DadosConcurso extrairMetadados(DocumentModel documento) {
        return extratorMetadados.extrair(documento);
    }

    protected StatusExtracao avaliar(EstruturaRascunho rascunho, DadosConcurso dados) {
        return avaliador.avaliar(rascunho, dadosCompletos(dados));
    }

    /**
     * A saída é sempre {@link EstruturaEditalDTO}: a API, o staging e o
     * frontend não precisam saber como a interpretação foi feita por dentro.
     * O curso "Geral" continua sendo o balde implícito enquanto a decisão
     * sobre a camada CURSO estiver pendente.
     */
    protected EstruturaEditalDTO montarResposta(DadosConcurso dados,
                                                EstruturaRascunho rascunho,
                                                StatusExtracao status) {
        List<MateriaExtraida> materias = converter(rascunho);

        String mensagem = rascunho.mensagem() != null
                ? rascunho.mensagem()
                : (status.exigeAtencao() ? status.getMensagem() : null);

        return new EstruturaEditalDTO(
                dados,
                materias,
                List.of(new CursoExtraido("Geral", null, materias)),
                status,
                mensagem);
    }

    private boolean dadosCompletos(DadosConcurso dados) {
        return dados != null
                && dados.orgao() != null && !dados.orgao().isBlank()
                && dados.ano() != null;
    }

    private List<MateriaExtraida> converter(EstruturaRascunho rascunho) {
        return rascunho.materias().stream()
                .map(this::converter)
                .toList();
    }

    private MateriaExtraida converter(MateriaRascunho materia) {
        List<TopicoExtraido> topicos = materia.topicos().stream()
                .map(this::converter)
                .toList();

        return new MateriaExtraida(materia.nome(), topicos);
    }

    private TopicoExtraido converter(TopicoRascunho topico) {
        return new TopicoExtraido(topico.nome(), topico.codigo(), null);
    }
}
