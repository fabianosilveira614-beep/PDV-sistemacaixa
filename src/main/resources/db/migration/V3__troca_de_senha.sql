-- =====================================================================
-- V3 - Obrigação de trocar a senha no próximo login
-- Ligada quando a senha foi definida por outra pessoa: o administrador,
-- ao cadastrar um operador, ou a senha padrão publicada do "admin" (V2).
-- =====================================================================

ALTER TABLE operador ADD COLUMN trocar_senha BOOLEAN NOT NULL DEFAULT FALSE;

-- Só marca o admin se ele ainda estiver com a senha padrão da V2
UPDATE operador
   SET trocar_senha = TRUE
 WHERE login = 'admin'
   AND senha_hash = 'pbkdf2-sha256$600000$uSVrllcX2On8Bp18YDE3JA==$5SEM+oBrysfDIaa5cAD2VT5TPKW0n2Y8QxYpMC6hAic=';
