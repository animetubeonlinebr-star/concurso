-- ============================================
-- TABELA USUARIO
-- ============================================
CREATE TABLE IF NOT EXISTS usuario (
                                       id BIGSERIAL PRIMARY KEY,
                                       nome VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    senha_hash VARCHAR(255) NOT NULL,
    perfil VARCHAR(20) NOT NULL DEFAULT 'USER',
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    );

-- ============================================
-- TABELA CONCURSO
-- ============================================
CREATE TABLE IF NOT EXISTS concurso (
                                        id BIGSERIAL PRIMARY KEY,
                                        nome VARCHAR(200) NOT NULL,
    orgao VARCHAR(200),
    banca VARCHAR(100),
    ano INTEGER,
    usuario_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ATIVO',
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_concurso_usuario FOREIGN KEY (usuario_id) REFERENCES usuario(id) ON DELETE CASCADE
    );

-- ============================================
-- TABELA MATERIA
-- ============================================
CREATE TABLE IF NOT EXISTS materia (
                                       id BIGSERIAL PRIMARY KEY,
                                       nome VARCHAR(200) NOT NULL,
    concurso_id BIGINT NOT NULL,
    ordem INTEGER DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'ATIVO',
    origem VARCHAR(20) DEFAULT 'EDITAL',
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_materia_concurso FOREIGN KEY (concurso_id) REFERENCES concurso(id) ON DELETE CASCADE
    );

-- ============================================
-- TABELA TOPICO
-- ============================================
CREATE TABLE IF NOT EXISTS topico (
                                      id BIGSERIAL PRIMARY KEY,
                                      nome VARCHAR(200) NOT NULL,
    materia_id BIGINT NOT NULL,
    ordem INTEGER DEFAULT 0,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_topico_materia FOREIGN KEY (materia_id) REFERENCES materia(id) ON DELETE CASCADE
    );

-- ============================================
-- ÍNDICES (otimização de consultas)
-- ============================================
CREATE INDEX idx_concurso_usuario ON concurso(usuario_id);
CREATE INDEX idx_materia_concurso ON materia(concurso_id);
CREATE INDEX idx_topico_materia ON topico(materia_id);