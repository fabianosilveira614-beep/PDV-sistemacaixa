-- =====================================================================
-- V2 - Administrador inicial
-- Sem ele ninguém consegue entrar: só um administrador cadastra operadores.
--
--   login: admin    senha: admin123
--
-- A senha NÃO está aqui: só o hash PBKDF2-SHA256 (600.000 iterações, sal
-- aleatório), gerado pela própria classe HashSenhaPbkdf2.
-- ATENÇÃO: senha conhecida publicamente. Em uso real, troque-a no
-- primeiro acesso (tela de troca de senha, fase 4).
-- =====================================================================

INSERT INTO operador (nome, login, senha_hash, perfil)
VALUES ('Administrador', 'admin',
        'pbkdf2-sha256$600000$uSVrllcX2On8Bp18YDE3JA==$5SEM+oBrysfDIaa5cAD2VT5TPKW0n2Y8QxYpMC6hAic=',
        'ADMINISTRADOR')
ON CONFLICT (login) DO NOTHING;
