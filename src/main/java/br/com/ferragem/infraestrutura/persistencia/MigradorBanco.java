package br.com.ferragem.infraestrutura.persistencia;

import br.com.ferragem.infraestrutura.config.ConfiguracaoBanco;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;

import java.util.Objects;

/**
 * Cria e atualiza as tabelas do banco aplicando os scripts de {@code src/main/resources/db/migration}.
 *
 * <p>Usa o Flyway: cada script {@code V<n>__descricao.sql} é aplicado uma única vez, em ordem, e o
 * Flyway registra no próprio banco quais já rodaram. Isso torna o banco reproduzível: qualquer
 * pessoa que rode a aplicação com um banco vazio obtém exatamente o mesmo esquema.</p>
 */
public class MigradorBanco {

    private static final String LOCAL_SCRIPTS = "classpath:db/migration";

    private final ConfiguracaoBanco configuracao;

    /**
     * @param configuracao dados de conexão; não pode ser nulo
     */
    public MigradorBanco(ConfiguracaoBanco configuracao) {
        this.configuracao = Objects.requireNonNull(configuracao, "configuracao");
    }

    /**
     * Aplica as migrações pendentes.
     *
     * @return quantidade de scripts aplicados nesta execução (0 se o banco já estava atualizado)
     * @throws PersistenciaException se o banco estiver inacessível ou algum script falhar
     */
    public int migrar() {
        try {
            Flyway flyway = Flyway.configure()
                    .dataSource(configuracao.getUrl(), configuracao.getUsuario(), configuracao.getSenha())
                    .locations(LOCAL_SCRIPTS)
                    .load();
            return flyway.migrate().migrationsExecuted;
        } catch (FlywayException e) {
            throw new PersistenciaException(
                    "Falha ao preparar o banco de dados em " + configuracao.getUrl(), e);
        }
    }
}
