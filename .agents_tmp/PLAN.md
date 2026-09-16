# 1. OBJECTIVE

Concluir a **Demanda 02 — Estruturação do Edital em Matérias e Tópicos**, transformando o upload de edital (hoje *stateless*) em um fluxo persistido, auditável e revisável pelo usuário:

```
Upload → Hash do arquivo → Verifica se já importado → Processamento
   → Extração de matérias/tópicos → Normalização → Detecção de duplicidades
   → Revisão (/concursos/{id}/processando, /revisao, /conteudo)
   → Confirmação → Persistência (Matéria → Tópico)
```

Em termos operacionais, ao final da demanda: um usuário envia um PDF, o sistema identifica se aquele arquivo já foi importado, processa a estrutura do edital, apresenta matérias/tópicos normalizados com possíveis duplicidades sinalizadas, permite revisão e, após confirmação, persiste a hierarquia de matérias e tópicos daquele concurso — consultável depois na tela de conteúdo.

---

# 2. CONTEXT SUMMARY

## 2.1 Diagnóstico da Demanda 01 (auditoria do repositório)

Não existe no repositório documento com critérios de aceite da Demanda 01 — o mapeamento abaixo foi **inferido do código**. O escopo real do repositório é **maior que "Demanda 01 apenas"** (já existem Questão, Simulado, Resposta, Correção, Desempenho e Autenticação), portanto há débito de escopo acumulado.

**O que a Demanda 01 efetivamente entregou:**

| Item | Situação | Evidência |
|---|---|---|
| Upload de PDF | ✅ Funciona | `api/controller/ConcursoUploadController.java` → `POST /api/v1/concursos/upload` |
| Extração de texto do PDF | ✅ Funciona | `infrastructure/pdf/PdfTextExtractorService.java` (PDFBox 3.0.3) |
| Extração estruturada | ⚠️ Parcial | `ExtratorEditalFacade` + `DetectorBanca` + `DetectorPadraoEdital` |
| Cadastro do concurso | ✅ Funciona | `POST /api/v1/concursos` → `ConcursoService.criar` |
| Persistência de matérias/tópicos | ✅ Funciona | `ConcursoService.criar`, `MateriaEntity`, `TopicoEntity` |
| Tela de upload | ✅ Funciona | `features/novo-concurso/` |
| Tela de confirmação | ⚠️ Funciona com contrato divergente | `features/confirmar-concurso/` |

**Lacunas confirmadas relativas ao fluxo da Demanda 02:**

1. **Nenhum hash de arquivo.** Grep por `MessageDigest`, `sha256`, `hashArquivo` em `src/main/java` retorna zero resultados.
2. **Nenhuma verificação de duplicidade de importação.** Não existe entidade `ImportacaoEdital` nem índice por hash.
3. **Upload é stateless.** `ConcursoUploadController.uploadEdital` retorna `EstruturaEditalDTO` e **não persiste nada**; não há `concursoId` para compor rotas `/concursos/{id}/...`.
4. **`textoExtraido` nunca é populado.** Existe a coluna (`V5`) e o campo em `ConcursoEntity`, mas nenhum código chama `setTextoExtraido`. Consequência: `ProcessarEditalUseCase` → `ConcursoService.buscarTextoExtraido` **sempre lançaria exceção**.
5. **Processamento não é assíncrono e não tem status.** `ConcursoEntity.processado` é um `Boolean` (não uma máquina de estados) — não há `PROCESSANDO`/`ERRO`/`REVISANDO`.
6. **Não existem as telas da demanda.** Grep por `processando|revisao|conteudo` em `frontend/src` retorna zero. `app.routes.ts` não possui `/concursos/:id/processando`, `/revisao` nem `/conteudo`.
7. **Contrato frontend ↔ backend divergente na extração.** O backend retorna `MateriaExtraida { nome, topicos: List<TopicoExtraido> }` com `TopicoExtraido { nome, codigo, detalhe }`, enquanto `frontend/src/app/core/models/dados-extraidos.model.ts` declara `MateriaExtraida { nome, topicos: string[] }`. O `verTopicos()` do frontend renderiza `t` como string — quebra com objetos.
8. **Extratores reais estão inalcançáveis ou são stubs:**
   - `ExtratorDisciplinaCabecalho` (implementação real) **nunca é retornado** por `ExtratorEditalFactory` — só FCC, Fallback e Padrao.
   - `ExtratorPadrao` é stub: retorna `EstruturaEditalDTO` vazio, com `TODO`.
   - `ExtratorFallback` é stub: retorna lista vazia, com `TODO` explícito.
   - Resultado: edital de banca desconhecida/padrão não-FCC tende a extrair **zero matérias**.
9. **Código morto/legado** (verificado com grep de termo único — cada um tem referência apenas dentro de si mesmo):
   - Pipeline antigo de regex: `ExtracaoDadosService`, `ExtratorMaterias`, `ExtratorTopicos`, `ExtratorDadosConcurso`.
   - Pipeline antigo de IA: `EditalResponseParser` e `EditalPromptBuilder` — o próprio `EditalResponseParser` documenta que a extração "agora é 100% determinística via Template Method + Strategy, então este parser está fora do fluxo principal. Mantido apenas para compatibilidade até ser removido junto com SegmentadorEdital, EditalPromptBuilder e o pipeline antigo."
   - `ConfirmarEstruturaUseCase` (`@Deprecated`), `EstruturaEditalProviderSimples` (**mock com dados fixos**), `ProcessarEditalUseCase` (caminho quebrado), `ConcursoService.criarCompleto` + `CriarConcursoCompletoRequest` (nunca utilizados).
   - **Consequência relevante:** `SegmentadorEdital` é hoje referenciado **apenas** por `EditalResponseParser` — ou seja, já está morto junto com o pipeline de IA, apesar de conter lógica útil (`INICIO_CONTEUDO`/`FIM_CONTEUDO`). A Atividade 02.2 propõe **reaproveitá-lo na extração determinística** em vez de removê-lo.
10. **Arquivos frontend órfãos:** `features/confirmar-concurso.ts` (componente vazio) e `features/confirmar-concurso.html` fora da pasta do componente roteado.

## 2.2 Estado do modelo de dados

- **Hierarquia implementada:** `CONCURSO` → `CURSO` → `MATERIA` → `TOPICO`.
  - `curso`: UNIQUE(`concurso_id`,`nome_normalizado`) — `V2__add_curso_and_nome_normalizado.sql`
  - `materia`: UNIQUE(`curso_id`,`nome_normalizado`); coluna legada `concurso_id` removida em `V3`
  - `topico`: UNIQUE(`materia_id`,`nome_normalizado`)
  - **⚠️ Divergência com a regra inegociável #1**, que declara `CONCURSO → MATÉRIA` com UNIQUE(`concurso_id`,`nome_normalizado`). Ver seção de decisões pendentes.
- **`Normalização já existe e é reutilizável:** `common/util/NomeNormalizer.normalizar` (remove acentos, colapsa espaços, uppercase), aplicada automaticamente via `@PrePersist`/`@PreUpdate` em `MateriaEntity` e `TopicoEntity`.
- **Constraints de segurança:** `MateriaService.criar` já lança `MATERIA_DUPLICADA` (`ErrorCodes.MATERIA_DUPLICADA`) por nome no mesmo curso — relevante para idempotência da confirmação.
- **Limites de extração:** `ExtracaoConstants` (`MAX_MATERIAS = 50`, `MAX_TOPICOS_POR_MATERIA = 100`, `MAX_LINHAS_PROCESSADAS = 5000`).
- Migrations existentes: `V1`…`V5`. A próxima é **`V6`**.
- `spring.jpa.hibernate.ddl-auto=validate` → **toda coluna nova exige migration**, senão a aplicação não sobe.

## 2.3 Restrições técnicas

- Backend: Spring Boot 4.1.0, Java 26, Gradle, Flyway, Spring Security + JWT, PostgreSQL (`localhost:5432/concursos`), perfil `dev` com `DataLoader`.
- `SecurityConfig`: `anyRequest().authenticated()` — todo novo endpoint exige JWT; identidade sempre via `UsuarioAutenticadoResolver` (nunca do corpo da requisição).
- Frontend: Angular 22 standalone components, Angular Material, `apiUrl = '/api/v1'` relativo.
- Regra #3 (IA não é autoridade): o fluxo desta demanda é **100% determinístico** — usa regex/detecção, sem IA. `IAGateway` existe apenas como `FakeIAGateway` e permanece intocado.
- Testes atuais: 5 arquivos (`ConcursosApplicationTests`, `NomeNormalizerTest`, `ConcursoControllerTest`, `ExtratorDisciplinaCabecalhoTest`, `ExtracaoDadosServiceTest`). Sem Testcontainers e sem `src/test/resources`.

---

# 3. APPROACH OVERVIEW

**Estratégia geral:** introduzir uma **área de staging** para a estrutura extraída e converter o upload stateless em um fluxo persistido com estados. A extração determinística passa a gravar em staging; a revisão do usuário opera sobre o staging; a confirmação promove staging → `materia`/`topico` reais, reutilizando o `NomeNormalizer` e os unique constraints já existentes como última barreira de integridade.

**Decisões de projeto propostas (com justificativa):**

1. **Staging em tabelas dedicadas** (`edital_importacao`, `materia_sugerida`, `topico_sugerido`) em vez de reutilizar `materia`/`topico` com flag provisória.
   *Motivo:* matérias não confirmadas não devem poluir a hierarquia real — isso vazaria para `MateriaController`, simulados e desempenho. Tabelas dedicadas também isolam a detecção de duplicidades e permitem descartar/refazer a extração sem `DELETE` destrutivo. (Alternativa descartada: coluna `status=SUGERIDA` em `materia`, que exigiria filtro em todos os consumidores existentes e quebraria o unique constraint durante a revisão.)

2. **Hash SHA-256 do arquivo persistido no registro de importação**, com verificação pré-processamento.
   *Motivo:* atende diretamente o passo "Hash do arquivo → Verifica se já importado" e é a forma determinística de evitar reprocessamento e custo duplicado — alinhado ao princípio de sustentabilidade do `projeto.md`.

3. **Processamento assíncrono com máquina de estados explícita** (`RECEBIDO → PROCESSANDO → AGUARDANDO_REVISAO → CONFIRMADO`, com `ERRO`).
   *Motivo:* a existência de uma tela `/concursos/{id}/processando` implica que o usuário observa o processamento em andamento; isso exige status persistido e endpoint de polling. `Boolean processado` não é suficiente. (Alternativa descartada: processamento síncrono no upload — compatível apenas com editais pequenos e sem tela de progresso real.)

4. **Persistência do texto extraído (`textoExtraido`) durante a importação.**
   *Motivo:* sanar a lacuna do item 2.1-4, habilitando reprocessamento (`reprocessar`) sem novo upload.

5. **Corrigir a cadeia de extratores dentro desta demanda.**
   *Motivo:* sem `ExtratorPadrao`/`ExtratorFallback` implementados e sem `ExtratorDisciplinaCabecalho` plugado na factory, o passo "Extração de matérias/tópicos" simplesmente não produz resultado para a maioria dos editais — o fluxo ficaria oco. *Alternativa descartada:* postergar para a Demanda 06 (Importação de questões) — deixaria a Demanda 02 não verificável ponta a ponta.

6. **Duplicidade em duas camadas:** exata (comparação por `nome_normalizado`, já disponível) + aproximada (similaridade de tokens ≥ limiar configurável, apenas para **sinalizar**, nunca para remover automaticamente).
   *Motivo:* atende "detecção de possíveis duplicidades" mantendo o usuário como autoridade da decisão. Nada é mesclado sem ação explícita.

7. **Nenhum uso de IA.** Todo o pipeline é determinístico, conforme regra inegociável #3 e `ADR-007`.

## 3.1 Decisões pendentes (requerem sua confirmação antes de codificar)

Estes pontos **alteram o plano de forma material** e por isso não foram decididos unilateralmente:

**D1. A camada `CURSO` deve permanecer?**
A regra inegociável #1 declara `EDITAL → CONCURSO → MATÉRIA → TÓPICO`, com `Matéria` pertencendo a um único `Concurso` e `UNIQUE(concurso_id, nome_normalizado)`. O código implementa `CONCURSO → CURSO → MATÉRIA → TOPICO`, criando um `Curso` "Geral" implícito como balde, e o `UNIQUE` efetivo é `(curso_id, nome_normalizado)`.
- **Opção A — Manter `CURSO`** (menor esforço): tratar a camada `CURSO` como o "cargo/módulo" do edital, atualizar a regra #1 e seguir com `materia_sugerida` ligada a curso.
- **Opção B — Remover `CURSO`** (aderência literal à regra): exigiria migration de consolidação, mudança de unique constraint para `(concurso_id, nome_normalizado)` e ajuste de `MateriaEntity`, `MateriaRepository`, `MateriaService`, `TopicoEntity`, `ConcursoService` e `DataLoader`.
*Este plano assume a Opção A; a Opção B muda a Atividade 02.1 e 02.6.*

**D2. Manter ou remover `POST /api/v1/concursos/upload`?**
O novo `POST /importar` o substitui no fluxo. Manter o antigo como pré-visualização sem persistência evita quebrar consumidores, mas duplica caminhos. *Este plano assume remoção do fluxo de UI antigo e manutenção temporária do endpoint.*

**D3. `ConcursoEntity.processado` (Boolean) é mantido?**
Foi substituído conceitualmente por `status_processamento`. Manter por compatibilidade exige sincronizar os dois campos; remover exige atualizar `ConcursoResponse`, `ConcursoMapper`, `ConcursoControllerTest` e o frontend.

**D4. `origem` em `materia` — verificado, coluna existe.** (checagem concluída)
A coluna `origem VARCHAR(20) DEFAULT 'EDITAL'` é criada em `V1__create_tables.sql` e não é removida por `V2`/`V3`, portanto `MateriaEntity.origem` está consistente com o schema. **Não é necessária ação na `V6`** para este campo. (Alerta de processo: greps com alternação como `origem|ordem` retornaram falso-negativo nesta auditoria — conclusões negativas foram refeitas com termos únicos.)

**D5. `/concursos/{id}/conteudo` é um agregado novo ou reutiliza `GET /concursos/{id}/materias` + `GET /materias/{id}/topicos`?**
Impacta a Atividade 02.7 e o service Angular.

**D6. Escopo da correção dos extratores (item 2.1-8).**
Implementar `ExtratorPadrao` e `ExtratorFallback` de forma completa pode ser trabalho substancial e parcialmente pertence à Demanda 06 (Importação de questões). Confirmar se nesta demanda basta plugar `ExtratorDisciplinaCabecalho` na factory e entregar uma versão mínima funcional dos dois stubs, deixando o refinamento para depois.

---

# 4. IMPLEMENTATION STEPS

## Atividade 02.1 — Fundação de dados: importação, hash, status e staging

**Objetivo:** criar a base de persistência que sustenta todo o fluxo (hash, estados, área de revisão).

- **Tarefa — Migration `V6`** · *goal:* adicionar estrutura sem quebrar `ddl-auto=validate` · *method:* novo arquivo `src/main/resources/db/migration/V6__edital_importacao_e_staging.sql` com: (a) tabela `edital_importacao` (`id`, `concurso_id` FK, `usuario_id` FK, `nome_arquivo`, `hash_sha256 CHAR(64)`, `tamanho_bytes`, `status VARCHAR(30)`, `mensagem_erro`, `extraido_em`, `confirmado_em`, `criado_em`, `atualizado_em`, índice único `uk_importacao_usuario_hash (usuario_id, hash_sha256)`, índice `idx_importacao_concurso (concurso_id)`); (b) tabela `materia_sugerida` (`id`, `importacao_id` FK, `nome`, `nome_normalizado`, `ordem`, `selecionada BOOLEAN DEFAULT TRUE`, `possivel_duplicidade BOOLEAN DEFAULT FALSE`, `similar_a_id` FK self, `criado_em`); (c) tabela `topico_sugerido` (`id`, `materia_sugerida_id` FK, `nome`, `nome_normalizado`, `ordem`, `selecionado BOOLEAN DEFAULT TRUE`, `possivel_duplicidade BOOLEAN DEFAULT FALSE`, `similar_a_id` FK self, `criado_em`); (d) `ALTER TABLE concurso ADD COLUMN status_processamento VARCHAR(30) NOT NULL DEFAULT 'RECEBIDO'` e `ADD COLUMN importacao_id BIGINT` · *reference:* migrations `V1`–`V5`, `application.properties` (`ddl-auto=validate`)

- **Tarefa — Enums de domínio** · *goal:* representar o ciclo de vida · *method:* criar `StatusProcessamento` (`RECEBIDO`, `PROCESSANDO`, `AGUARDANDO_REVISAO`, `CONFIRMADO`, `ERRO`) em `domain/edital/domain/` e `StatusImportacao` (mesma taxonomia para `edital_importacao`) · *reference:* `domain/edital/domain/Banca.java`, `PadraoEdital.java` (padrão de enum existente)

- **Tarefa — Entidades JPA** · *goal:* mapear as novas tabelas · *method:* criar `EditalImportacaoEntity`, `MateriaSugeridaEntity`, `TopicoSugeridoEntity` seguindo o padrão de `MateriaEntity` (`@PrePersist`/`@PreUpdate` sincronizando `nomeNormalizado` via `NomeNormalizer`); atualizar `ConcursoEntity` com `statusProcessamento` e `importacao` (`@ManyToOne` LAZY) · *reference:* `domain/materia/entity/MateriaEntity.java`, `domain/concurso/entity/ConcursoEntity.java`

- **Tarefa — Repositories** · *goal:* consultas de verificação e leitura · *method:* `EditalImportacaoRepository` com `findByUsuario_IdAndHashSha256`, `findByConcurso_Id`, `existsByUsuario_IdAndHashSha256`; `MateriaSugeridaRepository`/`TopicoSugeridoRepository` com `findByImportacao_IdOrderByOrdemAsc` e `deleteByImportacao_Id` · *reference:* `domain/concurso/repository/ConcursoRepository.java`

- **Tarefa — Serviço de hash** · *goal:* calcular SHA-256 do arquivo de forma reutilizável · *method:* `infrastructure/hash/HashService` com `calcularSha256(InputStream)` usando `MessageDigest` e `HexFormat`; expor também `calcularSha256(byte[])` · *reference:* `infrastructure/pdf/PdfTextExtractorService.java` (mesma camada de infraestrutura)

- **Tarefa — DTOs de importação** · *goal:* contratos de resposta do fluxo · *method:* `IniciarImportacaoResponse(concursoId, importacaoId, status, jaExistia)`; `StatusProcessamentoResponse(concursoId, status, progresso, mensagemErro)`; `RevisaoEstruturaResponse(concursoId, dadosConcurso, materias, status, mensagem)` com `MateriaRevisaoResponse(id, nome, ordem, selecionada, possivelDuplicidade, similarA, topicos)` e `TopicoRevisaoResponse(id, nome, ordem, selecionado, possivelDuplicidade, similarA)`

- **Tarefa — Códigos de erro** · *goal:* erros do fluxo tipados · *method:* adicionar a `ErrorCodes`: `EDITAL_JA_IMPORTADO`, `PROCESSAMENTO_EM_ANDAMENTO`, `ESTRUTURA_NAO_REVISAVEL`, `IMPORTACAO_NAO_ENCONTRADA` · *reference:* `common/exception/ErrorCodes.java`

## Atividade 02.2 — Extração e normalização de matérias/tópicos

**Objetivo:** fazer a extração determinística realmente produzir matérias e tópicos para os padrões de edital previstos.

- **Tarefa — Plugar `ExtratorDisciplinaCabecalho` na factory** · *goal:* tornar alcançável o extrator real hoje inacessível · *method:* em `ExtratorEditalFactory.criar`, resolver por `PadraoEdital` antes do retorno padrão — `DISCIPLINA_CABECALHO → ExtratorDisciplinaCabecalho`, `LISTA_SIMPLES → ExtratorFallback`, `ENFASE_BLOCOS`/`MULTI_NIVEL`/`DESCONHECIDO → ExtratorPadrao`; manter `FCC` com precedência (hoje verificado primeiro). Nota: `ExtratorDisciplinaCabecalho` expõe `padraoSuportado()`; a factory deve resolver por `padrao` · *reference:* `domain/edital/extractor/ExtratorEditalFactory.java` (hoje só cobre `FCC`, `LISTA_SIMPLES` e o stub padrão), `impl/ExtratorDisciplinaCabecalho.java` (`padraoSuportado() = DISCIPLINA_CABECALHO`), `domain/edital/domain/PadraoEdital.java`

- **Tarefa — Implementar `ExtratorPadrao`** · *goal:* cobrir edital bem formatado/tabular · *method:* substituir o `TODO` (retorno vazio) por extração de blocos `CONTEÚDO PROGRAMÁTICO`/`ANEXO` reaproveitando `SegmentadorEdital` (já implementado, mas não plugado) e `RE_CABECALHO_DISCIPLINA`; retornar `EstruturaEditalDTO` com `status` calculado · *reference:* `impl/ExtratorPadrao.java`, `domain/edital/segmenter/SegmentadorEdital.java`, `domain/edital/dto/EstruturaEditalDTO.calcularStatus`

- **Tarefa — Implementar `ExtratorFallback`** · *goal:* remover o `TODO` que hoje descarta tudo · *method:* usar os `Pattern`s já declarados no arquivo (`CURSO_PATTERN`, `VAGAS_PATTERN`, `TAXA_PATTERN`, `DATAS_CHAVE_PATTERN`) para preencher `DadosConcurso`; para matérias/tópicos em formato de lista simples, **mover ou replicar** os padrões de lista hoje privados em `ExtratorDisciplinaCabecalho` (`RE_BULLET` para marcadores `▪ ■ ◆ • -` e `RE_ROMANO` para `I.`, `II.`) — avaliar extrair um utilitário compartilhado para evitar duplicação · *reference:* `impl/ExtratorFallback.java`, `impl/ExtratorDisciplinaCabecalho.java` (`RE_BULLET`, `RE_ROMANO`), `domain/edital/dto/DadosConcurso.java`

- **Tarefa — Normalização e higienização** · *goal:* garantir nomes limpos antes do staging · *method:* aplicar `NomeNormalizer.normalizar` no `nome_normalizado` de cada sugestão; descartar nomes abaixo de `MIN_MATERIA_LENGTH`/`MIN_TOPICO_LENGTH`, acima de `MAX_MATERIA_LENGTH`/`MAX_TOPICO_LENGTH`, linhas de tabela (`PADRAO_LINHA_TABELA`) e duplicatas dentro da mesma extração; respeitar `MAX_MATERIAS`/`MAX_TOPICOS_POR_MATERIA` · *reference:* `common/util/NomeNormalizer.java`, `domain/edital/service/ExtracaoConstants.java`

- **Tarefa — Testes unitários de extração** · *goal:* validar os três extratores com amostras reais · *method:* ampliar `ExtratorDisciplinaCabecalhoTest` e criar testes para `ExtratorPadrao` e `ExtratorFallback` com trechos de edital; caso no `ExtratorEditalFactoryTest` assegurando que `DISCIPLINA_CABECALHO` **não** retorna mais o stub · *reference:* `src/test/java/.../extractor/impl/ExtratorDisciplinaCabecalhoTest.java`

## Atividade 02.3 — Detecção de duplicidades

**Objetivo:** sinalizar possíveis matérias/tópicos duplicados para decisão do usuário.

- **Tarefa — `DetectorDuplicidade` (exata)** · *goal:* identificar colisões por chave normalizada · *method:* service que agrupa por `nomeNormalizado` e marca `possivelDuplicidade = true` + `similarAId` nas ocorrências repetidas · *reference:* `NomeNormalizer` + unique constraints de `MateriaEntity`/`TopicoEntity`

- **Tarefa — Similaridade aproximada de tokens** · *goal:* detectar duplicidade semântica simples (ex.: "Direito Constitucional" vs "Noções de Direito Constitucional") · *method:* comparação determinística por sobreposição de tokens (`Jaccard` sobre tokens normalizados) com limiar configurável em `application.properties` (ex.: `edital.duplicidade.limiar=0.7`); apenas sinaliza · *reference:* `application.properties` (padrão de propriedade já usado em `jwt.*`)

- **Tarefa — Detecção contra o conteúdo já persistido** · *goal:* alertar quando o tópico/matéria já existe no concurso · *method:* marcar sugestões cujo `nomeNormalizado` já exista em `materia`/`topico` do mesmo concurso, sinalizando conflito com o item já gravado · *reference:* `MateriaRepository`, `TopicoRepository`

## Atividade 02.4 — Upload persistente com hash e verificação de duplicidade

**Objetivo:** converter o upload stateless em uma importação persistida e deduplicada por hash.

- **Tarefa — Refatorar `ConcursoUploadController`** · *goal:* gerar `concursoId` para as rotas `/concursos/{id}/...` · *method:* novo endpoint `POST /api/v1/concursos/importar` (multipart `arquivo`, opcionais `banca`/`padrao`) que: resolve usuário via `UsuarioAutenticadoResolver`; valida o arquivo (reaproveitando `validarArquivo`); calcula SHA-256 via `HashService`; consulta `existsByUsuario_IdAndHashSha256`; se já importado retorna `409` com `EDITAL_JA_IMPORTADO` e o `concursoId` existente; senão cria `ConcursoEntity` + `EditalImportacaoEntity` com `textoExtraido` preenchido (via `PdfTextExtractorService`) e status `RECEBIDO`, e retorna `202 Accepted` com `IniciarImportacaoResponse(jaExistia=false)` · *reference:* `api/controller/ConcursoUploadController.java`, `ConcursoService.buscarTextoExtraido` (lacuna do item 2.1-4), `common/security/UsuarioAutenticadoResolver.java`

- **Tarefa — Caso de uso `IniciarImportacaoUseCase`** · *goal:* tirar orquestração do controller (Clean Architecture do `projeto.md`) · *method:* mover hash + verificação + criação + disparo do processamento para `application/edital/IniciarImportacaoUseCase`, deixando o controller apenas validar entrada e mapear HTTP · *reference:* `projeto.md` (seção Clean Architecture), `application/edital/`

- **Tarefa — Preservar o endpoint legado ou removê-lo** · *goal:* evitar duplicação de caminho de upload · *method:* decidir com o usuário entre (a) manter `POST /upload` apenas como pré-visualização sem persistência ou (b) eliminá-lo e ajustar o frontend; registrar a decisão · *reference:* `ConcursoUploadController.uploadEdital`

## Atividade 02.5 — Processamento assíncrono e status

**Objetivo:** executar a extração em background com máquina de estados observável.

- **Tarefa — Habilitar `@Async`** · *goal:* ter executor para o processamento · *method:* criar `config/AsyncConfig` com `@EnableAsync` e um `TaskExecutor` nomeado (`editalExecutor`), com pool limitado · *reference:* `config/SecurityConfig.java` (padrão de classe `@Configuration`)

- **Tarefa — Use case `ProcessarEditalUseCase` real** · *goal:* substituir o mock e o caminho quebrado · *method:* reescrever `application/edital/ProcessarEditalUseCase` para: marcar `PROCESSANDO`; ler `textoExtraido`; chamar `ExtratorEditalFacade.extrair(texto)`; aplicar normalização e `DetectorDuplicidade`; limpar staging anterior (`deleteByImportacao_Id`); gravar `materia_sugerida`/`topico_sugerido`; marcar `AGUARDANDO_REVISAO` ou `ERRO` com `mensagemErro`. **Remover a dependência de `EstruturaEditalProvider`** (hoje aponta para o mock `EstruturaEditalProviderSimples`) · *reference:* `application/edital/ProcessarEditalUseCase.java`, `domain/edital/service/EstruturaEditalProviderSimples.java` (mock a remover), `domain/edital/extractor/ExtratorEditalFacade.java`

- **Tarefa — Endpoint de status (polling)** · *goal:* alimentar a tela `/processando` · *method:* `GET /api/v1/concursos/{id}/processamento` retornando `StatusProcessamentoResponse` (status, progresso percentual aproximado, mensagem de erro), com validação de propriedade do concurso pelo usuário autenticado · *reference:* `ConcursoController` (padrão de resolução de usuário), `ConcursoService.buscarPorId(id, usuarioId)`

- **Tarefa — Endpoint de reprocessamento** · *goal:* permitir refazer a extração sem novo upload · *method:* `POST /api/v1/concursos/{id}/reprocessar`, bloqueado se status for `PROCESSANDO` (erro `PROCESSAMENTO_EM_ANDAMENTO`) · *reference:* `ErrorCodes`

- **Tarefa — Tratamento de falhas** · *goal:* não deixar importação presa em `PROCESSANDO` · *method:* `try/catch` no processamento gravando status `ERRO` + `mensagemErro`, com log estruturado; garantir que a transação de staging não deixe dados parciais · *reference:* `common/exception/GlobalExceptionHandler.java`, `@Slf4j` (padrão do projeto)

## Atividade 02.6 — Revisão e confirmação (persistência final)

**Objetivo:** expor a estrutura para revisão e promover o staging para `materia`/`topico` após confirmação.

- **Tarefa — Endpoint de revisão** · *goal:* alimentar a tela `/revisao` · *method:* `GET /api/v1/concursos/{id}/revisao` retornando `RevisaoEstruturaResponse` com matérias/tópicos sugeridos, flags de seleção e sinalizações de duplicidade; retornar `ESTRUTURA_NAO_REVISAVEL` se o status não for `AGUARDANDO_REVISAO` · *reference:* `EditalImportacaoRepository`, `MateriaSugeridaRepository`

- **Tarefa — Endpoint de atualização da revisão** · *goal:* permitir editar/renomear/selecionar/mesclar antes de confirmar · *method:* `PUT /api/v1/concursos/{id}/revisao` recebendo `RevisaoEstruturaRequest` (nomes editados, `selecionada`/`selecionado`, remoções e mesclagens) e persistindo no staging; toda mesclagem é **explícita do usuário**, nunca automática · *reference:* `MateriaSugeridaEntity`, `TopicoSugeridoEntity`

- **Tarefa — `ConfirmarEstruturaUseCase` real** · *goal:* substituir a versão `@Deprecated` e persistir de staging → hierarquia real · *method:* reescrever `application/edital/ConfirmarEstruturaUseCase` para ler as sugestões selecionadas, criar `MateriaEntity` (com `origem = OrigemMateria.EDITAL`, `ordem` sequencial, `status = ATIVO`) e `TopicoEntity` (`ativo = true`, `ordem` sequencial) reutilizando `MateriaService.criar`/`TopicoService.criar` — mantendo o unique constraint e o erro `MATERIA_DUPLICADA` como última barreira de integridade; marcar importação e concurso como `CONFIRMADO` · *reference:* `application/edital/ConfirmarEstruturaUseCase.java`, `domain/materia/service/MateriaService.java`, `domain/topico/service/TopicoService.java`, `domain/materia/domain/OrigemMateria.java`

- **Tarefa — Endpoint de confirmação** · *goal:* fechar o fluxo · *method:* `POST /api/v1/concursos/{id}/confirmar` retornando o resumo do que foi persistido (quantidade de matérias/tópicos); rejeitar confirmação fora de `AGUARDANDO_REVISAO` · *reference:* `ErrorCodes.ESTRUTURA_NAO_REVISAVEL`

- **Tarefa — Idempotência e recusa de estrutura vazia** · *goal:* evitar duplicação e confirmação inútil · *method:* abortar com erro se nenhuma matéria estiver selecionada; impedir dupla promoção da mesma importação (checar status e reutilizar a checagem de duplicidade existente) · *reference:* `ConfirmarEstruturaUseCase.confirmar` (já valida estrutura vazia), `MateriaService.criar`

## Atividade 02.7 — API de conteúdo consolidado

**Objetivo:** disponibilizar a árvore final para a tela `/conteudo`.

- **Tarefa — Endpoint de conteúdo** · *goal:* entregar a hierarquia completa do concurso · *method:* `GET /api/v1/concursos/{id}/conteudo` retornando matérias com seus tópicos e contagens (matérias/tópicos/questões quando aplicável), filtrando por `usuarioId`; criar `ConteudoConcursoResponse` · *reference:* `MateriaController.listarPorConcurso`, `TopicoController.listarPorMateria` — avaliar se o novo endpoint incorpora/substitui essas consultas para evitar sobreposição · *note:* `MateriaController` já expõe `GET /concursos/{concursoId}/materias`; decidir com o usuário se `/conteudo` é um agregado novo ou se as telas reutilizam os endpoints existentes

## Atividade 02.8 — Frontend: fluxo de telas

**Objetivo:** implementar as três telas da demanda e ajustar o upload para o novo fluxo.

- **Tarefa — Rotas** · *goal:* existir as URLs exigidas · *method:* adicionar em `app.routes.ts` (dentro do `MainLayoutComponent` com `AuthGuard`): `concursos/:id/processando`, `concursos/:id/revisao`, `concursos/:id/conteudo` · *reference:* `frontend/src/app/app.routes.ts` — atenção: já existe `concursos/:concursoId` (`DetalheConcursoComponent`); usar o mesmo nome de parâmetro para consistência

- **Tarefa — Models e services Angular** · *goal:* alinhar contrato com o backend e corrigir a divergência do item 2.1-7 · *method:* criar `core/models/edital-importacao.model.ts` (`StatusProcessamento`, `IniciarImportacaoResponse`, `StatusProcessamentoResponse`, `RevisaoEstrutura`, `MateriaSugerida`, `TopicoSugerido`, `ConteudoConcurso`); corrigir `dados-extraidos.model.ts` para `topicos: { nome: string; codigo?: string; detalhe?: string }[]`; criar `EditalImportacaoService` com `importar()`, `buscarStatus()`, `buscarRevisao()`, `atualizarRevisao()`, `confirmar()`, `buscarConteudo()`, `reprocessar()` · *reference:* `core/models/dados-extraidos.model.ts`, `core/services/concurso.service.ts`

- **Tarefa — Tela `/concursos/:id/processando`** · *goal:* acompanhar o processamento · *method:* componente standalone com polling (RxJS `interval` + `switchMap`, com `takeUntil`/`DestroyRef`) em `buscarStatus()`; barra de progresso e etapas (padrão visual já existente em `novo-concurso`); ao receber `AGUARDANDO_REVISAO` navegar para `/revisao`; em `ERRO` exibir mensagem e ação de reprocessar · *reference:* `features/novo-concurso/novo-concurso.ts` (`simularProgresso`, `etapas`, `getStatusIcon`), `features/novo-concurso/novo-concurso.html`

- **Tarefa — Tela `/concursos/:id/revisao`** · *goal:* permitir revisar antes de persistir · *method:* componente com listagem de matérias/tópicos, checkbox de seleção (`selecionada`/`selecionado`), edição inline do nome, remoção de itens e sinalização visual de duplicidade (badge + sugestão de mesclagem); botão Confirmar chamando `confirmar()` e navegando para `/conteudo`. **Corrigir a renderização de tópicos** para o novo tipo objeto (a `verTopicos()` atual trata tópico como string) · *reference:* `features/confirmar-concurso/confirmar-concurso.ts` / `.html` (base a evoluir), `core/models/dados-extraidos.model.ts`

- **Tarefa — Tela `/concursos/:id/conteudo`** · *goal:* visualizar a estrutura confirmada · *method:* componente com árvore matéria → tópico (Angular Material `mat-tree` ou acordeão), estados de vazio e carregamento · *reference:* `features/concursos/detalhe-concurso/` (padrão de layout e `MatCard`/`MatSpinner`)

- **Tarefa — Adaptar o upload** · *goal:* ligar a tela inicial ao novo fluxo · *method:* alterar `NovoConcurso.upload()` para chamar `EditalImportacaoService.importar()` e navegar para `/concursos/:concursoId/processando`; tratar `409 EDITAL_JA_IMPORTADO` oferecendo abrir o concurso já existente · *reference:* `features/novo-concurso/novo-concurso.ts` (hoje navega para `/confirmar-concurso` via `ConcursoDadosService`)

- **Tarefa — Aposentar o fluxo antigo** · *goal:* eliminar caminho duplicado e órfãos · *method:* remover `features/confirmar-concurso` roteado e `ConcursoDadosService` (substituídos por `/revisao` baseado em backend), além dos arquivos órfãos `features/confirmar-concurso.ts` e `features/confirmar-concurso.html`; ajustar `features/concursos/detalhe-concurso` para apontar para `/conteudo` · *reference:* `app.routes.ts`, `core/services/concurso-dados.service.ts`, `features/confirmar-concurso.ts`

## Atividade 02.9 — Limpeza técnica, testes e documentação

**Objetivo:** remover o legado que a demanda tornou obsoleto e consolidar validação.

- **Tarefa — Remover código morto** · *goal:* evitar duas implementações concorrentes · *method:* excluir `ExtracaoDadosService`, `ExtratorMaterias`, `ExtratorTopicos`, `ExtratorDadosConcurso`, `EditalResponseParser`, `EditalPromptBuilder`, `EstruturaEditalProvider`/`EstruturaEditalProviderSimples`, `ConcursoService.criarCompleto` + `CriarConcursoCompletoRequest`; remover a classe de teste associada (`ExtracaoDadosServiceTest`). **Manter `SegmentadorEdital`**, que passa a ser usado pela extração determinística na Atividade 02.2 (era sua única referência viva o `EditalResponseParser`, removido aqui — logo a Atividade 02.2 precisa estar concluída antes desta) · *reference:* verificação por grep de termo único; ordem de execução importa

- **Tarefa — Alinhar `ConcursoService.criar`** · *goal:* manter o cadastro manual funcionando após a introdução dos estados · *method:* definir `statusProcessamento = CONFIRMADO` no cadastro manual (não passa por extração) e ajustar `marcarComoProcessado` / `ConcursoMapper` para o novo campo · *reference:* `ConcursoService.criar`, `ConcursoMapper`, `ConcursoResponse` (`processado` — decidir se é mantido por compatibilidade ou substituído por `statusProcessamento`)

- **Tarefa — Testes de backend** · *goal:* cobrir as regras novas · *method:* unitários com Mockito para `HashService` (vetor conhecido), `DetectorDuplicidade` (exato e aproximado), `IniciarImportacaoUseCase` (novo vs. já importado), `ProcessarEditalUseCase` (sucesso, extração vazia, exceção) e `ConfirmarEstruturaUseCase` (promoção, vazio, duplicado); testes de controller para `importar`, `processamento`, `revisao` e `confirmar` seguindo o padrão de `ConcursoControllerTest` (mocks de `ConcursoService`/`UsuarioRepository`/`Authentication`) · *reference:* `src/test/java/.../ConcursoControllerTest.java`

- **Tarefa — Testes de frontend** · *goal:* cobrir os componentes novos · *method:* specs Vitest/Jasmine para `ProcessandoComponent` (transições de status) e `RevisaoComponent` (seleção/edição/mesclagem) · *reference:* `package.json` (`vitest`, `@types/jasmine`, script `npm test`)

- **Tarefa — Documentação** · *goal:* registrar decisões e o fluxo · *method:* criar `PLAN_DEMANDA_02.md` consolidando Atividades/Tarefas executadas, o desenho das tabelas de staging, a decisão sobre a camada `CURSO` e exemplos de chamadas dos novos endpoints · *reference:* `projeto/projeto.md` (convenções: DTOs, logs, testes, documentação)

---

# 5. TESTING AND VALIDATION

**Critérios de sucesso (a demanda está concluída quando):**

1. Enviar um PDF cria um concurso com `statusProcessamento` evoluindo `RECEBIDO → PROCESSANDO → AGUARDANDO_REVISAO`, com `textoExtraido` persistido.
2. Enviar **o mesmo PDF novamente** não reprocessa: retorna `409` com `EDITAL_JA_IMPORTADO` e o `concursoId` existente (chave `(usuario_id, hash_sha256)`).
3. Uma importação de outro usuário com o mesmo arquivo **não** é bloqueada indevidamente.
4. `GET /concursos/{id}/revisao` retorna matérias/tópicos normalizados, com `possivelDuplicidade` sinalizado quando houver colisão por `nome_normalizado` ou similaridade acima do limiar.
5. `POST /concursos/{id}/confirmar` persiste `materia` (com `origem = EDITAL`) e `topico` reais, marcando `CONFIRMADO`; confirmar duas vezes não duplica registros.
6. `GET /concursos/{id}/conteudo` devolve a hierarquia confirmada; a tela `/conteudo` a renderiza.
7. `/concursos/{id}/processando` e `/concursos/{id}/revisao` são acessíveis apenas para o dono do concurso (JWT) e redirecionam corretamente conforme o status.

**Verificação automatizada**

- Backend: `./gradlew test` e `./gradlew build` verdes.
- Frontend: `npm run build` e `npm test` verdes.
- Requisito de ambiente: PostgreSQL ativo em `localhost:5432/concursos`; a migration `V6` deve aplicar limpo sobre `V1`–`V5` (a aplicação não sobe se `validate` falhar).

**Verificação manual ponta a ponta (roteiro)**

1. Subir backend e frontend; autenticar.
2. `POST /api/v1/concursos/importar` com um edital real → observar `202` e `concursoId`.
3. Acompanhar `GET /api/v1/concursos/{id}/processamento` até `AGUARDANDO_REVISAO`.
4. Abrir `/concursos/{id}/processando` → confirmar transição para `/revisao`.
5. Na revisão: desmarcar uma matéria, renomear um tópico, aceitar uma mesclagem sugerida → Confirmar.
6. Conferir no banco: `materia`/`topico` gravados com `nome_normalizado` correto e sem duplicatas; `concurso.status_processamento = 'CONFIRMADO'`.
7. Abrir `/concursos/{id}/conteudo` → árvore exibida.
8. Repetir o upload do mesmo arquivo → `409` tratado na UI.
9. Testar edital **sem** bloco de conteúdo programático → status `PARCIAL`/`NAO_IDENTIFICADO` em vez de falha silenciosa.
10. Testar edital de banca desconhecida (não-FCC) com cabeçalhos de disciplina → confirmar que matérias **são** extraídas (valida a correção da factory).

**Regressão a observar**

- Cadastro manual de concurso (`POST /api/v1/concursos`) e telas `lista-concursos` / `detalhe-concurso` continuam funcionando.
- Nenhum módulo fora do escopo (Questão, Simulado, Resposta, Correção, Desempenho) é alterado — apenas consumido como referência.
