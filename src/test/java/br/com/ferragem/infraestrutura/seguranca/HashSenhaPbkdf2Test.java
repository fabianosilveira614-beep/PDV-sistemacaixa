package br.com.ferragem.infraestrutura.seguranca;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Testes do hash de senha real (PBKDF2). Usam poucas iterações para rodar rápido. */
class HashSenhaPbkdf2Test {

    private final HashSenhaPbkdf2 hash = new HashSenhaPbkdf2(1_000);

    @Test
    @DisplayName("O hash não contém a senha e confere só com a senha certa")
    void confereSoComASenhaCerta() {
        String guardado = hash.gerar("minhasenha");

        assertFalse(guardado.contains("minhasenha"));
        assertTrue(hash.confere("minhasenha", guardado));
        assertFalse(hash.confere("MinhaSenha", guardado));
        assertFalse(hash.confere("", guardado));
    }

    @Test
    @DisplayName("A mesma senha gera hashes diferentes (sal aleatório), e os dois conferem")
    void salAleatorio() {
        String primeiro = hash.gerar("minhasenha");
        String segundo = hash.gerar("minhasenha");

        assertNotEquals(primeiro, segundo);
        assertTrue(hash.confere("minhasenha", primeiro));
        assertTrue(hash.confere("minhasenha", segundo));
    }

    @Test
    @DisplayName("Formato pbkdf2-sha256$iteracoes$sal$hash; o padrão usa 600.000 iterações")
    void formato() {
        String[] partes = hash.gerar("x").split("\\$");

        assertEquals(4, partes.length);
        assertEquals("pbkdf2-sha256", partes[0]);
        assertEquals("1000", partes[1]);
        assertEquals(600_000, HashSenhaPbkdf2.ITERACOES_PADRAO);
    }

    @Test
    @DisplayName("Hash guardado corrompido ou nulo não confere (e não lança exceção)")
    void hashCorrompido() {
        assertFalse(hash.confere("x", "lixo"));
        assertFalse(hash.confere("x", "pbkdf2-sha256$abc$@@$@@"));
        assertFalse(hash.confere("x", null));
        assertThrows(IllegalArgumentException.class, () -> new HashSenhaPbkdf2(0));
    }
}
