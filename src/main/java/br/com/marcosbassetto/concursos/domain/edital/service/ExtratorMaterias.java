package br.com.marcosbassetto.concursos.domain.edital.service;

import br.com.marcosbassetto.concursos.domain.edital.dto.MateriaExtraida;
import br.com.marcosbassetto.concursos.domain.edital.dto.TopicoExtraido;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


@Deprecated(since = "migração Template+Strategy")
@Slf4j
@Service
@RequiredArgsConstructor
public class ExtratorMaterias {

    private final ExtratorTopicos extratorTopicos;

    private static final List<String> MARCADORES_INICIO = List.of(
            "CONTEÚDO PROGRAMÁTICO",
            "CONTEUDO PROGRAMATICO",
            "CONTEÚDOS PROGRAMÁTICOS",
            "CONTEUDOS PROGRAMATICOS",
            "PROGRAMA DE PROVAS",
            "PROGRAMA DAS PROVAS",
            "ANEXO I - CONTEÚDO PROGRAMÁTICO",
            "ANEXO - CONTEÚDO PROGRAMÁTICO",
            "ANEXO ÚNICO - CONTEÚDO PROGRAMÁTICO"
    );

    private static final List<String> MARCADORES_FIM = List.of(
            "ANEXO I", "ANEXO II", "ANEXO III", "ANEXO IV", "ANEXO V",
            "ANEXO VI", "ANEXO VII", "ANEXO VIII",
            "CRONOGRAMA", "DAS PROVAS", "DOS RECURSOS", "DAS INSCRIÇÕES",
            "REQUISITOS PARA", "REQUISITOS DO CARGO", "ATRIBUIÇÕES DO CARGO",
            "REMUNERAÇÃO", "JORNADA DE TRABALHO", "QUADRO DE VAGAS",
            "DAS VAGAS", "BENEFÍCIOS", "DISPOSIÇÕES GERAIS", "DISPOSIÇÕES FINAIS"
    );

    private static final List<String> MATERIAS_CONHECIDAS = List.of(
            "LÍNGUA PORTUGUESA", "DIREITO CONSTITUCIONAL", "DIREITO ADMINISTRATIVO",
            "DIREITO PENAL", "DIREITO PROCESSUAL PENAL", "DIREITO CIVIL",
            "DIREITO PROCESSUAL CIVIL", "DIREITO TRIBUTÁRIO", "DIREITO FINANCEIRO",
            "DIREITO AMBIENTAL", "DIREITO DO TRABALHO", "DIREITO PREVIDENCIÁRIO",
            "INFORMÁTICA", "RACIOCÍNIO LÓGICO", "MATEMÁTICA", "ESTATÍSTICA",
            "CONTABILIDADE", "ADMINISTRAÇÃO", "ADMINISTRAÇÃO PÚBLICA",
            "GESTÃO DE PESSOAS", "ÉTICA", "LEGISLAÇÃO", "LEGISLAÇÃO APLICADA",
            "LEGISLAÇÃO ESPECIAL", "CONHECIMENTOS ESPECÍFICOS",
            "CONHECIMENTOS GERAIS", "ATUALIDADES", "HISTÓRIA", "GEOGRAFIA",
            "SOCIOLOGIA", "FILOSOFIA", "INGLÊS", "ESPANHOL", "ENFERMAGEM",
            "FARMÁCIA", "NUTRIÇÃO", "PSICOLOGIA", "SERVIÇO SOCIAL",
            "FISIOTERAPIA", "ODONTOLOGIA", "MEDICINA", "VETERINÁRIA",
            "SAÚDE PÚBLICA", "TÉCNICO EM ENFERMAGEM"
    );

    private static final Pattern PADRAO_SO_NUMEROS_ROMANOS =
            Pattern.compile("^[IVXLCDM\\d\\.\\s\\-]+$");

    public List<MateriaExtraida> extrair(String texto) {
        String bloco = localizarBlocoConteudoProgramatico(texto);
        if (bloco == null || bloco.isBlank()) {
            log.warn("⚠️ Bloco de conteúdo programático não localizado. Retornando vazio.");
            return new ArrayList<>();
        }

        List<String> linhas = normalizarLinhas(bloco);
        log.info("📚 {} linhas não vazias no bloco selecionado", linhas.size());

        if (log.isDebugEnabled()) {
            log.debug("📋 Primeiras 40 linhas do bloco:");
            linhas.stream().limit(40).forEach(l ->
                    log.debug("    | {}",
                            l.length() > 130 ? l.substring(0, 127) + "..." : l));
        }

        List<MateriaExtraida> materias = segmentarEmMaterias(linhas);

        List<MateriaExtraida> validas = materias.stream()
                .filter(m -> m.topicos() != null && !m.topicos().isEmpty())
                .limit(ExtracaoConstants.MAX_MATERIAS)
                .collect(Collectors.toList());

        validas.forEach(m ->
                log.debug("   ✓ {} → {} tópicos", m.nome(), m.topicos().size()));

        log.info("📚 {} matérias válidas extraídas", validas.size());
        return validas;
    }

    private String localizarBlocoConteudoProgramatico(String texto) {
        String upper = texto.toUpperCase();

        List<Integer> posicoes = new ArrayList<>();
        for (String marcador : MARCADORES_INICIO) {
            int fromIndex = 0;
            while (true) {
                int idx = upper.indexOf(marcador, fromIndex);
                if (idx < 0) break;
                posicoes.add(idx + marcador.length());
                fromIndex = idx + 1;
            }
        }

        if (posicoes.isEmpty()) {
            log.warn("⚠️ Nenhum marcador de início encontrado");
            return null;
        }

        log.debug("📌 {} ocorrências de marcadores de início encontradas", posicoes.size());

        String melhorBloco = null;
        int melhorTamanho = 0;
        int melhorPosicao = -1;

        for (int posicao : posicoes) {
            String bloco = extrairBlocoAPartirDe(texto, posicao);
            if (bloco != null && bloco.length() > melhorTamanho) {
                melhorTamanho = bloco.length();
                melhorBloco = bloco;
                melhorPosicao = posicao;
            }
        }

        if (melhorBloco == null || melhorTamanho < 1000) {
            log.warn("⚠️ Nenhum bloco de conteúdo significativo encontrado (maior: {} chars)",
                    melhorTamanho);
            return null;
        }

        log.info("📚 Melhor bloco: {} caracteres (posição {})",
                melhorTamanho, melhorPosicao);
        return melhorBloco;
    }

    private String extrairBlocoAPartirDe(String texto, int inicio) {
        if (inicio < 0 || inicio >= texto.length()) return null;

        String bloco = texto.substring(inicio);
        String blocoUpper = bloco.toUpperCase();

        final int MIN_BLOCO_ANTES_DE_PARAR = 1000;

        int fim = bloco.length();
        for (String marcador : MARCADORES_FIM) {
            int idx = blocoUpper.indexOf(marcador);
            if (idx > MIN_BLOCO_ANTES_DE_PARAR && idx < fim) {
                fim = idx;
            }
        }
        return bloco.substring(0, fim);
    }

    private List<String> normalizarLinhas(String texto) {
        return Arrays.stream(texto.split("\\r?\\n"))
                .map(String::trim)
                .map(l -> l.replaceAll("[ \\t]+", " "))
                .filter(l -> !l.isEmpty())
                .collect(Collectors.toList());
    }

    private List<MateriaExtraida> segmentarEmMaterias(List<String> linhas) {
        List<MateriaExtraida> materias = new ArrayList<>();

        String materiaAtual = null;
        // ⚠️ MUDANÇA: List<TopicoExtraido> em vez de List<String>
        List<TopicoExtraido> topicosAtuais = new ArrayList<>();
        int processadas = 0;

        for (String linha : linhas) {
            if (processadas++ > ExtracaoConstants.MAX_LINHAS_PROCESSADAS) {
                log.warn("⚠️ Limite de {} linhas atingido.",
                        ExtracaoConstants.MAX_LINHAS_PROCESSADAS);
                break;
            }

            if (ehCabecalhoMateria(linha)) {
                if (materiaAtual != null && !topicosAtuais.isEmpty()) {
                    materias.add(new MateriaExtraida(materiaAtual,
                            new ArrayList<>(topicosAtuais)));
                    if (materias.size() >= ExtracaoConstants.MAX_MATERIAS) break;
                }
                materiaAtual = normalizarNomeMateria(linha);
                topicosAtuais = new ArrayList<>();
                continue;
            }

            if (materiaAtual != null) {

                String topicoStr = extratorTopicos.extrairTopicoDaLinha(linha);
                if (topicoStr != null
                        && topicosAtuais.size() < ExtracaoConstants.MAX_TOPICOS_POR_MATERIA) {
                    topicosAtuais.add(new TopicoExtraido(topicoStr, null, null));
                }
            }
        }

        if (materiaAtual != null
                && !topicosAtuais.isEmpty()
                && materias.size() < ExtracaoConstants.MAX_MATERIAS) {
            materias.add(new MateriaExtraida(materiaAtual,
                    new ArrayList<>(topicosAtuais)));
        }

        return materias;
    }

    private boolean ehCabecalhoMateria(String linha) {
        if (linha == null) return false;
        String s = linha.trim();
        if (s.isEmpty()) return false;

        String upper = s.toUpperCase();

        for (String conhecida : MATERIAS_CONHECIDAS) {

            if (upper.equals(conhecida)) return true;

            if (upper.startsWith(conhecida)) {
                String resto = s.substring(conhecida.length()).trim();

                if (resto.isEmpty() || resto.matches("^[:\\-\\s]{1,3}$")) {
                    return true;
                }

                return false;
            }
        }

        if (s.length() < ExtracaoConstants.MIN_MATERIA_LENGTH) return false;
        if (s.length() > ExtracaoConstants.MAX_MATERIA_LENGTH) return false;
        if (ExtracaoConstants.PADRAO_LINHA_TABELA.matcher(s).find()) return false;
        if (PADRAO_SO_NUMEROS_ROMANOS.matcher(s).matches()) return false;
        if (s.endsWith(".") || s.endsWith(":") || s.endsWith(";") || s.endsWith(",")) {
            return false;
        }

        String[] palavras = s.split("\\s+");
        if (palavras.length < 1 || palavras.length > ExtracaoConstants.MAX_PALAVRAS_MATERIA) {
            return false;
        }

        return s.equals(s.toUpperCase());
    }

    private String normalizarNomeMateria(String linha) {
        String s = linha.trim();
        String upper = s.toUpperCase();

        for (String conhecida : MATERIAS_CONHECIDAS) {
            if (upper.equals(conhecida) || upper.startsWith(conhecida)) {
                return conhecida;
            }
        }

        String limpo = s.replaceAll("\\s+", " ");
        return limpo.length() > ExtracaoConstants.MAX_MATERIA_LENGTH
                ? limpo.substring(0, ExtracaoConstants.MAX_MATERIA_LENGTH - 3) + "..."
                : limpo;
    }
}
