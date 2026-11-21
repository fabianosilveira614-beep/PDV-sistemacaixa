-- =====================================================================
-- V4 - Estoque mínimo por produto (relatório de estoque baixo)
-- Opcional: NULL = produto sem controle de reposição.
-- Por produto, e não um limite fixo: 5 parafusos é nada, 5 sacos de
-- cimento é bastante.
-- =====================================================================

ALTER TABLE produto
    ADD COLUMN estoque_minimo NUMERIC(12,3)
        CHECK (estoque_minimo IS NULL OR estoque_minimo > 0);
