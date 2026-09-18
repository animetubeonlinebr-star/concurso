# Demanda 02.0.11 — Matriz arquitetural dos formatos reais

Análise dos 23 editais de `PDF_edital_teste` depois da 02.0.10, para decidir o
que deve permanecer genérico e o que deve variar por formato.

Não implementa nada: mede, classifica e recomenda. Complementa
`ARQUITETURA_INTERPRETADOR.md` (decisões de desenho) e `projeto.md` (sequência).

## Como a medição foi feita

`MatrizCapacidadesTest` roda o interpretador sobre os 23 PDFs e grava
`build/matriz-capacidades.tsv` com, para cada documento:

- o perfil e a profundidade da hierarquia declarada;
- **qual âncora** reconheceu o bloco, e **quais** âncoras casariam;
- **qual leitura de disciplina** venceu, e **quanto** cada leitura concorrente
  teria produzido;
- status, matérias e tópicos.

É diferente do diagnóstico da 02.0.9, que diz só o resultado. Este diz o
caminho — e é o caminho que revela a arquitetura.

## 1. Os dois eixos são independentes

O achado central da medição:

| | |
| --- | --- |
| Âncora do bloco | como o documento **declara** o conteúdo |
| Leitura de disciplina | como o documento **escreve** as disciplinas |

São decisões separadas, e a matriz prova isso:

| Âncora | Leitura | Documentos |
| ------ | ------- | ---------- |
| TITULO_EXPLICITO | LINHA_PROPRIA | 15 |
| TITULO_NUMERADO | INLINE | 2 |
| TITULO_EXPLICITO | INLINE | 2 |
| NENHUMA | SEM_BLOCO | 4 |

As duas linhas do meio são a evidência: `TITULO_EXPLICITO` aparece com as duas
leituras, e `INLINE` aparece com as duas âncoras. **Não existe "Perfil B" como
bloco único** — existe uma âncora e uma leitura, combináveis.

O TRANSPETRO é o caso mais claro: âncora igual à dos outros 15, leitura
diferente. Se o modelo fosse "Perfil A / Perfil B", ele não caberia em nenhum
dos dois.

### Quantas âncoras casam por documento

| Âncoras que casam | Documentos |
| ----------------- | ---------- |
| 0 | 4 |
| 1 | 10 |
| 2 | 9 |

Nove documentos casam **duas** âncoras — em todos, `TITULO_EXPLICITO` e
`AGRUPAMENTO_CONHECIMENTOS`. Isso importa: a **ordem de preferência é
comportamento**, não detalhe. Se o agrupamento viesse primeiro, esses nove
documentos seriam cortados no meio do conteúdo.

| Âncora | Casa em |
| ------ | ------- |
| `AncoraTituloExplicito` | 17 de 23 |
| `AncoraAgrupamentoConhecimentos` | 9 de 23 |
| `AncoraTituloNumerado` | 2 de 23 |

## 2. Matriz por etapa do pipeline

Natureza da diferença: **ALGORITMO** (passos diferentes), **PARÂMETRO**
(mesmo código, configuração diferente), **PADRÃO** (convenção do documento),
**DADO** (informação de entrada), **EXCEÇÃO** (caso isolado).

| Etapa | Perfil A (15 docs) | Perfil B (2 docs) | TRANSPETRO | Natureza | Compartilhado? |
| ----- | ------------------ | ----------------- | ---------- | -------- | -------------- |
| DocumentModel | igual | igual | igual | DADO | ✅ comum |
| Extração de metadados | igual | igual | igual | DADO | ✅ comum |
| Classificação de perfil | decide por bloco | decide por bloco | decide por bloco | **ALGORITMO** | ❌ varia |
| Âncora do bloco | `TITULO_EXPLICITO` | `TITULO_NUMERADO` | `TITULO_EXPLICITO` | **PARÂMETRO** | ❌ varia |
| Fim do bloco | anexo/cronograma | mudança de capítulo | anexo (sem subtítulo: não fecha) | **PARÂMETRO** | ❌ varia |
| Leitura de disciplina | linha própria | inline | inline | **ALGORITMO** | ❌ varia |
| Extração de tópico | marcador/linha | item numerado | item numerado | **ALGORITMO** | ❌ varia |
| Caminho hierárquico | cargo/área/curso | cargo + agrupamento | — | **PADRÃO** | ❌ varia |
| Normalização | igual | igual | igual | — | ✅ comum |
| Validação | igual | igual | igual | — | ✅ comum |
| Status | igual | igual | igual | — | ✅ comum |

Leitura da tabela: **as pontas são comuns; o meio varia**. Entrada
(`DocumentModel`, metadados) e saída (normalização, validação, status) não
sabem de formato. O que varia é ancoragem, segmentação e leitura.

E a variação é de dois tipos apenas: **parâmetro** (qual âncora, qual regra de
fim — mesmo código, configuração diferente) e **algoritmo** (linha própria vs
inline — passos diferentes).

## 3. A hierarquia declarada nunca é usada

Achado que muda a leitura da tabela acima. `EditalProfile` declara:

```java
record EditalProfile(
        String codigo,
        String descricao,
        HierarchyDefinition hierarquia,   // ← nunca lido
        List<String> marcadoresSecao,     // ← nunca lido
        List<String> marcadoresItem,      // ← nunca lido
        boolean tratarCargoComoNivel      // ← nunca lido
)
```

Verificado por busca: `hierarquia()` é consumido **apenas em um `log.info`**;
`marcadoresSecao()`, `marcadoresItem()` e `tratarCargoComoNivel()` não têm
nenhum consumidor fora do próprio record.

Consequência: a linha "Caminho hierárquico" da matriz **não é produzida pela
hierarquia declarada** — é produzida linha a linha pelo classificador e pelo
extrator. Ou seja, hoje existem duas descrições de hierarquia no sistema, e a
que está no perfil é a que não vale.

Isso é a evidência mais forte a favor da árvore tipada (02.0.14): o perfil
declara uma forma que o código ignora.

## 4. O gargalo de acoplamento

A pergunta: *para adicionar um quarto formato, quantas classes existentes
preciso modificar?*

`CustoDoQuartoFormatoTest` responde executando. Ele cria um formato fictício
(âncora `PROGRAMA DA PROVA`, disciplina `NOME | conteúdo`) e mede onde ele
encosta.

| Camada | Precisou mudar? |
| ------ | --------------- |
| `AncoraBlocoConteudo` + implementação nova | ✅ só acrescentar |
| `SegmentadorEdital` | ❌ não |
| `ExtratorDisciplinas` / `ExtratorDisciplinasInline` | ❌ não |
| `InterpretadorEditalTemplate` / Facade | ❌ não |
| `NormalizadorEstrutura` | ❌ não |
| `AvaliadorQualidadeExtracao` | ❌ não |
| `StatusExtracao` | ❌ não |
| **`EditalProfileClassifier`** | ⚠️ **sim** |

O classificador é o único ponto que precisa ser editado, e por um motivo
concreto: ele **duplica o conhecimento das âncoras**. `BLOCO_CONTEUDO`,
`CONHECIMENTOS` e `CONHECIMENTOS_NUMERADO` são os mesmos padrões que
`AncoraTituloExplicito`, `AncoraAgrupamentoConhecimentos` e
`AncoraTituloNumerado` já sabem reconhecer.

O classificador decide primeiro (`ehDocumentoDeConteudo`) e só depois as
âncoras localizam. Enquanto ele não souber a forma nova, o documento é
classificado como `SEM_CONTEUDO` e a âncora nunca é consultada — o teste mostra
exatamente isso: a estratégia lê o formato fictício corretamente quando
chamada direto, e o fluxo completo devolve zero matérias.

**Recomendação para a 02.0.12:** o classificador deve perguntar às âncoras
registradas em vez de manter os próprios padrões. Com isso o custo de um formato
novo passa a ser *uma classe nova*, sem edição em classe compartilhada.

## 5. Decisão sobre Abstract Factory

**Rejeitada de novo, agora com medição.**

A matriz mostra que os componentes das pontas são idênticos em todos os
formatos: `DocumentModel`, normalização, validação e status não variam em
nenhum dos 23 documentos. Uma Abstract Factory exigiria famílias de objetos
completas, e as famílias teriam membros idênticos — o resultado seria
duplicação de código que não varia.

O que varia é **configuração** (qual âncora, qual regra de fim, qual ordem) e
**um passo de algoritmo** (linha própria vs inline). Configuração já é o
`EditalProfile`; o passo de algoritmo é o ponto de extensão que a estratégia
oferece.

**Decisão registrada:** Template Method + Strategy + componentes especializados.
Abstract Factory fica postergada até existir evidência de duas ou mais famílias
de objetos que precisem variar **conjuntamente**. Hoje a evidência aponta o
contrário: as variações são independentes entre si.

## 6. Comportamentos aceitos e limitações

Registrados como teste, não como observação solta:

| Situação | Onde | Status |
| -------- | ---- | ------ |
| TRANSPETRO lê disciplinas reais e não campos de formulário | `TranspetroExtracaoTest` | ✅ melhoria verificada |
| TRANSPETRO: o bloco não é fechado por anexo com subtítulo | `TranspetroExtracaoTest` | ⚠️ inofensivo, medido |
| Ilhabela: agrupamento do cargo anterior vaza para o seguinte | `PerfilBExtracaoTest` | ⚠️ limitado |
| Ponta Porã nº1: título partido em duas linhas pelo PDF | `ARQUITETURA_INTERPRETADOR.md` §18.0.9 | ⚠️ limitado |
| `domain/edital/extractor` é código morto | grep de referências | ⚠️ dívida |

Sobre o TRANSPETRO: `ANEXO V - CRONOGRAMA` não fecha o bloco porque a regra de
fim exige a linha do anexo sozinha (`ANEXO V`), e este edital sempre escreve o
anexo com subtítulo. O bloco vai até o fim do documento. Hoje é inofensivo — a
leitura de disciplina é conservadora e não classifica campo de formulário — mas
está medido, para que a 02.0.12 decida se vale generalizar a regra de fim.

Sobre Ilhabela: o caminho fica na ordem de aparição
(`[CONHECIMENTOS ESPECÍFICOS:, CARGO: 302]` em vez de
`[CARGO: 302, CONHECIMENTOS ESPECÍFICOS:]`). A causa é o caminho ser uma lista
sem noção de profundidade — o que a árvore tipada resolve por construção.

## 7. O que a 02.0.12 deve consolidar

Com a evidência acima, três consolidações têm justificativa medida:

1. **Classificador sem padrões próprios.** Passa a consultar as âncoras
   registradas. Elimina o único ponto de acoplamento encontrado e remove a
   duplicação de conhecimento de âncora.

2. **Campo morto sai do perfil.** `marcadoresSecao`, `marcadoresItem` e
   `tratarCargoComoNivel` não têm consumidor. Ou ganham consumidor, ou saem —
   manter uma declaração que o código ignora é pior que não declará-la.

3. **Hierarquia: uma descrição só.** Hoje o perfil declara uma hierarquia e o
   código produz outra. A 02.0.14 (árvore tipada) é onde isso se resolve; até
   lá, `hierarquia()` deve parar de ser tratada como se descrevesse o
   comportamento.

O que **não** deve ser feito na 02.0.12: corrigir as limitações do §6. Elas são
evidência para a árvore tipada, não defeitos a remendar caso a caso. Corrigir
`GRUPO: DISCIPLINA. conteúdo` agora seria criar uma regra local antes de saber
a que família ela pertence.

## 8. Recomendação para a 02.0.14 (árvore tipada)

A árvore tipada resolve, de uma vez, o que a matriz mostrou como problemas
separados:

| Problema medido | Como a árvore resolve |
| --------------- | --------------------- |
| Caminho na ordem de aparição (Ilhabela) | nó tem profundidade, não posição na lista |
| Hierarquia declarada ≠ produzida | a árvore é a hierarquia; não há segunda descrição |
| Título partido em duas linhas | nó pode ser remontado antes de entrar na árvore |
| `caminho` é `List<String>` sem tipo | nó carrega `TipoUnidadeEdital` |
| Perfis diferentes produzem profundidades diferentes | a árvore aceita qualquer profundidade |

Forma sugerida:

```java
record UnidadeEditalRascunho(
        TipoUnidadeEdital tipo,
        String nome,
        int ordem,
        List<UnidadeEditalRascunho> filhos
)
```

`MateriaRascunho` e `TopicoRascunho` continuam existindo como a projeção que o
staging e a API consomem — a árvore é a representação intermediária, não um
substituto do contrato atual.

## Como reproduzir

```bash
export JAVA_HOME=~/jdk
export PATH=$JAVA_HOME/bin:$PATH

# matriz de capacidades (âncora, leitura, contagens)
./gradlew test --tests "*MatrizCapacidadesTest*"
cat build/matriz-capacidades.tsv

# resultado por documento (perfil, status, matérias, tópicos)
./gradlew test --tests "*DiagnosticoPerfisReaisTest*"
cat build/diagnostico-perfis.tsv

# custo de acoplamento de um formato novo
./gradlew test --tests "*CustoDoQuartoFormatoTest*"
```
