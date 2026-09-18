package br.com.marcosbassetto.concursos.domain.edital.interpretacao.disciplina;

import br.com.marcosbassetto.concursos.domain.edital.interpretacao.classificacao.TipoUnidadeEdital;
import br.com.marcosbassetto.concursos.domain.edital.interpretacao.rascunho.MateriaRascunho;
import br.com.marcosbassetto.concursos.domain.edital.interpretacao.rascunho.TopicoRascunho;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lê as disciplinas quando elas não estão em linha própria.
 *
 * <b>Quando esta leitura é usada.</b> A leitura por linha própria
 * ({@code MontadorHierarquia}) já reconhece a disciplina que ocupa a linha e
 * deixa o conteúdo nas linhas seguintes. Alguns editais não fazem isso: a
 * disciplina é um prefixo na mesma linha do conteúdo
 * ({@code ADMINISTRAÇÃO GERAL: 1 Evolução da administração.}) ou a linha traz
 * só o nome seguido de dois-pontos ({@code LÍNGUA PORTUGUESA:}).
 *
 * Esta classe é acionada apenas quando a leitura por linha própria não
 * encontrou nada — ou seja, quando o edital não foi lido. Isso é deliberado:
 * nenhum documento que hoje produz matérias passa por aqui, então o resultado
 * já obtido não pode regredir.
 *
 * <b>O que torna a leitura conservadora.</b> Uma linha só é disciplina se o
 * texto antes dos dois-pontos tiver forma de nome:
 *
 * <ul>
 *   <li>curto (até {@value #MAXIMO_TITULO} caracteres);</li>
 *   <li>sem pontuação de nível de frase — é o que separa
 *       {@code DIREITO ADMINISTRATIVO} de uma linha de conteúdo como
 *       {@code Seguridade social: origem e evolução; conceito};</li>
 *   <li>com cada palavra significativa em maiúscula, sendo maiúscula
 *       obrigatória no início;</li>
 *   <li>sem ser rótulo de unidade ({@code CARGO:}), agrupamento
 *       ({@code CONHECIMENTOS GERAIS:}) ou título estrutural.</li>
 * </ul>
 *
 * Nenhuma regra olha banca, órgão ou nome de arquivo: o que decide é a forma
 * como o documento escreve as disciplinas.
 */
@Slf4j
@Component
public class ExtratorDisciplinasInline {

    /** Um título de disciplina é curto; acima disso é linha de conteúdo. */
    private static final int MAXIMO_TITULO = 60;

    /** Abaixo disso não nomeia disciplina ("Ética:", "Saúde:"). */
    private static final int MINIMO_TITULO = 4;

    /**
     * Conteúdo que é apenas uma medida, não conteúdo programático.
     *
     * "TOTAL: 45 pontos" e "Prova: 50 questões" têm a forma de disciplina
     * inline (nome, dois-pontos, número), mas o número é uma quantidade — não
     * um item de conteúdo. Tratá-lo como disciplina criaria uma matéria a
     * partir de um rodapé de tabela de pontuação.
     */
    private static final Pattern MEDIDA = Pattern.compile(
            "^\\d{1,4}([.,]\\d+)?\\s*"
                    + "(PONTOS?|ITENS?|QUESTOES|QUESTOES|HORAS?|MINUTOS?|DIAS?|MESES?|"
                    + "ANOS?|VAGAS?|ACERTOS?|MINUTOS)\\b.*");

    /**
     * Item de conteúdo numerado dentro do parágrafo.
     *
     * O número precisa ser de nível de topo: não pode ser precedido por dígito
     * nem por ponto. Sem isso, "1.1" e "2.5.3" entrariam como novos itens a
     * cada sub-nível e a disciplina teria muitas vezes mais tópicos do que
     * realmente tem.
     */
    private static final Pattern ITEM_NUMERADO = Pattern.compile(
            "(?<![\\d.])(\\d{1,3})\\s+(?=[A-ZÁÉÍÓÚÂÊÔÃÕÇ])");

    /** Pontuação que denuncia frase, não nome. */
    private static final Pattern PONTUACAO_DE_FRASE = Pattern.compile("[;!?]\\s|\\.\\s");

    /** Rótulos que organizam o documento e não são disciplina. */
    private static final Pattern ROTULO_UNIDADE = Pattern.compile(
            "^(CARGO|EMPREGO|FUNCAO|CURSO|HABILITACAO|AREA|ESPECIALIDADE|ENFASE|PROVA|MODULO|EIXO)\\b");

    private static final Pattern AGRUPAMENTO = Pattern.compile(
            "^CONHECIMENTOS\\s+(GERAIS|ESPECIFICOS|BASICOS|COMPLEMENTARES)\\s*$");

    private static final Pattern SECAO = Pattern.compile(
            "^(ANEXO|APENDICE|CAPITULO|SECAO|TITULO|CLAUSULA|DISPOSICOES|CRONOGRAMA|"
                    + "GABARITO|RESULTADO|RETIFICACAO|ERRATA|AVISO|COMUNICADO|EDITAL|"
                    + "INSTRUCOES|OBSERVACOES|REFERENCIAS|BIBLIOGRAFIA|SUMARIO|INDICE|"
                    + "FORMULARIO|DECLARACAO|ATIVIDADE|ETAPAS?|QUESITOS?|PERIODO|CALENDARIO|"
                    + "REQUISITOS|CANDIDATOS?|CRITERIOS|CONHECIMENTO\\(S\\)|NOTAS?|"
                    + "PUBLICACAO|REALIZACAO|INSCRICAO|INSCRICOES)\\b");

    /**
     * Fecha a parte de conteúdo do bloco.
     *
     * É onde a leitura inline para de acumular. Sem isso, a bibliografia e o
     * rodapé que seguem o último cargo entrariam como conteúdo do último
     * cargo — foi assim que "Referências Bibliográficas" virou matéria em um
     * processo seletivo que não tem seção de conteúdo programático.
     */
    private static final Pattern FIM_DO_CONTEUDO = Pattern.compile(
            "^(REFERENCIAS|BIBLIOGRAFIA|ANEXOS?|CRONOGRAMA|APENDICE|GLOSSARIO)\\b.*");

    /**
     * Conectivos que podem aparecer em minúscula dentro de um nome próprio
     * ("Direito do Trabalho", "Controle de Constitucionalidade").
     */
    private static final Set<String> CONECTIVOS = Set.of(
            "DE", "DA", "DO", "DAS", "DOS", "E", "EM", "NO", "NA", "NOS", "NAS",
            "PARA", "POR", "COM", "SEM", "SOBRE", "OU", "A", "O", "AS", "OS", "AO", "AOS");

    /**
     * Lê as disciplinas do bloco.
     *
     * @param bloco conteúdo programático já recortado pelo segmentador
     * @return matérias na forma de rascunho, ou lista vazia se o bloco não usa
     *         nenhuma das formas reconhecidas
     */
    public List<MateriaRascunho> extrair(String bloco) {
        if (bloco == null || bloco.isBlank()) {
            return List.of();
        }

        List<MateriaRascunho> materias = new ArrayList<>();
        List<CaminhoNo> caminho = new ArrayList<>();

        String nome = null;
        StringBuilder conteudo = new StringBuilder();
        String linhaAnterior = null;
        boolean encerrado = false;

        for (String linha : bloco.split("\\R")) {
            String texto = linha.strip();

            if (texto.isEmpty()) {
                linhaAnterior = null;
                continue;
            }

            if (FIM_DO_CONTEUDO.matcher(normalizar(texto)).find()) {
                encerrado = true;
                fechar(materias, nome, caminho, conteudo);
                nome = null;
                conteudo = new StringBuilder();
                continue;
            }

            if (encerrado) {
                continue;
            }

            String nomeInline = nomeDeDisciplina(texto);
            Rotulo rotulo = nomeDeUnidade(texto);

            if (rotulo != null) {
                fechar(materias, nome, caminho, conteudo);
                nome = null;
                conteudo = new StringBuilder();
                atualizarCaminho(caminho, rotulo.texto(), rotulo.papel());
            } else if (nomeInline != null) {
                fechar(materias, nome, caminho, conteudo);
                nome = nomeInline;
                conteudo = new StringBuilder(aposDoisPontos(texto));
            } else if (nome != null) {
                if (!conteudo.isEmpty()) {
                    conteudo.append(' ');
                }
                conteudo.append(texto);
            }

            linhaAnterior = texto;
        }

        fechar(materias, nome, caminho, conteudo);

        log.debug("Leitura de disciplina inline | materias={}", materias.size());

        return materias;
    }

    /**
     * O caminho é hierárquico, não uma lista de rótulos alternativos.
     *
     * Uma unidade de nível 1 nova substitui a anterior do <i>mesmo tipo</i>:
     * o conteúdo pertence ao cargo corrente, não a todos os cargos vistos.
     * Já o agrupamento de conhecimentos é um nível a mais dentro da unidade, e
     * por isso acumula — {@code CARGO → CONHECIMENTOS ESPECÍFICOS → MATÉRIA} é
     * a hierarquia real de um edital que separa gerais de específicos.
     *
     * Substituir tudo a cada rótulo perdia a unidade: a matéria ficava
     * atribuída só a "CONHECIMENTOS ESPECÍFICOS", sem o cargo de origem.
     */
    private void atualizarCaminho(List<CaminhoNo> caminho, String texto, TipoUnidadeEdital papel) {
        if (!papel.ehAgrupamento()) {
            caminho.removeIf(no -> no.tipo() == papel);
        }
        caminho.add(new CaminhoNo(texto, papel));
    }

    private List<String> nomesDoCaminho(List<CaminhoNo> caminho) {
        return caminho.stream().map(CaminhoNo::texto).toList();
    }

    private void fechar(List<MateriaRascunho> materias,
                        String nome,
                        List<CaminhoNo> caminho,
                        StringBuilder conteudo) {
        if (nome == null || nome.isBlank()) {
            return;
        }

        materias.add(new MateriaRascunho(
                nome, nomesDoCaminho(caminho), dividirItens(conteudo.toString())));
    }

    /**
     * Divide o parágrafo da disciplina nos itens numerados de nível de topo.
     *
     * O número vira código do tópico, mas não faz parte do nome: o texto do
     * item é o conteúdo. Um parágrafo sem numeração vira um único tópico, que é
     * a leitura honesta — há conteúdo ali e ele não está subdividido.
     */
    private List<TopicoRascunho> dividirItens(String texto) {
        List<int[]> marcos = new ArrayList<>();
        List<String> codigos = new ArrayList<>();

        Matcher itens = ITEM_NUMERADO.matcher(texto);
        while (itens.find()) {
            marcos.add(new int[]{itens.start(), itens.end()});
            codigos.add(itens.group(1));
        }

        if (marcos.isEmpty()) {
            String unico = texto.strip();
            return unico.isEmpty() ? List.of() : List.of(TopicoRascunho.de(unico));
        }

        List<TopicoRascunho> topicos = new ArrayList<>();

        for (int i = 0; i < marcos.size(); i++) {
            int inicio = marcos.get(i)[1];
            int fim = (i + 1 < marcos.size()) ? marcos.get(i + 1)[0] : texto.length();
            String item = texto.substring(inicio, fim).strip();

            if (!item.isEmpty()) {
                topicos.add(new TopicoRascunho(item, codigos.get(i), TipoUnidadeEdital.TOPICO));
            }
        }

        return topicos;
    }

    /**
     * Devolve o nome da disciplina se a linha for uma disciplina, ou
     * {@code null}.
     *
     * A distinção entre "linha de disciplina" e "linha de conteúdo que contém
     * dois-pontos" é o que impede o extrator de inventar matérias: o nome
     * precisa ter forma de nome, não de frase.
     */
    private String nomeDeDisciplina(String linha) {
        if (!linha.contains(":")) {
            return null;
        }

        String nome = linha.substring(0, linha.indexOf(':')).strip();

        if (!ehNomeValido(nome)) {
            return null;
        }

        String resto = linha.substring(linha.indexOf(':') + 1).strip();

        // "TOTAL: 45 pontos" tem a forma de disciplina, mas o conteúdo é uma
        // quantidade. Sem esta guarda, o rodapé de uma tabela de pontuação
        // viraria matéria.
        if (MEDIDA.matcher(normalizar(resto)).matches()) {
            return null;
        }

        // Nome em caixa alta é o critério mais forte: é assim que os editais
        // destacam a disciplina. Vale mesmo com conteúdo solto depois dos
        // dois-pontos, porque o próprio edital o destaca dessa forma.
        if (ehCaixaAlta(nome)) {
            return nome;
        }

        // Nome em caixa mista: só vale como disciplina quando a linha é o nome
        // sozinho ("Direito Administrativo:"), forma em que o conteúdo vem
        // abaixo. Com conteúdo na mesma linha, a caixa mista indica frase.
        return resto.isEmpty() ? nome : null;
    }

    private boolean ehNomeValido(String nome) {
        if (nome.length() <= MINIMO_TITULO || nome.length() > MAXIMO_TITULO) {
            return false;
        }

        String normalizado = normalizar(nome);

        if (PONTUACAO_DE_FRASE.matcher(normalizado).find()) {
            return false;
        }

        if (ROTULO_UNIDADE.matcher(normalizado).find()
                || AGRUPAMENTO.matcher(normalizado).matches()
                || SECAO.matcher(normalizado).find()
                || FIM_DO_CONTEUDO.matcher(normalizado).find()) {
            return false;
        }

        return palavrasCapitalizadas(nome);
    }

    /**
     * Cada palavra significativa começa em maiúscula; conectivos podem vir em
     * minúscula. É o que distingue "Direito do Trabalho" de "de alteração,
     * extinção contratual".
     */
    private boolean palavrasCapitalizadas(String nome) {
        String[] palavras = nome.split("\\s+");

        if (palavras.length == 0) {
            return false;
        }

        for (String palavra : palavras) {
            String limpa = palavra.replaceAll("[^\\p{L}\\p{N}]", "");

            if (limpa.isEmpty() || CONECTIVOS.contains(normalizar(limpa))) {
                continue;
            }

            if (!Character.isUpperCase(limpa.codePointAt(0))) {
                return false;
            }
        }

        // A primeira palavra não pode ser conectivo: "de alteração" não nomeia.
        String primeira = palavras[0].replaceAll("[^\\p{L}\\p{N}]", "");
        return !primeira.isEmpty() && !CONECTIVOS.contains(normalizar(primeira));
    }

    private boolean ehCaixaAlta(String nome) {
        boolean temLetra = false;

        for (int i = 0; i < nome.length(); i++) {
            char c = nome.charAt(i);
            if (!Character.isLetter(c)) {
                continue;
            }
            temLetra = true;
            if (Character.isLowerCase(c)) {
                return false;
            }
        }

        return temLetra;
    }

    /**
     * Rótulo de unidade de nível 1 ou agrupamento de conhecimentos.
     *
     * Devolve a linha inteira como texto do caminho, e não só o nome antes dos
     * dois-pontos: "CARGO 1: ANALISTA DE SISTEMAS" é mais útil na auditoria do
     * que "CARGO 1". É também o que a leitura por linha própria registra.
     *
     * A verificação não passa por {@link #ehNomeValido} porque é justamente o
     * oposto: aqui o que se procura é um rótulo, que {@code ehNomeValido}
     * recusa para não virar disciplina.
     */
    private Rotulo nomeDeUnidade(String linha) {
        int corte = linha.indexOf(':');

        if (corte < 0) {
            return null;
        }

        String nome = linha.substring(0, corte).strip();

        if (nome.length() <= MINIMO_TITULO || nome.length() > MAXIMO_TITULO) {
            return null;
        }

        String normalizado = normalizar(nome);

        if (AGRUPAMENTO.matcher(normalizado).matches()) {
            return new Rotulo(linha.strip(), TipoUnidadeEdital.CONHECIMENTOS_GERAIS);
        }

        if (!ROTULO_UNIDADE.matcher(normalizado).find()) {
            return null;
        }

        return new Rotulo(linha.strip(), papelDaUnidade(normalizado));
    }

    /** Traduz o rótulo textual para o papel que ele representa na hierarquia. */
    private TipoUnidadeEdital papelDaUnidade(String normalizado) {
        if (normalizado.startsWith("CARGO") || normalizado.startsWith("EMPREGO")
                || normalizado.startsWith("FUNCAO")) {
            return TipoUnidadeEdital.CARGO;
        }
        if (normalizado.startsWith("CURSO") || normalizado.startsWith("HABILITA")) {
            return TipoUnidadeEdital.CURSO;
        }
        if (normalizado.startsWith("ESPECIALIDADE")) {
            return TipoUnidadeEdital.ESPECIALIDADE;
        }
        if (normalizado.startsWith("PROVA")) {
            return TipoUnidadeEdital.PROVA;
        }
        if (normalizado.startsWith("MODULO") || normalizado.startsWith("EIXO")) {
            return TipoUnidadeEdital.EIXO;
        }
        return TipoUnidadeEdital.AREA;
    }

    private String aposDoisPontos(String linha) {
        int corte = linha.indexOf(':');
        return corte >= 0 ? linha.substring(corte + 1).strip() : "";
    }

    private String normalizar(String texto) {
        String decomposto = Normalizer.normalize(texto.strip(), Normalizer.Form.NFD);
        return decomposto.replaceAll("\\p{M}", "")
                .replaceAll("\\s+", " ")
                .toUpperCase(Locale.ROOT);
    }

    /** Disciplina reconhecida na linha: o nome e o conteúdo que veio depois. */
    private record NomeInline(String nome, String conteudoOriginal) {
    }

    /** Rótulo de unidade reconhecido: o texto para o caminho e o papel na hierarquia. */
    private record Rotulo(String texto, TipoUnidadeEdital papel) {
    }

    /** Nó do caminho corrente, com o tipo para permitir a substituição por tipo. */
    private record CaminhoNo(String texto, TipoUnidadeEdital tipo) {
    }
}
