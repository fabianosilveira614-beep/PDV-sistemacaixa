-- =====================================================================
-- V1 - Esquema inicial do Sistema de Caixa da Ferragem
-- Aplicado automaticamente pelo Flyway na inicialização da aplicação.
-- NUNCA edite este arquivo depois de aplicado: crie um V2, V3...
-- Comentários "RNxx" apontam a regra de negócio reforçada no banco.
-- =====================================================================

CREATE TABLE operador (
    id          BIGSERIAL    PRIMARY KEY,
    nome        VARCHAR(120) NOT NULL,
    login       VARCHAR(40)  NOT NULL UNIQUE,
    senha_hash  VARCHAR(255) NOT NULL,
    perfil      VARCHAR(20)  NOT NULL CHECK (perfil IN ('OPERADOR', 'ADMINISTRADOR')),
    ativo       BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE cliente (
    id          BIGSERIAL    PRIMARY KEY,
    nome        VARCHAR(120) NOT NULL CHECK (btrim(nome) <> ''),              -- RN16
    cpf         CHAR(11)     UNIQUE,                                          -- RN03 (opcional)
    telefone    VARCHAR(20),
    ativo       BOOLEAN      NOT NULL DEFAULT TRUE,                           -- RN12
    criado_em   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE produto (
    id                  BIGSERIAL     PRIMARY KEY,
    codigo              VARCHAR(30)   NOT NULL UNIQUE,                        -- RN03
    codigo_barras       VARCHAR(14)   UNIQUE,                                 -- RN03
    nome                VARCHAR(120)  NOT NULL,
    unidade             VARCHAR(10)   NOT NULL CHECK (unidade IN ('UNIDADE', 'KG', 'METRO')),
    preco_custo         NUMERIC(12,2) NOT NULL CHECK (preco_custo >= 0),      -- RN15
    preco_venda         NUMERIC(12,2) NOT NULL CHECK (preco_venda > 0),       -- RN15
    quantidade_estoque  NUMERIC(12,3) NOT NULL DEFAULT 0
                        CHECK (quantidade_estoque >= 0),                      -- RN04
    ativo               BOOLEAN       NOT NULL DEFAULT TRUE                   -- RN12
);

CREATE TABLE sessao_caixa (
    id              BIGSERIAL     PRIMARY KEY,
    operador_id     BIGINT        NOT NULL REFERENCES operador (id),
    aberto_em       TIMESTAMP     NOT NULL,
    valor_abertura  NUMERIC(12,2) NOT NULL CHECK (valor_abertura >= 0),
    fechado_em      TIMESTAMP,
    valor_contado   NUMERIC(12,2) CHECK (valor_contado >= 0),
    status          VARCHAR(10)   NOT NULL CHECK (status IN ('ABERTO', 'FECHADO')),
    CONSTRAINT ck_sessao_fechamento CHECK (
        (status = 'ABERTO'  AND fechado_em IS NULL) OR
        (status = 'FECHADO' AND fechado_em IS NOT NULL AND valor_contado IS NOT NULL)
    )
);

-- RN08: no máximo um caixa aberto (índice único parcial)
CREATE UNIQUE INDEX uk_um_caixa_aberto ON sessao_caixa ((status)) WHERE status = 'ABERTO';

CREATE TABLE venda (
    id               BIGSERIAL     PRIMARY KEY,
    data_hora        TIMESTAMP     NOT NULL,
    cliente_id       BIGINT        REFERENCES cliente (id),
    operador_id      BIGINT        NOT NULL REFERENCES operador (id),
    sessao_caixa_id  BIGINT        NOT NULL REFERENCES sessao_caixa (id),
    forma_pagamento  VARCHAR(10)   NOT NULL
                     CHECK (forma_pagamento IN ('DINHEIRO', 'PIX', 'CREDITO', 'DEBITO', 'FIADO')),
    valor_total      NUMERIC(12,2) NOT NULL CHECK (valor_total > 0),          -- RN05
    valor_recebido   NUMERIC(12,2),
    troco            NUMERIC(12,2) CHECK (troco >= 0),                        -- RN09
    status           VARCHAR(12)   NOT NULL CHECK (status IN ('FINALIZADA')),
    CONSTRAINT ck_venda_fiado_com_cliente
        CHECK (forma_pagamento <> 'FIADO' OR cliente_id IS NOT NULL)          -- RN01
);

CREATE INDEX ix_venda_cliente_data ON venda (cliente_id, data_hora);

CREATE TABLE item_venda (
    id              BIGSERIAL     PRIMARY KEY,
    venda_id        BIGINT        NOT NULL REFERENCES venda (id) ON DELETE CASCADE,
    produto_id      BIGINT        NOT NULL REFERENCES produto (id),
    quantidade      NUMERIC(12,3) NOT NULL CHECK (quantidade > 0),
    preco_unitario  NUMERIC(12,2) NOT NULL CHECK (preco_unitario > 0)         -- RN13
);

CREATE INDEX ix_item_venda_venda ON item_venda (venda_id);

CREATE TABLE debito (
    id              BIGSERIAL     PRIMARY KEY,
    cliente_id      BIGINT        NOT NULL REFERENCES cliente (id),
    venda_id        BIGINT        NOT NULL UNIQUE REFERENCES venda (id),
    valor_original  NUMERIC(12,2) NOT NULL CHECK (valor_original > 0),
    valor_pago      NUMERIC(12,2) NOT NULL DEFAULT 0,
    status          VARCHAR(10)   NOT NULL CHECK (status IN ('ABERTO', 'PARCIAL', 'QUITADO')),
    criado_em       TIMESTAMP     NOT NULL,
    CONSTRAINT ck_debito_pago CHECK (valor_pago >= 0 AND valor_pago <= valor_original)  -- RN10
);

CREATE INDEX ix_debito_cliente ON debito (cliente_id);

CREATE TABLE pagamento_debito (
    id               BIGSERIAL     PRIMARY KEY,
    debito_id        BIGINT        NOT NULL REFERENCES debito (id),
    sessao_caixa_id  BIGINT        NOT NULL REFERENCES sessao_caixa (id),
    valor            NUMERIC(12,2) NOT NULL CHECK (valor > 0),               -- RN10
    forma_pagamento  VARCHAR(10)   NOT NULL
                     CHECK (forma_pagamento IN ('DINHEIRO', 'PIX', 'CREDITO', 'DEBITO')),
    data_hora        TIMESTAMP     NOT NULL
);

CREATE TABLE movimento_caixa (
    id                   BIGSERIAL     PRIMARY KEY,
    sessao_caixa_id      BIGINT        NOT NULL REFERENCES sessao_caixa (id),
    tipo                 VARCHAR(30)   NOT NULL CHECK (tipo IN
                         ('ENTRADA_VENDA', 'ENTRADA_PAGAMENTO_DEBITO', 'SUPRIMENTO', 'SANGRIA')),
    forma_pagamento      VARCHAR(10)   CHECK (forma_pagamento IN ('DINHEIRO', 'PIX', 'CREDITO', 'DEBITO')),
    valor                NUMERIC(12,2) NOT NULL CHECK (valor > 0),
    descricao            VARCHAR(200),
    data_hora            TIMESTAMP     NOT NULL,
    venda_id             BIGINT        REFERENCES venda (id),
    pagamento_debito_id  BIGINT        REFERENCES pagamento_debito (id)
);

CREATE TABLE movimento_estoque (
    id          BIGSERIAL     PRIMARY KEY,
    produto_id  BIGINT        NOT NULL REFERENCES produto (id),
    venda_id    BIGINT        REFERENCES venda (id),
    tipo        VARCHAR(20)   NOT NULL CHECK (tipo IN ('SAIDA_VENDA', 'ENTRADA_COMPRA', 'AJUSTE', 'PERDA')),
    -- positiva = entrou no estoque; negativa = saiu
    quantidade  NUMERIC(12,3) NOT NULL CHECK (quantidade <> 0),
    motivo      VARCHAR(200),
    data_hora   TIMESTAMP     NOT NULL
);

CREATE INDEX ix_movimento_estoque_produto ON movimento_estoque (produto_id, data_hora);
