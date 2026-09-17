package br.com.marcosbassetto.concursos.domain.edital.interpretacao;

import br.com.marcosbassetto.concursos.domain.edital.dto.CursoExtraido;
import br.com.marcosbassetto.concursos.domain.edital.dto.DadosConcurso;
import br.com.marcosbassetto.concursos.domain.edital.dto.EstruturaEditalDTO;
import br.com.marcosbassetto.concursos.domain.edital.dto.MateriaExtraida;
import br.com.marcosbassetto.concursos.domain.edital.dto.StatusExtracao;
import br.com.marcosbassetto.concursos.domain.edital.dto.TopicoExtraido;
import br.com.marcosbassetto.concursos.domain.edital.interpretacao.documento.DocumentModel;
import br.com.marcosbassetto.concursos.domain.edital.interpretacao.estrategia.EstrategiaFactory;
import br.com.marcosbassetto.concursos.domain.edital.interpretacao.estrategia.EstrategiaInterpretacaoEdital;
import br.com.marcosbassetto.concursos.domain.edital.interpretacao.metadados.ExtratorMetadadosEdital;
import br.com.marcosbassetto.concursos.domain.edital.interpretacao.perfil.EditalProfile;
import br.com.marcosbassetto.concursos.domain.edital.interpretacao.perfil.EditalProfileClassifier;
import br.com.marcosbassetto.concursos.domain.edital.interpretacao.rascunho.EstruturaRascunho;
import br.com.marcosbassetto.concursos.domain.edital.interpretacao.rascunho.MateriaRascunho;
import br.com.marcosbassetto.concursos.domain.edital.interpretacao.rascunho.TopicoRascunho;
import br.com.marcosbassetto.concursos.domain.edital.interpretacao.validacao.AvaliadorQualidadeExtracao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Ponto de entrada da interpretação de edital.
 *
 * Orquestra, nesta ordem, as perguntas que antes estavam misturadas dentro do
 * serviço de staging:
 *
 * <ol>
 *   <li>"que tipo de edital é este?" — {@link EditalProfileClassifier};</li>
 *   <li>"como este edital organiza a informação?" — o perfil devolvido;</li>
 *   <li>"o que extrair?" — a {@link EstrategiaInterpretacaoEdital} escolhida;</li>
 *   <li>"como mapear para a estrutura do sistema?" — {@code MontadorHierarquia};</li>
 *   <li>"o resultado é confiável?" — {@link AvaliadorQualidadeExtracao}.</li>
 * </ol>
 *
 * A saída continua sendo {@link EstruturaEditalDTO}: a API, o staging e o
 * frontend não precisam saber que a interpretação foi reescrita.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InterpretadorEditalFacade {

    private final EditalProfileClassifier classificadorPerfil;
    private final EstrategiaFactory estrategiaFactory;
    private final ExtratorMetadadosEdital extratorMetadados;
    private final AvaliadorQualidadeExtracao avaliador;

    public EstruturaEditalDTO interpretar(String texto) {
        if (texto == null || texto.isBlank()) {
            return EstruturaEditalDTO.naoIdentificado("Texto do edital está vazio.");
        }

        DocumentModel documento = DocumentModel.de(texto);

        EditalProfile perfil = classificadorPerfil.classificar(documento);

        EstrategiaInterpretacaoEdital estrategia = estrategiaFactory.criar(perfil.codigo());
        EstruturaRascunho rascunho = estrategia.interpretar(documento, perfil);

        DadosConcurso dados = extratorMetadados.extrair(documento);

        StatusExtracao status = avaliador.avaliar(rascunho, dadosCompletos(dados));

        List<MateriaExtraida> materias = converter(rascunho);

        String mensagem = rascunho.mensagem() != null
                ? rascunho.mensagem()
                : (status.exigeAtencao() ? status.getMensagem() : null);

        log.info("Edital interpretado | perfil={} | status={} | materias={} | topicos={}",
                perfil.codigo(), status, materias.size(),
                materias.stream().mapToInt(m -> m.topicos().size()).sum());

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