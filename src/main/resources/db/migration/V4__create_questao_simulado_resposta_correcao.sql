-- ============================================
-- V4 - Questão, Simulado, Resposta e Correção
--
-- Cria as tabelas que sustentam o núcleo funcional:
--   questao            (banco de questões)
--   simulado           (tentativa do usuário)
--   simulado_questao   (questões selecionadas na tentativa)
--   resposta           (resposta do usuário por questão)
--   correcao           (resultado objetivo por questão)
-- ============================================

-- ============================================
-- TABELA QUESTAO
-- ============================================
CREATE TABLE IF NOT EXISTS questao (
                                       id               BIGSERIAL    PRIMARY KEY,
                                       materia_id       BIGINT       NOT NULL,
                                       topico_id        BIGINT,
                                       enunciado        TEXT         NOT NULL,
    tipo             VARCHAR(20)  NOT NULL,
    alternativas     JSONB,
    resposta_correta VARCHAR(10)  NOT NULL,
    justificativa    TEXT,
    banca            VARCHAR(100),
    ano              INTEGER,
    dificuldade      VARCHAR(20),
    origem           VARCHAR(20)  NOT NULL DEFAULT 'USUARIO',
    referencia       VARCHAR(255),
    ativo            BOOLEAN      NOT NULL DEFAULT TRUE,
    criado_em        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_questao_materia
    FOREIGN KEY (materia_id) REFERENCES materia(id) ON DELETE CASCADE,
    CONSTRAINT fk_questao_topico
    FOREIGN KEY (topico_id) REFERENCES topico(id) ON DELETE SET NULL
    );

CREATE INDEX IF NOT EXISTS idx_questao_materia_id ON questao(materia_id);
CREATE INDEX IF NOT EXISTS idx_questao_topico_id ON questao(topico_id);
CREATE INDEX IF NOT EXISTS idx_questao_materia_ativo ON questao(materia_id, ativo);
CREATE INDEX IF NOT EXISTS idx_questao_topico_ativo ON questao(topico_id, ativo);
CREATE INDEX IF NOT EXISTS idx_questao_banca ON questao(banca);
CREATE INDEX IF NOT EXISTS idx_questao_tipo ON questao(tipo);

-- ============================================
-- TABELA SIMULADO
-- ============================================
CREATE TABLE IF NOT EXISTS simulado (
                                        id                   BIGSERIAL    PRIMARY KEY,
                                        usuario_id           BIGINT       NOT NULL,
                                        concurso_id          BIGINT       NOT NULL,
                                        materia_id           BIGINT       NOT NULL,
                                        titulo               VARCHAR(255),
    descricao            TEXT,
    status               VARCHAR(20)  NOT NULL DEFAULT 'CRIADO',
    quantidade_questoes  INTEGER,
    criado_em            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    iniciado_em          TIMESTAMP,
    finalizado_em        TIMESTAMP,
    atualizado_em        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_simulado_usuario
    FOREIGN KEY (usuario_id) REFERENCES usuario(id) ON DELETE CASCADE,
    CONSTRAINT fk_simulado_concurso
    FOREIGN KEY (concurso_id) REFERENCES concurso(id) ON DELETE CASCADE,
    CONSTRAINT fk_simulado_materia
    FOREIGN KEY (materia_id) REFERENCES materia(id) ON DELETE CASCADE
    );

CREATE INDEX IF NOT EXISTS idx_simulado_usuario_id ON simulado(usuario_id);
CREATE INDEX IF NOT EXISTS idx_simulado_concurso_id ON simulado(concurso_id);
CREATE INDEX IF NOT EXISTS idx_simulado_materia_id ON simulado(materia_id);
CREATE INDEX IF NOT EXISTS idx_simulado_status ON simulado(status);
CREATE INDEX IF NOT EXISTS idx_simulado_usuario_criado ON simulado(usuario_id, criado_em DESC);

-- ============================================
-- TABELA SIMULADO_QUESTAO
-- ============================================
CREATE TABLE IF NOT EXISTS simulado_questao (
                                                id          BIGSERIAL     PRIMARY KEY,
                                                simulado_id BIGINT        NOT NULL,
                                                questao_id  BIGINT        NOT NULL,
                                                ordem       INTEGER       NOT NULL,
                                                valor       NUMERIC(10,2) NOT NULL DEFAULT 1,
                                                criado_em   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_simulado_questao_simulado
    FOREIGN KEY (simulado_id) REFERENCES simulado(id) ON DELETE CASCADE,
    CONSTRAINT fk_simulado_questao_questao
    FOREIGN KEY (questao_id) REFERENCES questao(id),
    CONSTRAINT uk_simulado_questao_questao
    UNIQUE (simulado_id, questao_id),
    CONSTRAINT uk_simulado_questao_ordem
    UNIQUE (simulado_id, ordem)
    );

CREATE INDEX IF NOT EXISTS idx_simulado_questao_simulado_id ON simulado_questao(simulado_id);
CREATE INDEX IF NOT EXISTS idx_simulado_questao_questao_id ON simulado_questao(questao_id);

-- ============================================
-- TABELA RESPOSTA
-- ============================================
-- Garante no máximo uma resposta por questão do simulado.
-- UNIQUE(simulado_questao_id) - barreira de idempotência contra registro duplicado.
CREATE TABLE IF NOT EXISTS resposta (
                                        id                     BIGSERIAL PRIMARY KEY,
                                        simulado_questao_id    BIGINT    NOT NULL,
                                        alternativa_selecionada VARCHAR(10),
    resposta_texto         TEXT,
    respondida_em          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizada_em          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    criado_em              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_resposta_simulado_questao
    FOREIGN KEY (simulado_questao_id) REFERENCES simulado_questao(id) ON DELETE CASCADE,
    CONSTRAINT uk_resposta_simulado_questao
    UNIQUE (simulado_questao_id)
    );

CREATE INDEX IF NOT EXISTS idx_resposta_simulado_questao_id ON resposta(simulado_questao_id);

-- ============================================
-- TABELA CORRECAO
-- ============================================
CREATE TABLE IF NOT EXISTS correcao (
                                        id                  BIGSERIAL PRIMARY KEY,
                                        simulado_id         BIGINT    NOT NULL,
                                        simulado_questao_id BIGINT    NOT NULL,
                                        resposta_usuario    VARCHAR(20000),
    resposta_correta    VARCHAR(10) NOT NULL,
    resultado           VARCHAR(20) NOT NULL,
    corrigida_em        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_correcao_simulado
    FOREIGN KEY (simulado_id) REFERENCES simulado(id) ON DELETE CASCADE,
    CONSTRAINT fk_correcao_simulado_questao
    FOREIGN KEY (simulado_questao_id) REFERENCES simulado_questao(id) ON DELETE CASCADE,
    CONSTRAINT uk_correcao_simulado_questao
    UNIQUE (simulado_questao_id)
    );

CREATE INDEX IF NOT EXISTS idx_correcao_simulado_id ON correcao(simulado_id);
CREATE INDEX IF NOT EXISTS idx_correcao_simulado_questao_id ON correcao(simulado_questao_id);
CREATE INDEX IF NOT EXISTS idx_correcao_resultado ON correcao(resultado);
