package br.com.marcosbassetto.concursos.domain.edital.interpretacao.classificacao;

import br.com.marcosbassetto.concursos.domain.edital.interpretacao.documento.DocumentModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Decide o papel de uma linha na hierarquia do edital.
 *
 * É a peça que faltava entre "encontrei uma linha em destaque" e "isto é uma
 * matéria". Antes, qualquer linha em maiúsculas virava matéria, e o resultado
 * trazia "UNIVERSIDADE ESTADUAL PAULISTA", "COMUNICADO" e "VALOR DE" como se
 * fossem disciplinas.
 *
 * O princípio é exigir evidência positiva: uma linha só é título de disciplina
 * se tiver forma de título e estiver isolada do parágrafo anterior. Letras
 * maiúsculas NÃO são usadas como critério, porque editais reais escrevem as
 * disciplinas tanto em caixa alta ("LÍNGUA PORTUGUESA") quanto em caixa mista
 * ("Língua Portuguesa") — e exigir caixa alta descartaria metade dos casos.
 */
@Slf4j
@Component
public class ClassificadorUnidadeEdital {

    /** Marcadores de item de conteúdo. */
    private static final Pattern ORDINAL = Pattern.compile(
            "^\\s*(?:[▪■◆•\\-*]|[IVXLCDM]{1,6}\\.|\\d{1,3}[.)])\\s+\\S.*");

    /** Agrupamento explícito de conteúdo. */
    private static final Pattern CONHECIMENTOS = Pattern.compile(
            "^CONHECIMENTOS\\s+(GERAIS|ESPECIFICOS|ESPECÍFICOS|BASICOS|BÁSICOS|COMPLEMENTARES)\\b.*");

    /**
     * Agrupamento por escolaridade/cargo — não é disciplina, mas organiza o
     * conteúdo em blocos ("CARGOS DE ENSINO MÉDIO / TÉCNICO COMPLETO").
     */
    private static final Pattern NIVEL_ESCOLARIDADE = Pattern.compile(
            "^(CARGOS?\\s+DE\\s+|NIVEIS?\\s+DE\\s+|N[ÍI]VEIS?\\s+DE\\s+)?"
                    + "(ENSINO\\s+(MEDIO|MÉDIO|FUNDAMENTAL|SUPERIOR|TECNICO|TÉCNICO)"
                    + "|NIVEL\\s+(MEDIO|MÉDIO|FUNDAMENTAL|SUPERIOR|TECNICO|TÉCNICO)"
                    + "|N[ÍI]VEL\\s+(MEDIO|MÉDIO|FUNDAMENTAL|SUPERIOR|TECNICO|TÉCNICO))"
                    + "(\\s+COMPLETO)?\\b.*");

    /**
     * Unidade de nível 1 declarada com rótulo e valor. O número entre o rótulo
     * e os dois-pontos é opcional porque editais escrevem das duas formas
     * ("CARGO: AGENTE" e "CARGO 4: CONTADOR").
     */
    private static final Pattern ROTULO_UNIDADE = Pattern.compile(
            "^(CARGO|EMPREGO|FUNCAO|FUNÇÃO|CURSO|HABILITACAO|HABILITAÇÃO|"
                    + "AREA|ÁREA|ESPECIALIDADE|ENFASE|ÊNFASE|PROVA|MODULO|MÓDULO|EIXO)"
                    + "\\s*\\d*\\s*[:\\-].*");

    /**
     * Conectivos que não podem encerrar um título de disciplina. Como o PDFBox
     * corta parágrafos sem aviso, sobram fragmentos como "VALOR DE" e
     * "CONFORME A" — uma disciplina de verdade não termina em preposição,
     * artigo ou conjunção.
     */
    private static final Pattern CONECTIVO_FINAL = Pattern.compile(
            ".*\\b(DE|DA|DO|DAS|DOS|EM|NO|NA|NOS|NAS|PARA|POR|COM|SEM|E|OU|"
                    + "AO|AOS|A|AS|À|ÀS|O|OS|UM|UMA|SOB|ENTRE|SOBRE)\\s*$");

    /**
     * Título estrutural do documento. Não é conteúdo programático: separa
     * blocos, e tratá-lo como disciplina produz "CRONOGRAMA" como matéria.
     */
    private static final Pattern SECAO = Pattern.compile(
            "^(ANEXO\\b|APENDICE|APÊNDICE|CAPITULO|CAPÍTULO|SECAO|SEÇÃO|TITULO|TÍTULO|"
                    + "CLAUSULA|CLÁUSULA|DISPOSICOES|DISPOSIÇÕES|CRONOGRAMA|GABARITO|"
                    + "RESULTADO|RETIFICACAO|RETIFICAÇÃO|ERRATA|AVISO|COMUNICADO|EDITAL|"
                    + "INSTRUCOES|INSTRUÇÕES|OBSERVACOES|OBSERVAÇÕES|REFERENCIAS|REFERÊNCIAS|"
                    + "BIBLIOGRAFIA|SUMARIO|SUMÁRIO|INDICE|ÍNDICE|FORMULARIO|FORMULÁRIO|"
                    + "DECLARACAO|DECLARAÇÃO|ATIVIDADE|ETAPAS?|QUESITOS?|PERIODO|PERÍODO|"
                    + "DAS?\\s+DISPOSI|CALENDARIO|CALENDÁRIO|REQUISITOS|"
                    + "CANDIDATOS?|CRITERIOS|CRITÉRIOS)\\b.*");

    /** Seção numerada ("3.2 DA PROVA OBJETIVA", "11. DAS PROVAS"). */
    private static final Pattern SECAO_NUMERADA = Pattern.compile(
            "^\\d{1,2}(?:\\.\\d{1,2})*\\.?\\s+(?:D[AEOS]\\s+)?[A-ZÁÉÍÓÚÂÊÔÃÕÇ].*");

    /**
     * Vocabulário institucional: uma linha com estes termos costuma ser o
     * cabeçalho do órgão, repetido no topo de cada página.
     */
    private static final Pattern INSTITUICAO = Pattern.compile(
            "\\b(UNIVERSIDADE|FACULDADE|CENTRO UNIVERSITARIO|CENTRO UNIVERSITÁRIO|"
                    + "INSTITUTO|TRIBUNAL|MINISTERIO|MINISTÉRIO|PREFEITURA|CAMARA|CÂMARA|"
                    + "ASSEMBLEIA|COMPANHIA|FUNDACAO|FUNDAÇÃO|SECRETARIA|REITORIA|"
                    + "CONSELHO|CONSORCIO|CONSÓRCIO|AUTARQUIA|DEPARTAMENTO|"
                    + "PODER JUDICIARIO|PODER JUDICIÁRIO)\\b.*");

    /** Um título de disciplina é curto. */
    private static final int MAXIMO_TITULO = 70;

    private static final int MAXIMO_PALAVRAS_TITULO = 10;

    /** Menos que isso não nomeia disciplina ("VALOR DE", "CADA TÍTULO"). */
    private static final int MINIMO_TITULO = 5;

    /** Fim de frase: prova que a linha anterior não foi cortada no meio. */
    private static final Pattern FIM_DE_FRASE = Pattern.compile(".*[.;:!?]\\s*$");

    /**
     * Repetições a partir das quais uma linha é mobiliário de página — cabeçalho
     * ou rodapé. O limiar é alto de propósito: uma disciplina pode repetir duas
     * ou três vezes no mesmo edital (uma vez por cargo), mas cabeçalho e rodapé
     * repetem em quase todas as páginas.
     */
    private static final int REPETICOES_MOBILIARIO = 5;

    /**
     * Repetições a partir das quais uma linha com vocabulário institucional é
     * cabeçalho. Como o termo já é forte por si, o limiar é menor.
     */
    private static final int REPETICOES_CABECALHO = 2;

    /**
     * Classifica a linha considerando o documento e a linha anterior.
     *
     * @param linhaAnterior última linha não vazia antes desta, ou null
     */
    public TipoUnidadeEdital classificar(String linha,
                                         String linhaAnterior,
                                         Map<String, Integer> frequencia) {
        String texto = normalizar(linha);

        if (texto.isEmpty()) {
            return TipoUnidadeEdital.TOPICO;
        }

        // Do sinal mais específico ao mais genérico: um item de lista continua
        // sendo item mesmo que cite uma instituição.
        if (ORDINAL.matcher(texto).matches()) {
            return TipoUnidadeEdital.TOPICO;
        }

        if (CONHECIMENTOS.matcher(texto).matches()) {
            return texto.contains("ESPEC")
                    ? TipoUnidadeEdital.CONHECIMENTOS_ESPECIFICOS
                    : TipoUnidadeEdital.CONHECIMENTOS_GERAIS;
        }

        if (NIVEL_ESCOLARIDADE.matcher(texto).matches()) {
            return TipoUnidadeEdital.EIXO;
        }

        if (ROTULO_UNIDADE.matcher(texto).matches()) {
            return papelPorRotulo(texto);
        }

        if (SECAO.matcher(texto).matches() || SECAO_NUMERADA.matcher(texto).matches()) {
            return TipoUnidadeEdital.SECAO;
        }

        if (ehCabecalhoInstitucional(texto, frequencia)) {
            return TipoUnidadeEdital.CABECALHO_DOCUMENTO;
        }

        if (ehMobiliarioDePagina(linha, frequencia)) {
            return TipoUnidadeEdital.CABECALHO_DOCUMENTO;
        }

        return ehTituloDeDisciplina(linha, texto, linhaAnterior)
                ? TipoUnidadeEdital.MATERIA
                : TipoUnidadeEdital.TOPICO;
    }

    public TipoUnidadeEdital classificar(String linha, String linhaAnterior) {
        return classificar(linha, linhaAnterior, Map.of());
    }

    public TipoUnidadeEdital classificar(String linha, DocumentModel documento) {
        return classificar(linha, null,
                documento == null ? Map.of() : documento.frequenciaLinhasMaiusculas());
    }

    /**
     * Um título de disciplina é uma linha curta, sem pontuação final, que não
     * dá continuidade à frase anterior.
     *
     * O teste da linha anterior é o que separa título de texto quebrado: o
     * PDFBox corta parágrafos longos em várias linhas, e uma linha cortada
     * sempre continua uma frase inacabada — ou seja, a anterior não termina
     * com pontuação. Um título, ao contrário, abre assunto novo depois de uma
     * frase já encerrada (ou logo após outro título).
     */
    private boolean ehTituloDeDisciplina(String linhaOriginal,
                                         String texto,
                                         String linhaAnterior) {
        if (texto.length() < MINIMO_TITULO || texto.length() > MAXIMO_TITULO) {
            return false;
        }

        if (texto.split("\\s+").length > MAXIMO_PALAVRAS_TITULO) {
            return false;
        }

        // "Leitura e interpretação de textos." é conteúdo, não disciplina: a
        // pontuação final denuncia a frase.
        if (FIM_DE_FRASE.matcher(texto).matches()) {
            return false;
        }

        // "VALOR DE" é sobra do corte de um parágrafo, não nome de disciplina.
        if (CONECTIVO_FINAL.matcher(texto).matches()) {
            return false;
        }

        // Não começa minúsculo: continuação de frase, não título.
        if (!texto.matches("^[A-ZÁÉÍÓÚÂÊÔÃÕÇ0-9].*")) {
            return false;
        }

        if (linhaAnterior == null || linhaAnterior.isBlank()) {
            return true;
        }

        String anterior = linhaAnterior.strip();

        // Frase encerrada antes: o título abre assunto novo. Caso contrário,
        // só cabe título se a linha anterior também for curta — se ela for
        // longa e sem pontuação final, é parágrafo quebrado e esta linha é
        // a continuação dele.
        boolean anteriorEncerrouFrase = FIM_DE_FRASE.matcher(anterior).matches();
        boolean anteriorCurta = anterior.length() <= MAXIMO_TITULO
                && anterior.split("\\s+").length <= MAXIMO_PALAVRAS_TITULO;

        return anteriorEncerrouFrase || anteriorCurta;
    }

    /**
     * Remove a marcação visual para que a classificação enxergue o texto, não
     * o formato: o mesmo edital pode marcar a disciplina com "##", com negrito
     * ou com barra de tabela, e as três formas devem cair na mesma regra.
     *
     * Também remove acentos, porque o PDFBox não os codifica nas fontes
     * padrão — "CONTEUDO" e "CONTEÚDO" precisam casar com o mesmo padrão.
     */
    private String normalizar(String linha) {
        if (linha == null) {
            return "";
        }

        String texto = linha.strip()
                .replaceAll("^\\s*[|#*>]+\\s*", "")
                .replaceAll("\\s*[|#*]+\\s*$", "")
                .replaceAll("\\s+", " ")
                .strip();

        String decomposto = Normalizer.normalize(texto, Normalizer.Form.NFD);
        return decomposto.replaceAll("\\p{M}", "").toUpperCase(Locale.ROOT);
    }

    /**
     * Cabeçalho e rodapé de página repetem em quase todas as páginas, e por
     * isso aparecem dentro do bloco de conteúdo mesmo sem pertencer a ele. A
     * repetição é o que os denuncia, sem precisar de posição no PDF — que o
     * extrator de texto não preserva.
     */
    private boolean ehMobiliarioDePagina(String linha, Map<String, Integer> frequencia) {
        if (frequencia == null || frequencia.isEmpty()) {
            return false;
        }

        Integer repeticoes = frequencia.get(linha.strip());
        if (repeticoes == null || repeticoes < REPETICOES_MOBILIARIO) {
            return false;
        }

        String texto = normalizar(linha);

        // Disciplina que se repete muito é rara, mas "CONHECIMENTOS
        // ESPECÍFICOS" se repete uma vez por cargo e continua sendo conteúdo.
        return !CONHECIMENTOS.matcher(texto).matches()
                && !NIVEL_ESCOLARIDADE.matcher(texto).matches()
                && !ROTULO_UNIDADE.matcher(texto).matches();
    }

    private boolean ehCabecalhoInstitucional(String texto, Map<String, Integer> frequencia) {
        if (!INSTITUICAO.matcher(texto).matches()) {
            return false;
        }

        // Instituição repetida no topo de cada página: o sinal é a repetição.
        // Sem repetição, a linha pode ser uma disciplina de nome institucional.
        if (frequencia == null || frequencia.isEmpty()) {
            return false;
        }

        for (Map.Entry<String, Integer> entrada : frequencia.entrySet()) {
            if (entrada.getValue() >= REPETICOES_CABECALHO
                    && normalizar(entrada.getKey()).equals(texto)) {
                return true;
            }
        }

        return false;
    }

    private TipoUnidadeEdital papelPorRotulo(String texto) {
        if (texto.startsWith("CARGO") || texto.startsWith("EMPREGO")
                || texto.startsWith("FUNCAO")) {
            return TipoUnidadeEdital.CARGO;
        }
        if (texto.startsWith("CURSO") || texto.startsWith("HABILITA")) {
            return TipoUnidadeEdital.CURSO;
        }
        if (texto.startsWith("ESPECIALIDADE")) {
            return TipoUnidadeEdital.ESPECIALIDADE;
        }
        if (texto.startsWith("PROVA")) {
            return TipoUnidadeEdital.PROVA;
        }
        if (texto.startsWith("MODULO") || texto.startsWith("EIXO")) {
            return TipoUnidadeEdital.EIXO;
        }
        return TipoUnidadeEdital.AREA;
    }
}