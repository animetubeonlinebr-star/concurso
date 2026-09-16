package br.com.marcosbassetto.concursos.domain.edital.service;

import br.com.marcosbassetto.concursos.common.util.NomeNormalizer;
import br.com.marcosbassetto.concursos.domain.edital.dto.CursoExtraido;
import br.com.marcosbassetto.concursos.domain.edital.dto.EstruturaEditalDTO;
import br.com.marcosbassetto.concursos.domain.edital.dto.MateriaExtraida;
import br.com.marcosbassetto.concursos.domain.edital.dto.TopicoExtraido;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Higieniza a estrutura bruta devolvida pelos extratores antes de gravá-la
 * no staging: descarta ruído, limita tamanhos/quantidades e remove
 * duplicatas dentro da mesma extração.
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
        Set<String> chaves = new LinkedHashSet<>();

        for (MateriaExtraida m : brutas) {
            if (resultado.size() >= ExtracaoConstants.MAX_MATERIAS) break;

            String nome = limparNome(m.nome());
            if (!nomeValido(nome, ExtracaoConstants.MIN_MATERIA_LENGTH,
                    ExtracaoConstants.MAX_MATERIA_LENGTH,
                    ExtracaoConstants.MAX_PALAVRAS_MATERIA)) {
                continue;
            }

            String chave = NomeNormalizer.normalizar(nome);
            if (!chaves.add(chave)) {
                log.debug("Matéria duplicada na extração, descartada: {}", nome);
                continue;
            }

            resultado.add(new MateriaExtraida(nome, normalizarTopicos(m.topicos())));
        }

        return resultado;
    }

    private List<TopicoExtraido> normalizarTopicos(List<TopicoExtraido> brutos) {
        List<TopicoExtraido> resultado = new ArrayList<>();
        Set<String> chaves = new LinkedHashSet<>();

        if (brutos == null) return resultado;

        for (TopicoExtraido t : brutos) {
            if (resultado.size() >= ExtracaoConstants.MAX_TOPICOS_POR_MATERIA) break;

            String nome = limparNome(t.nome());
            if (!nomeValido(nome, ExtracaoConstants.MIN_TOPICO_LENGTH,
                    ExtracaoConstants.MAX_TOPICO_LENGTH, Integer.MAX_VALUE)) {
                continue;
            }

            String chave = NomeNormalizer.normalizar(nome);
            if (!chaves.add(chave)) {
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
