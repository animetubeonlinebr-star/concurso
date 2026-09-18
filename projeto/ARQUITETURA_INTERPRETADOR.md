# Demanda 02 — Arquitetura do Interpretador de Edital

Registro das decisões de desenho da interpretação de edital: como escolher o
segundo perfil com evidência, o que a comparação A × B mostrou e a estrutura
universal que o domínio passou a ter.

Complementa `projeto.md` (convenções gerais) e `PLAN_DEMANDA_02.md` (fluxo de
upload, staging, revisão e confirmação).

## Sequência da Demanda 02

| Etapa | Item                                   | Status        |
| ----- | -------------------------------------- | ------------- |
| 02.0  | Arquitetura do interpretador           | ✅ concluído  |
| 02.0.1 | DocumentModel                         | ✅ concluído  |
| 02.0.2 | EditalProfile                         | ✅ concluído  |
| 02.0.3 | Classifier                            | ✅ concluído  |
| 02.0.4 | Strategy                              | ✅ concluído  |
| 02.0.5 | Factory Method                        | ✅ concluído  |
| 02.0.6 | Facade + Quality Validator            | ✅ concluído  |
| 02.0.7 | Status da extração                    | ✅ concluído  |
| 02.0.8 | Template Method                       | ✅ concluído  |
| 02.0.9 | Escolha do segundo edital real         | ✅ concluído  |
| 02.0.10 | Implementação do segundo perfil       | ✅ concluído  |
| 02.0.11 | Matriz arquitetural dos formatos     | ✅ concluído  |
| 02.0.12 | Consolidar características           | ← próxima     |
| 02.0.13 | Confirmar/rejeitar Abstract Factory  | depois        |
| 02.0.14 | Árvore tipada de UnidadeEdital       | depois        |
| 02.0.15 | Revisar Concurso/Curso/Matéria/Tópico | depois       |
| 02.0.16 | Persistência final                   | depois        |
| 02.0.17 | E2E completo dos 23 PDFs             | depois        |

## Perfil A (atual)

O perfil A é o formato **mais comum** entre os editais reais do repositório:
uma seção declarada de conteúdo programático, com disciplinas em destaque e os
itens de conteúdo logo abaixo, no mesmo parágrafo ou em linhas de lista.

```text
ANEXO II – CONTEÚDO PROGRAMÁTICO

LÍNGUA PORTUGUESA
Interpretação de texto. Significação das palavras. Ortografia Oficial.

MATEMÁTICA
Operações com números naturais. Porcentagem. Raciocínio lógico.
```

A disciplina é uma linha curta, isolada, sem pontuação final, seguida do
parágrafo de conteúdo. Um tópico é um item de lista (`▪`, `-`, `I.`, `1.`) ou
uma linha de texto do parágrafo de conteúdo.

É o perfil registrado como `GENERICO`/`CONHECIMENTOS`/`CARGO`/`AREA`/`CURSO`/
`PROVA`, todos lidos por `EstrategiaBlocoConteudo`.

## Como o segundo perfil foi escolhido

A escolha não foi por impressão: `DiagnosticoPerfisReaisTest` roda o
interpretador sobre os 23 PDFs de `PDF_edital_teste` e grava
`build/diagnostico-perfis.tsv` com perfil detectado, hierarquia, status,
contagem de matérias e tópicos, e ocorrências de cada rótulo de unidade.

### Matriz dos editais reais (perfil A)

| PDF (abreviado)            | Perfil A detectado | Hierarquia     | Status         | Matérias | Tópicos |
| -------------------------- | ------------------ | -------------- | -------------- | -------- | ------- |
| `05d3a1ad…` (Itapecerica)  | CONHECIMENTOS      | conhecimentos  | PARCIAL        | 137      | 827     |
| `4D1A8250…` (Ponta Porã)   | SEM_CONTEUDO       | —              | NAO_IDENTIFICADO | **0**  | **0**   |
| `9454893c…` (SEMA/MT)      | CONHECIMENTOS      | conhecimentos  | PARCIAL        | 37       | 2320    |
| `9B3058B2…` (Ponta Porã 2) | SEM_CONTEUDO       | —              | NAO_IDENTIFICADO | 0      | 0       |
| `CMIH2601…` (Itanhaém)     | CONHECIMENTOS      | conhecimentos  | PARCIAL        | 43       | 68      |
| `VNSP2603…` (Unesp)        | SEM_CONTEUDO       | —              | NAO_IDENTIFICADO | 0      | 0       |
| `…n_001_2026_1704026`      | CARGO              | cargo          | PROCESSADO     | 18       | 493     |
| `…n_1_2026_1704238`        | CONHECIMENTOS      | conhecimentos  | PARCIAL        | 28       | 107     |
| `…funcamp_n_39`            | CARGO              | cargo          | NAO_IDENTIFICADO | 0      | 0       |
| `…funcamp_n_83`            | CARGO              | cargo          | NAO_IDENTIFICADO | 0      | 0       |
| `edital_n_003_2026…`       | PROVA              | prova          | PARCIAL        | 21       | 39      |
| `edital_n_01_2026_1703858` | CONHECIMENTOS      | conhecimentos  | PARCIAL        | 10       | 55      |
| `edital_n_01_2026_1704225` | CONHECIMENTOS      | conhecimentos  | NAO_IDENTIFICADO | 0      | 0       |
| `edital_n_02_2026_1704078` | CONHECIMENTOS      | conhecimentos  | PARCIAL        | 22       | 24      |
| `edital_n_259_2026…`       | SEM_CONTEUDO       | —              | NAO_IDENTIFICADO | 0      | 0       |
| `edital_n_4_completo…`     | CONHECIMENTOS      | conhecimentos  | PARCIAL        | 20       | 152     |
| `sei_9998416…` (TJRS)      | GENERICO           | simples        | PARCIAL        | 35       | 672     |
| `unasp126…` (UNASP/FCC)    | GENERICO           | simples        | PARCIAL        | 12       | 507     |
| `edital_de_concurso_publico_01_2026_1703649` | CONHECIMENTOS | conhecimentos | PARCIAL | 73 | 300 |
| `edital_de_concurso_publico_n_01_2026_1703610` | CONHECIMENTOS | conhecimentos | PARCIAL | 53 | 125 |
| `edital_de_abertura_n_003_2026_1702518` | SEM_CONTEUDO | — | NAO_IDENTIFICADO | 0 | 0 |
| `edital_de_abertura_n_35_2025_1675222` | CARGO | cargo | NAO_IDENTIFICADO | 0 | 0 |
| `edital_de_abertura_processo_seletivo_n_01_2026_1703174` | SEM_CONTEUDO | — | NAO_IDENTIFICADO | 0 | 0 |

Resumo: **13 dos 23 extraem estrutura**; 10 terminam em `NAO_IDENTIFICADO`, e a
divisão desses 10 é o que importa:

| Motivo da falha                                     | Quantos | Exemplos                                  |
| --------------------------------------------------- | ------- | ----------------------------------------- |
| Documento realmente sem conteúdo programático       | 4       | `VNSP2603…` (comunicado), `edital_n_259…`, `…n_003_2026_1702518`, `…processo_seletivo_n_01_2026_1703174` |
| Formato do Perfil B (tem conteúdo, âncora não casa) | 3       | `4D1A8250…`, `9B3058B2…`, `edital_n_01_2026_1704225` |
| Processo seletivo FUNCAMP sem anexo de programa     | 3       | `…funcamp_n_39`, `…funcamp_n_83`, `…n_35_2025_1675222` |

Os 4 primeiros são a resposta correta do sistema: não há o que extrair e o
usuário é avisado. Os 3 do meio são o alvo do Perfil B. Os 3 últimos não têm
anexo de conteúdo — a falha é do documento, não do interpretador.

### Eixos estruturais medidos

Além da contagem, a medição classifica cada PDF por eixo. Os números são
ocorrências de linha por padrão:

| PDF                      | título anexo | disciplina inline (`NOME: 1 item`) | item numerado | unidade nível 1 | `PARA O CARGO` |
| ------------------------ | ------------ | ---------------------------------- | ------------- | --------------- | -------------- |
| `4D1A8250…` (Ponta Porã) | **0**        | **43**                             | **473**       | **28** (cargo)  | 0              |
| `05d3a1ad…`              | 1            | 0                                  | 160           | 23 (área)       | 0              |
| `9454893c…`              | 1            | 3                                  | 1316          | 2 (cargo)       | 0              |
| `edital_n_02_2026…`      | 1            | 0                                  | 63            | 3 (cargo)       | 11             |
| `…funcamp_n_39`          | 0            | 1                                  | 5             | 1 (cargo)       | 0              |
| `edital_de_abertura_n_003_2026_1702518` | 0 | 0 | 0 | 0 | 0 |

### Por que `4D1A8250…` (Câmara de Ponta Porã/MS) é o exemplar do Perfil B

O formato do Perfil B **não é um caso isolado**: a mesma forma aparece em três
editais do conjunto — Ponta Porã/MS (`4D1A8250…`), Ponta Porã nº 2 (`9B3058B2…`)
e Fundação Arte e Cultura de Ilhabela (`edital_n_01_2026_1704225`). Isso muda a
natureza da decisão: não é tratar uma exceção, é ler um formato recorrente.

| PDF                        | `CARGO n:` | âncora `CONHECIMENTOS` | disciplina inline | Perfil A hoje   |
| -------------------------- | ---------- | ---------------------- | ----------------- | --------------- |
| `4D1A8250…` (Ponta Porã)   | **26**     | 2                      | **94**            | SEM_CONTEUDO    |
| `edital_n_01_2026_1704225` | 2          | 4                      | 26                | CONHECIMENTOS   |
| `9B3058B2…` (Ponta Porã 2) | 2          | 1                      | 2                 | SEM_CONTEUDO    |

Ponta Porã nº 1 é o **exemplar** — o mais rico nos três eixos — e é o que serve
de referência para a implementação. Ele falha onde o perfil A acerta, e falha
pelo motivo mais informativo possível: falta a âncora que o perfil A exige.

1. **Não tem título de anexo.** O bloco começa no meio do capítulo 15, em
   `15.2.4 CONHECIMENTOS GERAIS` / `15.2.5 CONHECIMENTOS ESPECÍFICOS`. O
   `EditalProfileClassifier` reconhece `CONHECIMENTOS` isolado, mas o
   `SegmentadorEdital` não o aceita como âncora porque a linha vem numerada
   (`15.2.4`). Resultado: `SEM_CONTEUDO` e **zero matérias** em um edital com
   7 cargos e cerca de 473 itens de conteúdo.

2. **Disciplina é inline, não é linha de título.** A forma é
   `NOME DA DISCIPLINA: 1 item. 2 item. 2.1 subitem.` — 43 ocorrências no bloco
   (94 no documento). Não há linha curta isolada para virar matéria; o nome da
   disciplina é um prefixo antes dos dois-pontos. O classificador do perfil A
   exige título em linha própria, então nenhum desses nomes seria reconhecido.

3. **Tópicos são numerados dentro do parágrafo.** `1 ... 2 ... 2.1 ... 2.2 ...`
   no mesmo bloco de texto (473 ocorrências). O perfil A espera item de lista
   com marcador ou linha separada.

4. **Unidade de nível 1 é explícita e rotulada** — `CARGO 1: ANALISTA DE
   LICITAÇÃO E CONTRATOS`, 26 ocorrências, incluindo as seções de conteúdo
   específico. É a evidência de que o cargo introduz nova ramificação, e é o
   que o `EditalProfile.porUnidade` já declara — mas a estratégia nunca chega a
   usá-lo, porque o perfil resolvido é `SEM_CONTEUDO`.

5. **Hierarquia alvo**: `CARGO → CONHECIMENTOS_GERAIS/CONHECIMENTOS_ESPECÍFICOS
   → MATERIA → TOPICO` — quatro níveis, contra os dois do perfil A. É a maior
   diferença estrutural disponível no conjunto.

Também tem `CONHECIMENTOS GERAIS` **descartáveis**: o `CARGO 2` repete o bloco
geral (`LÍNGUA PORTUGUESA`, `RACIOCÍNIO LÓGICO-MATEMÁTICO`, `REALIDADE ÉTNICA…`)
em outra seção — o que exige decidir se o conteúdo geral é compartilhado ou
replicado por cargo. Nenhum outro edital do conjunto coloca essa pergunta.

### Perfil B — resultado esperado

```text
CARGO 1: ANALISTA DE LICITAÇÃO E CONTRATOS
├── CONHECIMENTOS GERAIS
│   ├── LÍNGUA PORTUGUESA
│   │   ├── 1 Compreensão e interpretação de textos de gêneros variados.
│   │   └── 2 Reconhecimento de tipos e gêneros textuais.
│   └── RACIOCÍNIO LÓGICO-MATEMÁTICO
│       └── 1 Conjuntos numéricos: números inteiros, racionais e reais.
└── CONHECIMENTOS ESPECÍFICOS
    ├── ADMINISTRAÇÃO GERAL
    └── LICITAÇÃO E GESTÃO DE CONTRATOS
```

O que muda em relação ao perfil A:

| Aspecto                  | Perfil A                          | Perfil B                                  |
| ------------------------ | --------------------------------- | ----------------------------------------- |
| Âncora do bloco          | título de anexo                   | título numerado de capítulo               |
| Disciplina               | linha de título isolada           | prefixo antes de `:` no parágrafo         |
| Tópico                   | item de lista/marcador            | item numerado no mesmo parágrafo          |
| Nível 1                  | implícito (curso "Geral")         | `CARGO n: …` explícito                    |
| Agrupamento              | `CONHECIMENTOS GERAIS/ESPECÍFICOS`| idem, mas por cargo                       |
| Profundidade da árvore   | 2                                 | 4                                         |

## Estrutura universal do domínio

Depois de medir, a comparação por componente é esta:

| Componente             | Perfil A | Perfil B | Evidência                                        |
| ---------------------- | -------- | -------- | ------------------------------------------------ |
| Metadata               | =        | =        | `ExtratorMetadadosEdital` lê só o cabeçalho; independe do bloco |
| DocumentParser         | =        | =        | os dois chegam como texto via `PdfTextExtractorService` → `DocumentModel` |
| ProfileClassifier      | ≠        | ≠        | A resolve título de anexo; B precisa aceitar título numerado e `CARGO n:` |
| HierarchyExtractor     | ≠        | ≠        | `HierarchyDefinition.simples()` vs `CARGO → CONHECIMENTOS → MATERIA → TOPICO` |
| SubjectExtractor       | ≠        | ≠        | linha de título vs prefixo `NOME: 1 item…`       |
| TopicExtractor         | ≠        | ≠        | marcador/linha vs numeração inline               |
| Segmenter              | ≠        | ≠        | âncora diferente                                 |
| Normalizer             | =        | =        | `NormalizadorEstrutura` + `NomeNormalizer` não olham o formato |
| QualityValidator       | ≈        | ≈        | mesmas regras de contagem; muda só o limiar esperado de profundidade |
| Status                 | =        | =        | `StatusExtracao` é o mesmo vocabulário            |

**Leitura da tabela:** os componentes que variam são os **do meio do pipeline**
(segmentação, classificação, extração de disciplina e de tópico). Os das pontas
— entrada (`DocumentModel`) e saída (normalização, validação, status) — são
comuns. E a variação é sempre do mesmo tipo: **onde está a fronteira de cada
nível**, não como o nível é representado.

Isso é uma diferença de **parâmetros de leitura**, não de **família de
objetos**. Onde o perfil A procura uma linha de título, o perfil B procura um
prefixo antes dos dois-pontos; nos dois casos o resultado é uma disciplina com
tópicos. O tipo do resultado não muda.

### Template Method (02.0.8) — implementado

O esqueleto invariante da interpretação está em
`InterpretadorEditalTemplate`. A ordem é fixa:

```text
interpretar(texto)                    [final]
  ├── texto vazio → NAO_IDENTIFICADO
  ├── DocumentModel.de(texto)
  ├── classificador.classificar(documento)
  ├── interpretarConteudo(documento, perfil)   [abstrato — único passo variável]
  ├── extratorMetadados.extrair(documento)
  ├── avaliador.avaliar(rascunho, dadosCompletos)
  └── montarResposta(dados, rascunho, status)  [Curso "Geral" implícito]
```

`InterpretadorEditalFacade` é a implementação de produção e sua especialização
tem três linhas: escolher a estratégia pelo perfil e aplicá-la.

O valor de fixar a ordem não é estético. As etapas 5 e 6 eram justamente as que
se perdiam: o status calculado pelo extrator já foi descartado em produção, e um
edital sem conteúdo programático chegou à revisão como sucesso vazio. Com o
esqueleto em `final`, um formato novo não pode esquecer a avaliação de qualidade
nem a extração de metadados — `InterpretadorEditalTemplateTest` fixa isso
inclusive para um perfil que devolve matérias sem tópicos.

### Decisão sobre Abstract Factory (02.0.12)

**Não será criada.** A evidência da matriz não sustenta uma família de objetos:

- Normalizer, Validator, Status e DocumentModel são **idênticos** nos dois
  perfis. Uma Abstract Factory de "família A" e "família B" criaria duas
  implementações de componentes que não variam, e a duplicação seria o único
  resultado.
- O que varia é **configuração de leitura** (âncora, forma de título, forma de
  item) e um **passo de algoritmo** (recortar o bloco, ler disciplina inline).
  Configuração já é o `EditalProfile`; o passo de algoritmo é o ponto de
  extensão que `EstrategiaInterpretacaoEdital` oferece.

A combinação que resolve é **Template Method + Strategy + componentes
especializados** — exatamente o cenário A previsto no roteiro. A Abstract
Factory fica como decisão **rejeitada com motivo registrado**, a ser revisitada
se um terceiro formato exigir troca conjunta de normalização e validação.

### Consequência para o Perfil B (02.0.10)

Como o que varia é a leitura, o segundo perfil entra **sem classe nova de
normalização, validação ou persistência**. A implementação mínima é:

1. `EditalProfileClassifier` — aceitar `CONHECIMENTOS GERAIS/ESPECÍFICOS`
   numerados (`15.2.4`) como âncora e priorizar `CARGO n:` rotulado;
2. `SegmentadorEdital` — aceitar o título numerado como início de bloco;
3. uma disciplina inline — a leitura de `NOME: 1 item. 2 item.` como matéria
   com tópicos numerados.

Os itens 2 e 3 são a leitura especializada; o item 1 é o classificador
reconhecendo uma forma de título que já existe no documento.

### Regra de segurança da extração

Nenhum dos dois perfis tenta acertar 100% sozinho — e isso é desenho, não
limitação. Toda saída carrega `StatusExtracao`, e `PARCIAL`, `BAIXA_CONFIANCA`
e `NAO_IDENTIFICADO` chegam ao usuário como aviso na tela de processamento e
como banner na revisão. Um PDF estranho tem três desfechos honestos: extrair o
que deu, avisar o que não deu, ou declarar que não há conteúdo programático. O
que ele nunca faz é devolver matérias inventadas como se fossem sucesso.

### 18.0.7 Implementação do Perfil B

A leitura do Perfil B entrou por três pontos, sem nenhuma regra por nome de
arquivo, órgão ou banca — o que decide é a forma como o documento escreve.

**1. Âncora do bloco.** O reconhecimento do início deixou de ser um regex único
e virou `AncoraBlocoConteudo`, com três implementações por ordem de
especificidade: `AncoraTituloExplicito` (ANEXO … CONTEÚDO PROGRAMÁTICO),
`AncoraAgrupamentoConhecimentos` (CONHECIMENTOS GERAIS) e `AncoraTituloNumerado`
(`15.2.4 CONHECIMENTOS GERAIS`). O número é contexto; o nome do agrupamento
depois dele é a evidência — é o que impede `1. DISPOSIÇÕES PRELIMINARES` de
virar conteúdo programático.

**2. Fim do bloco.** O bloco ancorado em título numerado não é fechado por um
anexo; ele termina na mudança de capítulo (`FIM_CAPITULO_NUMERADO`, nível raso
como `16 DAS DISPOSIÇÕES GERAIS`). Sem isso o bloco seguiria até o fim do
documento e o capítulo seguinte viraria conteúdo.

**3. Disciplina inline.** `ExtratorDisciplinas` escolhe entre as duas leituras
por qual reconhece mais disciplinas no próprio documento, com empate para a
leitura por linha própria, que é a validada. `ExtratorDisciplinasInline`
reconhece a disciplina como prefixo (`NOME: 1 item`) e como nome sozinho
seguido de dois-pontos, com estas recusas, todas necessárias na prática:

| Recusa | Por quê |
| ------ | ------- |
| nome com pontuação de frase (`; `, `. `) | separa `DIREITO ADMINISTRATIVO` de uma linha de conteúdo que termina em dois-pontos |
| nome longo (mais de 60 caracteres) | idem: linha de conteúdo, não título |
| nome em caixa mista com conteúdo na mesma linha | `Seguridade social: origem e evolução` é frase, não disciplina |
| medida (`TOTAL: 45 pontos`, `Duração: 4 horas`) | tem forma de disciplina, mas o número é quantidade — 197 linhas assim existem no corpus |
| rótulo de unidade e agrupamento (`CARGO:`, `CONHECIMENTOS GERAIS:`) | organizam o documento, não são disciplina |
| seção (`REFERÊNCIAS BIBLIOGRÁFICAS`, `ANEXO`) | encerra o conteúdo; sem isso a bibliografia entrava como matéria |

O sub-nível não vira tópico: `(?<![\d.])(\d{1,3})\s+` exige item de topo, então
`1.1` e `2.5.3` ficam dentro do item `1` e `2`. O número vira código do tópico,
nunca parte do nome.

O caminho da matéria é hierárquico: uma unidade de nível 1 nova substitui a
anterior do mesmo tipo, e o agrupamento acumula —
`CARGO → CONHECIMENTOS ESPECÍFICOS → MATÉRIA`.

### 18.0.8 Medição da 02.0.10

Mesma medição dos 23 PDFs, antes e depois:

| | Antes | Depois |
| --- | --- | --- |
| Extraem estrutura | 13 | **16** |
| Perfil B (Ponta Porã nº1, nº2, Ilhabela) | `NAO_IDENTIFICADO`, 0 matérias | `PROCESSADO`, 31/4/12 matérias |
| Regressões | — | **0** |

Os três documentos do Perfil B:

| Documento | Antes | Depois |
| --------- | ----- | ------ |
| Ponta Porã nº1 | NAO_IDENTIFICADO / 0 / 0 | PROCESSADO / 31 / 355 |
| Ponta Porã nº2 | NAO_IDENTIFICADO / 0 / 0 | PROCESSADO / 4 / 10 |
| Ilhabela | NAO_IDENTIFICADO / 0 / 0 | PROCESSADO / 12 / 12 |

Um quarto documento mudou — `edital_n_4_completo` (TRANSPETRO). Não estava
previsto, mas a comparação mostra que é melhora: antes a leitura por linha
própria produzia como matérias os campos de um formulário
(`CONTEÚDOS PROGRAMÁTICOS`, `PETROBRAS TRANSPORTE S.A - TRANSPETRO`,
`Inscrições. 12/08 a 14/09/2026`, e datas soltas como `09/09/2026`); agora
produz as disciplinas reais (`LÍNGUA PORTUGUESA`, `LÍNGUA INGLESA`,
`ADMINISTRAÇÃO FINANCEIRA E ORÇAMENTÁRIA`). O status sobe de `PARCIAL` para
`PROCESSADO`.

Os outros 19 documentos mantiveram perfil, status, matérias e tópicos
idênticos. As 10 linhas que continuam em `NAO_IDENTIFICADO` são: 4 documentos
realmente sem conteúdo programático, 3 processos seletivos FUNCAMP sem anexo de
programa, e 3 que o limite desta etapa não cobriu.

### 18.0.9 Limites conhecidos desta etapa

Três coisas ficaram fora, de propósito:

1. **Título de disciplina quebrado em duas linhas.** Em Ponta Porã nº1, o
   título `REALIDADE ÉTNICA, SOCIAL, HISTÓRICA, GEOGRÁFICA, CULTURAL, POLÍTICA
   E ECONÔMICA DO MUNICÍPIO DE PONTA PORÃ/MS` (109 caracteres) é impresso em
   duas linhas pelo PDF, e a segunda começa em `DE PONTA PORÃ/MS : 1 …`. Nenhum
   limite de tamanho resolve isso: o nome está fisicamente partido. Resolver
   exige juntar a linha de continuação antes de classificar — é a mesma
   reconstrução de parágrafo que a leitura por linha própria já faz para o
   conteúdo, aplicada ao título. Ficou fora porque não estava entre as três
   variações comprovadas e mexer nisso afeta o classificador compartilhado.

2. **Forma `GRUPO: DISCIPLINA. conteúdo`.** Em Ilhabela, o segundo cargo tem
   `CONHECIMENTOS ESPECÍFICOS: Direito Constitucional: Constituição: conceito e
   espécies…` — agrupamento, disciplina e conteúdo na mesma linha, com três
   dois-pontos. As 3 matérias desse trecho ficaram sem unidade no caminho. É
   uma quarta variação, vista em um documento só.

3. **`ExtratorDisciplinasInline` não é um interpretador de itens completo.** O
   conteúdo de um item que contém dois-pontos pode absorver o título seguinte
   quando o título é justamente o que foi partido (caso 1). Nos outros dois
   documentos isso não ocorre.

Também foi observado, e não corrigido por estar fora do escopo: o pacote
`domain/edital/extractor` (ExtratorFCC, ExtratorPadrao, ExtratorFallback,
ExtratorDisciplinaCabecalho, ExtratorEditalFactory, ExtratorEditalFacade,
ExtratorEditalBase) **não é referenciado por nenhuma classe de produção** —
é código morto anterior à reescrita da interpretação, mantido apenas com seus
testes.

## Como reproduzir a medição

```bash
export JAVA_HOME=~/jdk
export PATH=$JAVA_HOME/bin:$PATH
./gradlew test --tests "*DiagnosticoPerfisReaisTest*"
cat build/diagnostico-perfis.tsv
```

O arquivo é regerado a cada execução. Rodar antes e depois de uma mudança de
estratégia é o que permite comparar A × B por número, e não por impressão.
