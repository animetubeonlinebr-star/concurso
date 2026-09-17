# AGENTS.md

Conhecimento operacional deste repositório (backend Spring Boot + frontend Angular).

## Build e testes do backend

O projeto exige **JDK 26** (`build.gradle`) e **PostgreSQL** em
`localhost:5432/concursos` (`postgres`/`postgres`). Sem os dois, o contexto
Spring nem sobe e todos os testes falham com
`FlywaySqlUnableToConnectToDbException` / `ConnectException`.

```bash
export JAVA_HOME=~/jdk          # Temurin 26
export PATH=$JAVA_HOME/bin:$PATH
./gradlew test                  # suite completa (inclui ITs contra Postgres real)
./gradlew bootRun               # sobe a API em :8080
```

Os testes de integração (`*IT`) **não** usam Testcontainers: batem no Postgres
local. Rodar a suite com o banco parado produz falhas de contexto que parecem
erros de código, mas não são.

### Provisionar o Postgres sem Docker

`dockerd` aqui não sobe (exige root). Alternativa usada: binários embarcados do
Zonky via Maven Central.

```bash
curl -sL -o /tmp/zp.jar "https://repo1.maven.org/maven2/io/zonky/test/postgres/embedded-postgres-binaries-linux-amd64/18.6.0/embedded-postgres-binaries-linux-amd64-18.6.0.jar"
python3 -c "import zipfile; zipfile.ZipFile('/tmp/zp.jar').extract('postgres-linux-x86_64.txz','/tmp/zp')"
mkdir -p /tmp/zp/pg && tar -xJf /tmp/zp/postgres-linux-x86_64.txz -C /tmp/zp/pg
export LD_LIBRARY_PATH=/tmp/zp/pg/lib
/tmp/zp/pg/bin/initdb -D /tmp/pgdata -U postgres --auth=trust
echo "CREATE DATABASE concursos OWNER postgres;" | /tmp/zp/pg/bin/postgres --single -D /tmp/pgdata postgres
/tmp/zp/pg/bin/pg_ctl -D /tmp/pgdata -o "-p 5432 -k /tmp -h 127.0.0.1" -l /tmp/pg.log start
```

O bundle traz só `initdb`, `pg_ctl` e `postgres` — **não há `psql`**. Para
inspecionar dados, use a própria API ou `postgres --single`.

## Testes do frontend

O projeto usa o builder `@angular/build:unit-test` (Vitest), **não** Karma.
`--browsers`/`--run` são rejeitados.

```bash
cd frontend
CI=true npx ng test --watch=false
npx ng build
```

## Convenções que já causaram bug

- **`textoExtraido` não pode ter `@Lob`.** A coluna é `text`; com `@Lob` o
  Hibernate grava um OID de large object e a leitura estoura fora de transação
  (`open-in-view=false`).
- **Listeners assíncronos de evento de upload devem ser
  `@TransactionalEventListener(AFTER_COMMIT)`.** Com `@EventListener` comum a
  thread assíncrona lê o concurso antes do commit e falha com "não encontrado",
  deixando a importação presa em `RECEBIDO`.
- `materia` referencia `curso_id`, não `concurso_id`.

## Migrations

`ddl-auto=validate`: a aplicação não sobe se `V1`–`V7` não casarem com as
entidades. Nunca altere uma migration já aplicada — crie a próxima.