-- =====================================================================
-- Exemplo de dados com produtos e cliente ja cadastrados
-- Como usar: pgAdmin >> banco "caixa" >> Query Tool >> abrir este arquivo
-- >> F5
-- =====================================================================

BEGIN;

INSERT INTO produto (codigo, codigo_barras, nome, unidade, preco_custo, preco_venda, quantidade_estoque) VALUES
    ('PAR-6MM',  '7890000000017', 'Parafuso sextavado 6mm',        'UNIDADE',  0.10,  0.25, 500),
    ('MAR-27',   '7890000000024', 'Martelo unha 27mm',             'UNIDADE', 22.00, 39.90,  15),
    ('CAD-30',   '7890000000031', 'Cadeado 30mm',                  'UNIDADE', 18.00, 32.50,  20),
    ('CIM-50',   '7890000000079', 'Cimento CP-II 50kg',            'UNIDADE', 28.00, 42.00,   3),
    ('PRE-1727', '7890000000048', 'Prego 17x27 (granel)',          'KG',       6.00,  9.99,  50.000),
    ('ARA-12',   '7890000000055', 'Arame galvanizado nº 12',       'KG',      14.00, 24.90,  30.000),
    ('COR-5MM',  '7890000000062', 'Corrente galvanizada 5mm',      'METRO',    8.00, 15.90, 100.00),
    ('MAN-12',   NULL,            'Mangueira de jardim 1/2 pol.',  'METRO',    2.50,  4.90,  80.00)
ON CONFLICT (codigo) DO NOTHING;

-- o estoque inicial também entra na trilha de movimentos, como no cadastro feito pelo sistema
INSERT INTO movimento_estoque (produto_id, tipo, quantidade, motivo, data_hora)
SELECT p.id, 'ENTRADA_COMPRA', p.quantidade_estoque, 'estoque inicial (dados de exemplo)', CURRENT_TIMESTAMP
  FROM produto p
 WHERE p.codigo IN ('PAR-6MM', 'MAR-27', 'CAD-30', 'CIM-50', 'PRE-1727', 'ARA-12', 'COR-5MM', 'MAN-12')
   AND p.quantidade_estoque > 0
   AND NOT EXISTS (SELECT 1 FROM movimento_estoque m WHERE m.produto_id = p.id);

-- estoque mínimo, abrir programa antes de rodar script para migrar v4

UPDATE produto SET estoque_minimo = 100 WHERE codigo = 'PAR-6MM' AND estoque_minimo IS NULL;
UPDATE produto SET estoque_minimo = 5   WHERE codigo = 'CIM-50'  AND estoque_minimo IS NULL;
UPDATE produto SET estoque_minimo = 10  WHERE codigo = 'PRE-1727' AND estoque_minimo IS NULL;
UPDATE produto SET estoque_minimo = 20  WHERE codigo = 'COR-5MM' AND estoque_minimo IS NULL;


INSERT INTO cliente (nome, cpf, telefone) VALUES ('José da Silva', '52998224725', '(11) 98888-0001')
ON CONFLICT (cpf) DO NOTHING;

INSERT INTO cliente (nome, cpf, telefone)
SELECT 'Maria Oliveira', NULL, NULL
 WHERE NOT EXISTS (SELECT 1 FROM cliente WHERE nome = 'Maria Oliveira');

COMMIT;
