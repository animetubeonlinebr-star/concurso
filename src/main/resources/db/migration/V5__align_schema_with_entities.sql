-- ============================================
-- V5 - Alinhamento do schema com as entidades
--
-- As migrações V1-V3 foram criadas antes de alguns atributos das entidades
-- (cargo, descricao, texto_extraido, processado, peso). Como o Hibernate está
-- configurado com ddl-auto=validate, a aplicação não subia sem estas colunas.
-- ============================================

-- ============================================
-- CONCURSO
-- ============================================
ALTER TABLE concurso ADD COLUMN IF NOT EXISTS cargo          VARCHAR(100);
ALTER TABLE concurso ADD COLUMN IF NOT EXISTS descricao      VARCHAR(500);
ALTER TABLE concurso ADD COLUMN IF NOT EXISTS texto_extraido TEXT;
ALTER TABLE concurso ADD COLUMN IF NOT EXISTS processado     BOOLEAN NOT NULL DEFAULT FALSE;

-- ============================================
-- MATERIA
-- ============================================
ALTER TABLE materia ADD COLUMN IF NOT EXISTS descricao TEXT;
ALTER TABLE materia ADD COLUMN IF NOT EXISTS peso      NUMERIC(5,2);

-- ============================================
-- TOPICO
-- ============================================
ALTER TABLE topico ADD COLUMN IF NOT EXISTS descricao TEXT;

-- ============================================
-- SIMULADO
-- ============================================
-- Coluna de controle de concorrência otimista: evita que duas requisições
-- simultâneas finalizem o mesmo simulado ou gravem respostas concorrentes.
ALTER TABLE simulado ADD COLUMN IF NOT EXISTS versao BIGINT NOT NULL DEFAULT 0;

-- A entidade declara valor nulo, então a coluna não deve impor NOT NULL.
ALTER TABLE simulado_questao ALTER COLUMN valor DROP NOT NULL;
ALTER TABLE simulado_questao ALTER COLUMN valor DROP DEFAULT;

-- O curso foi criado em V2 com TIMESTAMPTZ, mas a entidade usa LocalDateTime
-- TIMESTAMP WITHOUT TIME ZONE. Normaliza para o mesmo tipo das demais tabelas.
ALTER TABLE curso ALTER COLUMN criado_em     TYPE TIMESTAMP WITHOUT TIME ZONE;
ALTER TABLE curso ALTER COLUMN atualizado_em TYPE TIMESTAMP WITHOUT TIME ZONE;
