package br.com.marcosbassetto.concursos.domain.edital.service;

import br.com.marcosbassetto.concursos.domain.edital.dto.CursoExtraido;
import br.com.marcosbassetto.concursos.domain.edital.dto.EstruturaEditalDTO;
import br.com.marcosbassetto.concursos.domain.edital.dto.MateriaExtraida;
import br.com.marcosbassetto.concursos.domain.edital.dto.TopicoExtraido;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Higieniza a estrutura bruta devolvida pelos extratores antes de gravá-la
 * no staging: limpa nomes, descarta ruído e limita tamanhos/quantidades.
 *
 * Duplicatas NÃO são removidas aqui: quem decide se dois itens iguais devem
 * ser mesclados é o usuário, na revisão. {@link DetectorDuplicidade} apenas
 * sinaliza, e apagar uma matéria antes disso esconderia o problema dele.
 */
@Slf4j
@Component
public class NormalizadorEstrutura {

    public EstruturaEditalDTO normalizar(EstruturaEditalDTO estrutura) {
        if (estrutura == null) {
            return EstruturaEditalDTO.naoIdentificado("Estrutura não foi extraída.");
        }

        List<MateriaExtraida> materias = normalizarMaterias(estrutura.materiasDoPrimeiroCurso());

        log.debug("Normalização | materiasBrutas={} materiasNormalizadas={}",
                estrutura.materiasDoPrimeiroCurso().size(), materias.size());

        List<CursoExtraido> cursos = List.of(new CursoExtraido("Geral", null, materias));

        return new EstruturaEditalDTO(
                estrutura.dadosConcurso(), materias, cursos, estrutura.status(), estrutura.mensagem());
    }

    private List<MateriaExtraida> normalizarMaterias(List<MateriaExtraida> brutas) {
        List<MateriaExtraida> resultado = new ArrayList<>();

        for (MateriaExtraida m : brutas) {
            if (resultado.size() >= ExtracaoConstants.MAX_MATERIAS) break;

            String nome = limparNome(m.nome());
            if (!nomeValido(nome, ExtracaoConstants.MIN_MATERIA_LENGTH,
                    ExtracaoConstants.MAX_MATERIA_LENGTH,
                    ExtracaoConstants.MAX_PALAVRAS_MATERIA)) {
                continue;
            }

            resultado.add(new MateriaExtraida(nome, normalizarTopicos(m.topicos())));
        }

        return resultado;
    }

    private List<TopicoExtraido> normalizarTopicos(List<TopicoExtraido> brutos) {
        List<TopicoExtraido> resultado = new ArrayList<>();

        if (brutos == null) return resultado;

        for (TopicoExtraido t : brutos) {
            if (resultado.size() >= ExtracaoConstants.MAX_TOPICOS_POR_MATERIA) break;

            String nome = limparNome(t.nome());
            if (!nomeValido(nome, ExtracaoConstants.MIN_TOPICO_LENGTH,
                    ExtracaoConstants.MAX_TOPICO_LENGTH, Integer.MAX_VALUE)) {
                continue;
            }

            resultado.add(new TopicoExtraido(nome, t.codigo(), t.detalhe()));
        }

        return resultado;
    }

    private String limparNome(String nome) {
        if (nome == null) return null;

        return nome
                .replaceAll("\\s+", " ")
                .replaceAll("^[\\s•▪■◆\\-:;,.]+", "")
                .replaceAll("[\\s:;,.]+$", "")
                .strip();
    }

    private boolean nomeValido(String nome, int min, int max, int maxPalavras) {
        if (nome == null || nome.length() < min || nome.length() > max) {
            return false;
        }
        if (ExtracaoConstants.PADRAO_LINHA_TABELA.matcher(nome).find()) {
            return false;
        }
        return nome.split("\\s+").length <= maxPalavras;
    }
}
