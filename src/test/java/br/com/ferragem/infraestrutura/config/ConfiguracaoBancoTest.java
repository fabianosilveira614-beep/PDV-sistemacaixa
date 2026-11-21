package br.com.ferragem.infraestrutura.config;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConfiguracaoBancoTest {

    private static Properties arquivoCompleto() {
        Properties arquivo = new Properties();
        arquivo.setProperty("db.url", "jdbc:postgresql://localhost:5432/caixa");
        arquivo.setProperty("db.usuario", "caixa");
        arquivo.setProperty("db.senha", "segredo-do-arquivo");
        return arquivo;
    }

    @Test
    void usaValoresDoArquivoQuandoNaoHaVariaveisDeAmbiente() {
        ConfiguracaoBanco configuracao = ConfiguracaoBanco.carregar(arquivoCompleto(), Map.of());

        assertEquals("jdbc:postgresql://localhost:5432/caixa", configuracao.getUrl());
        assertEquals("caixa", configuracao.getUsuario());
        assertEquals("segredo-do-arquivo", configuracao.getSenha());
    }

    @Test
    void variavelDeAmbienteTemPrioridadeSobreOArquivo() {
        Map<String, String> ambiente = Map.of(
                "CAIXA_DB_URL", "jdbc:postgresql://servidor:5433/producao",
                "CAIXA_DB_SENHA", "segredo-do-ambiente");

        ConfiguracaoBanco configuracao = ConfiguracaoBanco.carregar(arquivoCompleto(), ambiente);

        assertEquals("jdbc:postgresql://servidor:5433/producao", configuracao.getUrl());
        assertEquals("caixa", configuracao.getUsuario());
        assertEquals("segredo-do-ambiente", configuracao.getSenha());
    }

    @Test
    void variavelDeAmbienteEmBrancoNaoSobrescreveOArquivo() {
        ConfiguracaoBanco configuracao =
                ConfiguracaoBanco.carregar(arquivoCompleto(), Map.of("CAIXA_DB_USUARIO", "  "));

        assertEquals("caixa", configuracao.getUsuario());
    }

    @Test
    void falhaComMensagemClaraQuandoFaltaAUrl() {
        Properties semUrl = arquivoCompleto();
        semUrl.remove("db.url");

        IllegalStateException erro = assertThrows(IllegalStateException.class,
                () -> ConfiguracaoBanco.carregar(semUrl, Map.of()));

        assertEquals("Configuração ausente: URL do banco (db.url / CAIXA_DB_URL).", erro.getMessage());
    }

    @Test
    void toStringNaoExpoeASenha() {
        ConfiguracaoBanco configuracao = ConfiguracaoBanco.carregar(arquivoCompleto(), Map.of());

        assertFalse(configuracao.toString().contains("segredo-do-arquivo"));
    }
}
