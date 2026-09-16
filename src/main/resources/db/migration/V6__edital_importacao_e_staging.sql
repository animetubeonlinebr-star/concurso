-- ============================================
-- V6 - Importação de edital: hash, status e staging de revisão
--
-- Converte o upload stateless em um fluxo persistido:
--   upload -> hash -> verificação de duplicidade -> processamento
--   -> staging (matérias/tópicos sugeridos) -> revisão -> confirmação
--
-- O staging vive em tabelas próprias para que matérias ainda não
-- confirmadas não poluam a hierarquia real (materia/topico).
-- ============================================

-- ============================================
-- EDITAL_IMPORTACAO
-- ============================================
-- hash_sha256 é VARCHAR (e não CHAR) porque o Hibernate está em
-- ddl-auto=validate e compara o tipo JDBC da coluna com o da entidade.
CREATE TABLE IF NOT EXISTS edital_importacao (
    id            BIGSERIAL    PRIMARY KEY,
    concurso_id   BIGINT       NOT NULL,
    usuario_id    BIGINT       NOT NULL,
    nome_arquivo  VARCHAR(255) NOT NULL,
    hash_sha256   VARCHAR(64)  NOT NULL,
    tamanho_bytes BIGINT,
    status        VARCHAR(30)  NOT NULL DEFAULT 'RECEBIDO',
    mensagem_erro TEXT,
    extraido_em   TIMESTAMP WITHOUT TIME ZONE,
    confirmado_em TIMESTAMP WITHOUT TIME ZONE,
    criado_em     TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP WITHOUT TIME ZONE,

    CONSTRAINT fk_importacao_concurso
        FOREIGN KEY (concurso_id) REFERENCES concurso(id) ON DELETE CASCADE,
    CONSTRAINT fk_importacao_usuario
        FOREIGN KEY (usuario_id) REFERENCES usuario(id) ON DELETE CASCADE,
    -- Deduplicação por arquivo: o mesmo PDF não é processado duas vezes
    -- pelo mesmo usuário. Usuários diferentes não interferem entre si.
    CONSTRAINT uk_importacao_usuario_hash
        UNIQUE (usuario_id, hash_sha256)
);

CREATE INDEX IF NOT EXISTS idx_importacao_concurso ON edital_importacao(concurso_id);

-- ============================================
-- MATERIA_SUGERIDA (staging da revisão)
-- ============================================
CREATE TABLE IF NOT EXISTS materia_sugerida (
    id                   BIGSERIAL    PRIMARY KEY,
    importacao_id        BIGINT       NOT NULL,
    nome                 VARCHAR(150) NOT NULL,
    nome_normalizado     VARCHAR(255) NOT NULL,
    ordem                INTEGER      NOT NULL DEFAULT 0,
    selecionada          BOOLEAN      NOT NULL DEFAULT TRUE,
    possivel_duplicidade BOOLEAN      NOT NULL DEFAULT FALSE,
    similar_a_id         BIGINT,
    criado_em            TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_materia_sugerida_importacao
        FOREIGN KEY (importacao_id) REFERENCES edital_importacao(id) ON DELETE CASCADE,
    CONSTRAINT fk_materia_sugerida_similar
        FOREIGN KEY (similar_a_id) REFERENCES materia_sugerida(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_materia_sugerida_importacao ON materia_sugerida(importacao_id);

-- ============================================
-- TOPICO_SUGERIDO (staging da revisão)
-- ============================================
CREATE TABLE IF NOT EXISTS topico_sugerido (
    id                   BIGSERIAL    PRIMARY KEY,
    materia_sugerida_id  BIGINT       NOT NULL,
    nome                 VARCHAR(150) NOT NULL,
    nome_normalizado     VARCHAR(255) NOT NULL,
    ordem                INTEGER      NOT NULL DEFAULT 0,
    selecionado          BOOLEAN      NOT NULL DEFAULT TRUE,
    possivel_duplicidade BOOLEAN      NOT NULL DEFAULT FALSE,
    similar_a_id         BIGINT,
    criado_em            TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_topico_sugerido_materia
        FOREIGN KEY (materia_sugerida_id) REFERENCES materia_sugerida(id) ON DELETE CASCADE,
    CONSTRAINT fk_topico_sugerido_similar
        FOREIGN KEY (similar_a_id) REFERENCES topico_sugerido(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_topico_sugerido_materia ON topico_sugerido(materia_sugerida_id);

-- ============================================
-- CONCURSO: status de processamento e importação corrente
-- ============================================
-- A coluna nasce nullable para permitir o backfill das linhas existentes:
-- concursos cadastrados manualmente (processado = true) já estão confirmados;
-- os demais nunca passaram por extração.
ALTER TABLE concurso ADD COLUMN IF NOT EXISTS status_processamento VARCHAR(30);

UPDATE concurso
SET status_processamento = CASE WHEN processado THEN 'CONFIRMADO' ELSE 'RECEBIDO' END
WHERE status_processamento IS NULL;

ALTER TABLE concurso ALTER COLUMN status_processamento SET NOT NULL;
ALTER TABLE concurso ALTER COLUMN status_processamento SET DEFAULT 'RECEBIDO';

ALTER TABLE concurso ADD COLUMN IF NOT EXISTS importacao_id BIGINT;

-- ON DELETE SET NULL: apagar a importação não apaga o concurso, que pode
-- continuar existindo com a estrutura já confirmada.
ALTER TABLE concurso DROP CONSTRAINT IF EXISTS fk_concurso_importacao;
ALTER TABLE concurso
    ADD CONSTRAINT fk_concurso_importacao
        FOREIGN KEY (importacao_id) REFERENCES edital_importacao(id) ON DELETE SET NULL;