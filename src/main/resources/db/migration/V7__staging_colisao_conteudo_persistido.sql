-- ============================================
-- V7 - Colisão do staging com o conteúdo já persistido
--
-- Uma sugestão pode não ser duplicata de outra sugestão do mesmo edital,
-- mas já existir em materia/topico do concurso (por exemplo, quando o
-- edital é reimportado depois da estrutura ter sido confirmada). A revisão
-- precisa distinguir os dois casos: no primeiro a mesclagem é entre itens
-- do staging; no segundo o item já está gravado e mesclar não se aplica.
-- ============================================

ALTER TABLE materia_sugerida
    ADD COLUMN IF NOT EXISTS ja_existe_confirmada BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE topico_sugerido
    ADD COLUMN IF NOT EXISTS ja_existe_confirmado BOOLEAN NOT NULL DEFAULT FALSE;
