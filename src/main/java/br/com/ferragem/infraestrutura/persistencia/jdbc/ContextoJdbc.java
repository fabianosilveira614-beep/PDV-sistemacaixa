package br.com.ferragem.infraestrutura.persistencia.jdbc;

import br.com.ferragem.aplicacao.porta.GerenciadorTransacao;
import br.com.ferragem.aplicacao.porta.OperacaoTransacional;
import br.com.ferragem.dominio.excecao.RegraNegocioException;
import br.com.ferragem.infraestrutura.persistencia.FabricaConexao;
import br.com.ferragem.infraestrutura.persistencia.PersistenciaException;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

/**
 * Implementação JDBC da porta {@link GerenciadorTransacao} e ponto único de acesso à conexão
 * para todos os repositórios.
 *
 * <p><b>Como funciona:</b> {@link #executar} abre uma conexão, desliga o auto-commit e a guarda
 * num {@link ThreadLocal}. Enquanto a operação roda, todo repositório que chamar {@link #usar}
 * recebe <i>essa mesma</i> conexão, então tudo entra na mesma transação sem que o caso de uso
 * precise passar a conexão adiante. No fim, se a operação terminou normalmente, faz
 * {@code commit}; se lançou qualquer exceção (regra de negócio ou erro técnico), faz
 * {@code rollback} e relança a exceção original.</p>
 *
 * <p>Fora de uma transação, {@link #usar} abre uma conexão com auto-commit só para aquela
 * chamada e a fecha em seguida (consultas simples das telas).</p>
 *
 * <p>Transações aninhadas não abrem uma nova: a interna simplesmente participa da externa.</p>
 */
public class ContextoJdbc implements GerenciadorTransacao {

    private final FabricaConexao fabrica;
    private final ThreadLocal<Connection> conexaoDaTransacao = new ThreadLocal<>();

    /**
     * @param fabrica fábrica de conexões; não pode ser nula
     */
    public ContextoJdbc(FabricaConexao fabrica) {
        this.fabrica = Objects.requireNonNull(fabrica, "fabrica");
    }

    @Override
    public <T> T executar(OperacaoTransacional<T> operacao) throws RegraNegocioException {
        Objects.requireNonNull(operacao, "operacao");
        if (emTransacao()) {
            return operacao.executar();
        }
        try (Connection conexao = fabrica.abrirConexao()) {
            conexao.setAutoCommit(false);
            conexaoDaTransacao.set(conexao);
            T resultado;
            try {
                resultado = operacao.executar();
            } catch (Throwable erro) {
                desfazer(conexao, erro);
                throw erro;
            } finally {
                conexaoDaTransacao.remove();
            }
            conexao.commit();
            return resultado;
        } catch (SQLException e) {
            throw new PersistenciaException("Falha ao confirmar a transação no banco.", e);
        }
    }

    /**
     * Executa uma ação com a conexão da transação corrente ou, se não houver, com uma conexão
     * avulsa (auto-commit) aberta e fechada só para ela.
     *
     * @param acao código que usa a conexão
     * @param <T>  tipo do resultado
     * @return o que a ação devolver
     * @throws PersistenciaException se o banco falhar
     */
    public <T> T usar(AcaoSql<T> acao) {
        Connection atual = conexaoDaTransacao.get();
        try {
            if (atual != null) {
                return acao.executar(atual);
            }
            try (Connection avulsa = fabrica.abrirConexao()) {
                return acao.executar(avulsa);
            }
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao acessar o banco: " + e.getMessage(), e);
        }
    }

    /** @return {@code true} se esta thread estiver dentro de {@link #executar} */
    public boolean emTransacao() {
        return conexaoDaTransacao.get() != null;
    }

    private static void desfazer(Connection conexao, Throwable causa) {
        try {
            conexao.rollback();
        } catch (SQLException falhaNoRollback) {
            causa.addSuppressed(falhaNoRollback);
        }
    }
}
