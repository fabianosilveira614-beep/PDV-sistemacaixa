package br.com.ferragem.infraestrutura.persistencia;

import br.com.ferragem.infraestrutura.config.ConfiguracaoBanco;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Objects;

/**
 * Abre conexões JDBC com o PostgreSQL a partir de uma {@link ConfiguracaoBanco}.
 *
 * <p>É o único ponto do sistema que sabe <i>como</i> conectar no banco. Os repositórios JDBC
 * recebem esta fábrica e pedem conexões a ela.</p>
 */
public class FabricaConexao {

    private final ConfiguracaoBanco configuracao;

    /**
     * @param configuracao dados de conexão; não pode ser nulo
     */
    public FabricaConexao(ConfiguracaoBanco configuracao) {
        this.configuracao = Objects.requireNonNull(configuracao, "configuracao");
    }

    /**
     * Abre uma nova conexão. Quem chama é responsável por fechá-la (use try-with-resources).
     *
     * @return conexão aberta
     * @throws PersistenciaException se não for possível conectar
     */
    public Connection abrirConexao() {
        try {
            return DriverManager.getConnection(
                    configuracao.getUrl(), configuracao.getUsuario(), configuracao.getSenha());
        } catch (SQLException e) {
            throw new PersistenciaException(
                    "Não foi possível conectar ao banco em " + configuracao.getUrl(), e);
        }
    }

    /** @return configuração usada por esta fábrica */
    public ConfiguracaoBanco getConfiguracao() {
        return configuracao;
    }
}
