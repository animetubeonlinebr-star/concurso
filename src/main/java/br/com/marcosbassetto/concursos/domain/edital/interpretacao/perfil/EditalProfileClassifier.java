package br.com.marcosbassetto.concursos.domain.edital.interpretacao.perfil;

import br.com.marcosbassetto.concursos.domain.edital.interpretacao.classificacao.TipoUnidadeEdital;
import br.com.marcosbassetto.concursos.domain.edital.interpretacao.documento.DocumentModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.regex.Pattern;

/**
 * Decide que tipo de edital é este documento.
 *
 * Existe para tornar explícita uma pergunta que antes ficava implícita na
 * escolha do extrator: se o documento não tem seção de conteúdo programático,
 * não adianta procurar disciplinas nele. É melhor dizer que não há conteúdo e
 * parar, do que devolver linhas soltas como matérias.
 */
@Slf4j
@Component
public class EditalProfileClassifier {

    /**
     * Abertura da seção que declara o conteúdo programático. Aceita as
     * variações reais dos editais: com ou sem "ANEXO" antes, com "DO" no meio
     * ("ANEXO II – DO CONTEÚDO PROGRAMÁTICO") e com numeração de capítulo.
     */
    private static final Pattern BLOCO_CONTEUDO = Pattern.compile(
            "^\\s*(?:ANEXO\\s+[IVXL]+\\s*[-–—:.]?\\s*)?"
                    + "(?:\\d{1,2}(?:\\.\\d{1,2})*\\s*)?"
                    + "(?:D[AEOS]\\s+)?"
                    + "(?:CONTEUDOS?\\s+PROGRAMATICOS?"
                    + "|PROGRAMA\\s+DE\\s+PROVA"
                    + "|CONTEUDO\\s+PROGRAMATICO"
                    + "|PROGRAMAS?\\s+DE\\s+PROVAS?)"
                    + "[^\\n]*$");

    private static final Pattern CONHECIMENTOS = Pattern.compile(
            "^\\s*CONHECIMENTOS\\s+(GERAIS|ESPECIFICOS|BASICOS|COMPLEMENTARES)\\s*$");

    /**
     * Agrupamento de conhecimentos numerado dentro de um capítulo, como
     * "15.2.4 CONHECIMENTOS GERAIS".
     *
     * Editais que não dedicam um anexo ao conteúdo programático declaram o
     * agrupamento dentro do capítulo das provas. Sem reconhecer esta forma, o
     * documento era classificado como sem conteúdo e as matérias ficavam
     * invisíveis — mesmo estando lá.
     */
    private static final Pattern CONHECIMENTOS_NUMERADO = Pattern.compile(
            "^\\s*\\d{1,2}(?:\\.\\d{1,2}){1,3}\\s+(?:D[AEOS]\\s+)?"
                    + "CONHECIMENTOS\\s+(GERAIS|ESPECIFICOS|BASICOS|COMPLEMENTARES)[^\\n]*$");

    private static final Pattern CARGO = Pattern.compile(
            "^\\s*(?:CARGO|EMPREGO|FUNCAO)\\s*\\d*\\s*[:\\-]\\s*\\S.*");

    private static final Pattern AREA = Pattern.compile(
            "^\\s*(?:AREA|ESPECIALIDADE|ENFASE)\\s*[:\\-]\\s*\\S.*");

    private static final Pattern CURSO = Pattern.compile(
            "^\\s*(?:CURSO|HABILITACAO)\\s*[:\\-]\\s*\\S.*");

    private static final Pattern PROVA = Pattern.compile(
            "^\\s*PROVA\\s+(OBJETIVA|DISCURSIVA|PRATICA)\\s*$");

    /**
     * Documentos que existem no lugar de um edital de conteúdo: avisos,
     * comunicados, errata e resultado. Nada a extrair deles.
     */
    private static final Pattern DOCUMENTO_SEM_CONTEUDO = Pattern.compile(
            "^\\s*(COMUNICADO|AVISO|ERRATA|RETIFICACAO|"
                    + "RESULTADO(\\s+FINAL)?|GABARITO(\\s+OFICIAL)?|"
                    + "HOMOLOGACAO|EXTRATO)\\s*$");

    public EditalProfile classificar(DocumentModel documento) {
        if (documento == null || documento.vazio()) {
            return EditalProfile.generico();
        }

        EditalProfile perfil = decidir(documento);

        log.info("Perfil do edital | codigo={} | hierarquia={}",
                perfil.codigo(), perfil.hierarquia().niveis());

        return perfil;
    }

    /**
     * Um documento sem seção de conteúdo programático é classificado como
     * SEM_CONTEUDO mesmo que tenha linhas em destaque: é o caso do comunicado
     * que lista cursos em tabela e que antes virava cinco matérias falsas.
     */
    public boolean ehDocumentoDeConteudo(DocumentModel documento) {
        if (documento == null || documento.vazio()) {
            return false;
        }

        String texto = documento.textoLimpo();
        boolean temBloco = possuiBlocoConteudo(texto);

        if (temBloco) {
            return true;
        }

        // Sem bloco declarado, ainda pode ser conteúdo se o documento declarar
        // conhecimentos gerais/específicos — formato que dispensa o título.
        return possuiLinha(CONHECIMENTOS, texto);
    }

    private EditalProfile decidir(DocumentModel documento) {
        String texto = documento.textoLimpo();

        if (documentoSemConteudo(texto)) {
            return EditalProfile.semConteudo();
        }

        if (!ehDocumentoDeConteudo(documento)) {
            return EditalProfile.semConteudo();
        }

        if (possuiLinha(CONHECIMENTOS, texto)) {
            return EditalProfile.porConhecimentos();
        }

        return comUnidadeSeHouver(texto);
    }

    private boolean documentoSemConteudo(String texto) {
        for (String linha : texto.split("\\R")) {
            String normalizada = normalizar(linha);
            if (DOCUMENTO_SEM_CONTEUDO.matcher(normalizada).matches()) {
                return true;
            }
        }
        return false;
    }

    private boolean possuiBlocoConteudo(String texto) {
        for (String linha : texto.split("\\R")) {
            String normalizada = normalizar(linha);
            if (BLOCO_CONTEUDO.matcher(normalizada).matches()
                    || CONHECIMENTOS.matcher(normalizada).matches()
                    || CONHECIMENTOS_NUMERADO.matcher(normalizada).matches()) {
                return true;
            }
        }
        return false;
    }

    private boolean possuiLinha(Pattern padrao, String texto) {
        for (String linha : texto.split("\\R")) {
            if (padrao.matcher(normalizar(linha)).matches()) {
                return true;
            }
        }
        return false;
    }

    private EditalProfile comUnidadeSeHouver(String texto) {
        if (possuiLinha(CARGO, texto)) {
            return EditalProfile.porUnidade(TipoUnidadeEdital.CARGO, "CARGO");
        }
        if (possuiLinha(AREA, texto)) {
            return EditalProfile.porUnidade(TipoUnidadeEdital.AREA, "AREA");
        }
        if (possuiLinha(CURSO, texto)) {
            return EditalProfile.porUnidade(TipoUnidadeEdital.CURSO, "CURSO");
        }
        if (possuiLinha(PROVA, texto)) {
            return EditalProfile.porUnidade(TipoUnidadeEdital.PROVA, "PROVA");
        }
        return EditalProfile.generico();
    }

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
        return decomposto.replaceAll("\\p{M}", "").toUpperCase(java.util.Locale.ROOT);
    }
}