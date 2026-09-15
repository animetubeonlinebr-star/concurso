-- V3__drop_materia_concurso_id.sql
--
-- Remove a coluna legada `concurso_id` da tabela `materia`.
--
-- Contexto: a migration V2 introduziu o nível `curso` entre `concurso` e
-- `materia`. Os dados foram migrados e a coluna `concurso_id` foi mantida
-- temporariamente por segurança. Agora que o schema está validado, ela
-- é removida — a entidade MateriaEntity só conhece `curso`.
--
-- Ordem: precisa dropar FK primeiro, depois a coluna.

-- 1. Drop da foreign key (se ainda existir)
ALTER TABLE materia
DROP CONSTRAINT IF EXISTS fk_materia_concurso;

-- 2. Drop da coluna
ALTER TABLE materia
DROP COLUMN IF EXISTS concurso_id;

-- 3. Drop do índice (se ainda existir)
DROP INDEX IF EXISTS idx_materia_concurso_id;