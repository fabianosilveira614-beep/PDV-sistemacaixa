-- Orçamentos: retrato do carrinho guardado para reimprimir ou carregar no caixa depois.
-- Tabelas próprias: não há ligação com estoque, caixa ou fiado (orçamento não é venda).
CREATE TABLE orcamento (
    id           BIGSERIAL  PRIMARY KEY,
    criado_em    TIMESTAMP  NOT NULL,
    -- cliente excluído do cadastro: o orçamento continua, só sem cliente
    cliente_id   BIGINT     REFERENCES cliente (id) ON DELETE SET NULL,
    operador_id  BIGINT     NOT NULL REFERENCES operador (id)
);

CREATE INDEX ix_orcamento_criado_em ON orcamento (criado_em);

CREATE TABLE item_orcamento (
    id              BIGSERIAL     PRIMARY KEY,
    orcamento_id    BIGINT        NOT NULL REFERENCES orcamento (id) ON DELETE CASCADE,
    -- produto apagado do cadastro (só acontece com cadastro errado, nunca vendido): sai do orçamento
    produto_id      BIGINT        NOT NULL REFERENCES produto (id) ON DELETE CASCADE,
    quantidade      NUMERIC(12,3) NOT NULL CHECK (quantidade > 0),
    preco_unitario  NUMERIC(12,2) NOT NULL CHECK (preco_unitario > 0)
);

CREATE INDEX ix_item_orcamento_orcamento ON item_orcamento (orcamento_id);
