# Demanda 02 — Estruturação do Edital em Matérias e Tópicos

Registro das decisões de desenho e do fluxo implementado, para consulta
posterior. Complementa `projeto.md` (convenções de código, DTOs, logs e testes).

## Objetivo

O upload de edital deixou de ser stateless: hoje o PDF é persistido, tem hash
calculado, passa por deduplicação, é extraído deterministicamente em staging,
revisado pelo usuário e só então promovido para a hierarquia real
(`materia` / `topico`).

## Fluxo

```
Upload → Hash → Dedup → PROCESSANDO → Extração → Normalização
      → Detecção de duplicidade → Revisão → Confirmação → Persistência
```

Estados (coluna `concurso.status_processamento`):

| Estado               | Significado                                              |
| -------------------- | -------------------------------------------------------- |
| `RECEBIDO`           | Importação criada, processamento ainda não iniciou       |
| `PROCESSANDO`        | Extração em andamento                                    |
| `AGUARDANDO_REVISAO` | Staging preenchido, esperando o usuário                  |
| `CONFIRMADO`         | Estrutura promovida para `materia` / `topico`            |
| `ERRO`               | Falha na extração; `mensagem_erro` preenchida            |

### Etapas

1. **Upload** — `POST /api/v1/concursos/importar` cria o `Concurso` e o
   `EditalImportacao`, grava o PDF e devolve `202 Accepted`.
2. **Hash** — `HashService` calcula SHA-256 do arquivo. É a chave de
   deduplicação: não depende de IA nem de comparação linha a linha.
3. **Dedup** — `UNIQUE (usuario_id, hash_sha256)`. Reenviar o mesmo PDF
   devolve `409 Conflict`. Usuários diferentes não interferem entre si.
4. **Processamento** — disparado por evento `AFTER_COMMIT`
   (`ImportacaoIniciadaEvent` → `ProcessarEditalUseCase`), para não processar
   dentro da transação de upload nem travar a resposta HTTP.
5. **Extração** — `ExtratorEditalFacade` escolhe o extrator pela banca
   (`ExtratorFCC`, `ExtratorDisciplinaCabecalho`, `ExtratorPadrao`) com
   `ExtratorFallback` como rede de segurança. Tudo por regex determinístico:
   **nenhuma etapa usa IA**.
6. **Normalização** — `NormalizadorEstrutura` + `NomeNormalizer` produzem
   `nome_normalizado`, base da comparação de duplicidade e do `UNIQUE` real.
7. **Detecção de duplicidade** — `DetectorDuplicidade` marca
   `possivel_duplicidade` e `similar_a_id` dentro do staging, e
   `ja_existe_confirmada` / `ja_existe_confirmado` quando o item já está na
   hierarquia real (caso de reimportação após confirmação).
8. **Revisão** — o usuário decide o que fica. Nada é mesclado
   automaticamente.
9. **Confirmação** — `ConfirmarEstruturaUseCase` promove o staging.

## Modelo de dados

### Staging (tabelas próprias)

Matérias e tópicos ainda não confirmados não poluem a hierarquia real:

- `edital_importacao` — arquivo, hash, status, timestamps. `UNIQUE (usuario_id, hash_sha256)`.
- `materia_sugerida` / `topico_sugerido` — árvore sugerida, com
  `selecionada`, `possivel_duplicidade`, `similar_a_id`,
  `ja_existe_confirmada` / `ja_existe_confirmado`.

`hash_sha256` é `VARCHAR(64)` (e não `CHAR(64)`) porque o Hibernate está em
`ddl-auto=validate` e compara o tipo JDBC da coluna com o da entidade.

### Qualidade da extração (`status_extracao`, migration `V8`)

`status_processamento` descreve o **ciclo de vida**, não o que a extração
conseguiu ler. Um edital sem bloco de conteúdo programático chegava a
`AGUARDANDO_REVISAO` com zero matérias: o `StatusExtracao` calculado pelo
extrator (`PROCESSADO`, `PARCIAL`, `BAIXA_CONFIANCA`, `NAO_IDENTIFICADO`) era
descartado, e o usuário via um sucesso vazio.

Por isso `edital_importacao.status_extracao` guarda essa qualidade em separado:

- a tela `/processando` mostra a mensagem do `StatusExtracao` quando ele não é
  `PROCESSADO`, com precedência sobre a descrição do ciclo de vida;
- a tela `/revisao` exibe o mesmo aviso em um banner;
- `PARCIAL` surgiu na prática em edital em que órgão/ano não foram
  identificados, mesmo com matérias extraídas.

### Persistido

- `concurso.status_processamento` — estado do fluxo.
- `concurso.importacao_id` — importação que originou a estrutura corrente,
  com `ON DELETE SET NULL` (apagar a importação não apaga o concurso, que
  pode seguir com a estrutura confirmada).
- `materia` / `topico` — hierarquia real, com `origem = EDITAL`.

## Decisão sobre a camada `CURSO`

**Pendente de confirmação do usuário.**

A regra inegociável #1 declara `EDITAL → CONCURSO → MATÉRIA → TÓPICO`, com
`UNIQUE(concurso_id, nome_normalizado)`. O código implementa
`CONCURSO → CURSO → MATÉRIA → TOPICO`, criando um `Curso` "Geral" implícito
como balde, com `UNIQUE` efetivo em `(curso_id, nome_normalizado)`.

- **Opção A — manter `CURSO`**: tratar `CURSO` como o cargo/módulo do edital
  e atualizar a regra #1. Menor esforço.
- **Opção B — remover `CURSO`**: aderência literal à regra, mas exige
  migration de consolidação e ajustes em `MateriaEntity`, `MateriaRepository`,
  `MateriaService`, `TopicoEntity`, `ConcursoService` e `DataLoader`.

Enquanto não houver decisão, o código segue com a **Opção A**.

## Endpoints

| Método | Rota                                        | Uso                                            |
| ------ | ------------------------------------------- | ---------------------------------------------- |
| `POST` | `/api/v1/concursos/importar`                | Inicia a importação (`202 Accepted`)           |
| `POST` | `/api/v1/concursos/upload`                  | Pré-visualização sem persistência              |
| `GET`  | `/api/v1/concursos/{id}/processamento`      | Polling da tela `/processando`                 |
| `POST` | `/api/v1/concursos/{id}/reprocessar`        | Reinicia a extração de um edital em erro       |
| `GET`  | `/api/v1/concursos/{id}/revisao`            | Estrutura sugerida para revisão                |
| `PUT`  | `/api/v1/concursos/{id}/revisao`            | Aplica edições e devolve o estado persistido   |
| `POST` | `/api/v1/concursos/{id}/confirmar`          | Promove o staging para `materia` / `topico`    |
| `GET`  | `/api/v1/concursos/{id}/conteudo`           | Árvore confirmada                              |

`/upload` foi mantido: a tela inicial permite ajustar banca e padrão e ver o
resultado na hora. O caminho que grava dados é `/importar`.

O `PUT /revisao` devolve o estado já persistido, para a tela continuar em
sincronia sem um segundo `GET` após cada edição.

O `POST /reprocessar` chama `validarReprocessamento` **antes** de disparar: a
checagem de posse não pode depender do executor assíncrono.

## Segurança

O `usuarioId` **nunca** vem do corpo da requisição: é resolvido do token por
`UsuarioAutenticadoResolver`. O frontend não pode agir em nome de outra
pessoa. `EditalFluxoControllerTest` fixa esse contrato.

## Telas

| Rota                              | Componente           | Papel                                  |
| --------------------------------- | -------------------- | -------------------------------------- |
| `/concursos/:id/processando`      | `ProcessandoComponent` | Polling do status até liberar revisão |
| `/concursos/:id/revisao`          | `RevisaoComponent`     | Mesclagem explícita de duplicatas      |
| `/concursos/:id/conteudo`         | `ConteudoComponent`    | Árvore confirmada                      |

A mesclagem é sempre **explícita**: duplicatas são apenas sinalizadas
(`possivel_duplicidade` + `similar_a_id`), jamais fundidas sozinhas.

## Testes

Backend (`./gradlew test`) — 81 testes, 0 falhas. Inclui:

- `HashServiceTest` — vetores SHA-256 conhecidos, determinismo, formato hex,
  falha de leitura → `ARQUIVO_INVALIDO`.
- `EditalFluxoControllerTest` — endpoints do fluxo e origem do `usuarioId`.
- `ConcursoServiceIT` — cadastro manual nasce `CONFIRMADO` e `processado`
  permanece derivado de `statusProcessamento`.
- `IniciarImportacaoUseCaseTest` — hash, deduplicação e estado inicial,
  contra PostgreSQL real.
- `RevisaoEConfirmacaoIT` — revisão e confirmação ponta a ponta.
- `ProcessamentoAssincronoIT` — upload commita → processamento assíncrono
  → `AGUARDANDO_REVISAO`; e edital sem conteúdo programático →
  `NAO_IDENTIFICADO` (roteiro item 9).
- `DetectorDuplicidadeTest`, `NormalizadorEstrutura`, extratores por banca.

Frontend (`npm test`) — 13 arquivos, 38 testes, com specs para as três telas.

## Limpeza (02.9)

O pipeline legado foi removido por estar substituído pelo extrator
determinístico. Confirmado por busca de termo único que nada em `src/main`
os referenciava:

- `ExtracaoDadosService`, `ExtratorMaterias`, `ExtratorTopicos`,
  `ExtratorDadosConcurso`
- `EditalResponseParser`, `EditalPromptBuilder` (resquícios do caminho com IA)
- `DadosExtraidos`
- `ConcursoService.criarCompleto` + `CriarConcursoCompletoRequest`

Mantidos por estarem em uso: `SegmentadorEdital`, `PadroesLista`,
`ExtracaoConstants`, `NormalizadorEstrutura`, `ProcessarEditalUseCase` e
`ConfirmarEstruturaUseCase`.
