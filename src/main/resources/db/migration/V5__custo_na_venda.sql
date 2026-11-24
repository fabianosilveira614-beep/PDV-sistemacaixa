-- Custo do produto no dia da venda, para o relatório de lucro bruto.
-- O preço de custo do produto muda com o tempo; o lucro de uma venda antiga precisa do custo daquela época.
-- Vendas feitas antes desta versão ficam com NULL ("sem custo registrado"): não há como saber o custo delas,
-- e preencher com o custo de hoje inventaria um lucro que pode não ter existido.
ALTER TABLE item_venda
    ADD COLUMN custo_unitario NUMERIC(12,2) CHECK (custo_unitario IS NULL OR custo_unitario >= 0);
