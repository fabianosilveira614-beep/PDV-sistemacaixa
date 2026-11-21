package br.com.ferragem.infraestrutura.persistencia.jdbc;

import br.com.ferragem.infraestrutura.config.ConfiguracaoBanco;
import br.com.ferragem.infraestrutura.persistencia.FabricaConexao;
import br.com.ferragem.infraestrutura.persistencia.MigradorBanco;
import org.junit.jupiter.api.Assumptions;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Prepara o banco <b>de teste</b> ({@code caixa_teste}, nunca o banco de produção) para os
 * testes de integração.
 *
 * <p>Configuração por variáveis de ambiente, com padrão para a instalação local do grupo:
 * {@code CAIXA_TESTE_DB_URL} (padrão {@code jdbc:postgresql://localhost:5432/caixa_teste}),
 * {@code CAIXA_TESTE_DB_USUARIO} e {@code CAIXA_TESTE_DB_SENHA} (padrão {@code caixa}).</p>
 *
 * <p>Se o banco não estiver acessível, os testes de integração são <b>pulados</b> (aparecem
 * como "skipped" no relatório), não reprovados: quem só quer rodar os testes de regra não
 * precisa de PostgreSQL.</p>
 */
final class BancoDeTeste {

    private static final String TABELAS = "movimento_estoque, movimento_caixa, pagamento_debito, debito, "
            + "item_venda, venda, sessao_caixa, produto, cliente, operador";

    private static ConfiguracaoBanco configuracao;
    private static boolean migrado;

    private BancoDeTeste() {
    }

    /**
     * Garante as tabelas criadas (uma vez por execução) e apaga todos os dados, deixando cada
     * teste independente da ordem em que roda.
     *
     * @return fábrica de conexões do banco de teste
     */
    static synchronized FabricaConexao limparEPreparar() {
        if (configuracao == null) {
            configuracao = new ConfiguracaoBanco(
                    variavel("CAIXA_TESTE_DB_URL", "jdbc:postgresql://localhost:5432/caixa_teste"),
                    variavel("CAIXA_TESTE_DB_USUARIO", "caixa"),
                    variavel("CAIXA_TESTE_DB_SENHA", "caixa"));
        }
        FabricaConexao fabrica = new FabricaConexao(configuracao);
        try (Connection conexao = fabrica.abrirConexao()) {
            if (!migrado) {
                new MigradorBanco(configuracao).migrar();
                migrado = true;
            }
            try (Statement comando = conexao.createStatement()) {
                comando.execute("TRUNCATE " + TABELAS + " RESTART IDENTITY CASCADE");
            }
        } catch (RuntimeException | SQLException e) {
            Assumptions.assumeTrue(false, "Banco de teste indisponível em " + configuracao.getUrl()
                    + " (" + e.getMessage() + "). Crie o banco caixa_teste para rodar os testes de integração.");
        }
        return fabrica;
    }

    private static String variavel(String nome, String padrao) {
        String valor = System.getenv(nome);
        return valor == null || valor.isBlank() ? padrao : valor;
    }
}
