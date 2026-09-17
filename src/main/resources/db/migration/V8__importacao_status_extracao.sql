-- ============================================
-- V8 - Qualidade da extração da importação
--
-- `status` (status_processamento) é o ciclo de vida da importação, mas não
-- distingue "extraiu tudo" de "não achou nada". Sem isso um edital sem bloco
-- de conteúdo programático terminava em AGUARDANDO_REVISAO com zero matérias,
-- falhando silenciosamente.
--
-- Guarda o StatusExtracao calculado pelo extrator: PROCESSADO, PARCIAL,
-- BAIXA_CONFIANCA, NAO_IDENTIFICADO ou ERRO.
-- ============================================

ALTER TABLE edital_importacao
    ADD COLUMN IF NOT EXISTS status_extracao VARCHAR(30);