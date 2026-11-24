package br.com.ferragem.infraestrutura.persistencia.jdbc;

import br.com.ferragem.dominio.excecao.RegraNegocioException;
import br.com.ferragem.dominio.modelo.caixa.Operador;
import br.com.ferragem.dominio.modelo.caixa.PerfilOperador;
import br.com.ferragem.dominio.modelo.cliente.Cliente;
import br.com.ferragem.dominio.modelo.cliente.Cpf;
import br.com.ferragem.dominio.modelo.produto.FabricaProduto;
import br.com.ferragem.dominio.modelo.produto.Produto;
import br.com.ferragem.dominio.modelo.produto.UnidadeMedida;
import br.com.ferragem.infraestrutura.persistencia.PersistenciaException;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Converte linhas do banco em objetos do domínio (e alguns valores na direção inversa).
 *
 * <p>Os objetos são reconstruídos <b>pelos mesmos construtores e métodos</b> que o resto do
 * sistema usa, nunca por reflexão nem por setters criados só para o banco. Consequência: se
 * alguém alterar a tabela à mão e gravar um dado que viola uma regra (preço negativo, nome
 * vazio), a leitura falha com uma mensagem clara em vez de o objeto inválido circular pelo
 * sistema.</p>
 *
 * <p>Cada método espera que as colunas venham com um prefixo (ex.: {@code p_} para produto),
 * para poder ser usado em consultas com JOIN.</p>
 */
final class Mapeadores {

    static final String COLUNAS_PRODUTO = """
            p.id AS p_id, p.codigo AS p_codigo, p.codigo_barras AS p_codigo_barras, p.nome AS p_nome,
            p.unidade AS p_unidade, p.preco_custo AS p_preco_custo, p.preco_venda AS p_preco_venda,
            p.quantidade_estoque AS p_estoque, p.estoque_minimo AS p_estoque_minimo, p.ativo AS p_ativo""";

    static final String COLUNAS_CLIENTE = """
            c.id AS c_id, c.nome AS c_nome, c.cpf AS c_cpf, c.telefone AS c_telefone, c.ativo AS c_ativo""";

    static final String COLUNAS_OPERADOR = """
            o.id AS o_id, o.nome AS o_nome, o.login AS o_login, o.senha_hash AS o_senha_hash,
            o.perfil AS o_perfil, o.trocar_senha AS o_trocar_senha, o.ativo AS o_ativo""";

    private Mapeadores() {
    }

    static Produto produto(ResultSet linha) throws SQLException {
        long id = linha.getLong("p_id");
        try {
            Produto produto = FabricaProduto.criar(
                    UnidadeMedida.valueOf(linha.getString("p_unidade")),
                    linha.getString("p_codigo"),
                    linha.getString("p_codigo_barras"),
                    linha.getString("p_nome"),
                    linha.getBigDecimal("p_preco_custo"),
                    linha.getBigDecimal("p_preco_venda"));
            BigDecimal estoque = linha.getBigDecimal("p_estoque");
            if (estoque.signum() > 0) {
                produto.reporEstoque(estoque);
            }
            produto.definirEstoqueMinimo(linha.getBigDecimal("p_estoque_minimo"));
            if (!linha.getBoolean("p_ativo")) {
                produto.inativar();
            }
            produto.atribuirId(id);
            return produto;
        } catch (RegraNegocioException e) {
            throw dadoInvalido("produto", id, e);
        }
    }

    static Cliente cliente(ResultSet linha) throws SQLException {
        long id = linha.getLong("c_id");
        try {
            String cpf = linha.getString("c_cpf");
            Cliente cliente = new Cliente(linha.getString("c_nome"),
                    cpf == null ? null : Cpf.de(cpf),
                    linha.getString("c_telefone"));
            if (!linha.getBoolean("c_ativo")) {
                cliente.inativar();
            }
            cliente.atribuirId(id);
            return cliente;
        } catch (RegraNegocioException e) {
            throw dadoInvalido("cliente", id, e);
        }
    }

    static Operador operador(ResultSet linha) throws SQLException {
        long id = linha.getLong("o_id");
        try {
            Operador operador = new Operador(linha.getString("o_nome"), linha.getString("o_login"),
                    linha.getString("o_senha_hash"), PerfilOperador.valueOf(linha.getString("o_perfil")));
            if (linha.getBoolean("o_trocar_senha")) {
                operador.exigirTrocaDeSenha();
            }
            if (!linha.getBoolean("o_ativo")) {
                operador.desativar();
            }
            operador.atribuirId(id);
            return operador;
        } catch (RegraNegocioException e) {
            throw dadoInvalido("operador", id, e);
        }
    }

    /** Grava um valor que pode ser nulo (o JDBC exige informar o tipo SQL no caso nulo). */
    static void definirOuNulo(PreparedStatement comando, int posicao, Object valor, int tipoSql)
            throws SQLException {
        if (valor == null) {
            comando.setNull(posicao, tipoSql);
        } else {
            comando.setObject(posicao, valor, tipoSql);
        }
    }

    /** Lê o id gerado por um {@code INSERT ... RETURNING id}. */
    static long idGerado(PreparedStatement comando) throws SQLException {
        try (ResultSet resultado = comando.executeQuery()) {
            resultado.next();
            return resultado.getLong(1);
        }
    }

    static PersistenciaException dadoInvalido(String entidade, long id, Exception causa) {
        return new PersistenciaException("O " + entidade + " #" + id
                + " está gravado no banco com dados que violam as regras do sistema: " + causa.getMessage(), causa);
    }
}
