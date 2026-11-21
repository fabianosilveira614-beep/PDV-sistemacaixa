package br.com.ferragem.infraestrutura.persistencia.jdbc;

import br.com.ferragem.aplicacao.porta.ClienteRepository;
import br.com.ferragem.dominio.modelo.cliente.Cliente;
import br.com.ferragem.dominio.modelo.cliente.Cpf;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Clientes no PostgreSQL (tabela {@code cliente}). O CPF é gravado só com os 11 dígitos.
 */
public class ClienteRepositoryJdbc implements ClienteRepository {

    private static final String SELECT = "SELECT " + Mapeadores.COLUNAS_CLIENTE + " FROM cliente c ";

    private final ContextoJdbc contexto;

    /**
     * @param contexto acesso ao banco; não pode ser nulo
     */
    public ClienteRepositoryJdbc(ContextoJdbc contexto) {
        this.contexto = Objects.requireNonNull(contexto, "contexto");
    }

    @Override
    public void salvar(Cliente cliente) {
        contexto.usar(conexao -> {
            if (cliente.getId().isEmpty()) {
                inserir(conexao, cliente);
            } else {
                atualizar(conexao, cliente);
            }
            return null;
        });
    }

    @Override
    public Optional<Cliente> buscarPorId(long id) {
        return buscar("WHERE c.id = ?", id).stream().findFirst();
    }

    @Override
    public Optional<Cliente> buscarPorCpf(Cpf cpf) {
        return buscar("WHERE c.cpf = ?", cpf.getDigitos()).stream().findFirst();
    }

    @Override
    public List<Cliente> buscarPorNome(String trecho) {
        return buscar("WHERE c.ativo AND c.nome ILIKE ? ORDER BY c.nome",
                "%" + ProdutoRepositoryJdbc.escaparLike(trecho) + "%");
    }

    @Override
    public void excluir(Cliente cliente) {
        long id = cliente.getId().orElseThrow(() -> new IllegalArgumentException("Cliente sem id."));
        contexto.usar(conexao -> {
            try (PreparedStatement comando = conexao.prepareStatement("DELETE FROM cliente WHERE id = ?")) {
                comando.setLong(1, id);
                return comando.executeUpdate();
            }
        });
    }

    private List<Cliente> buscar(String filtro, Object valor) {
        return contexto.usar(conexao -> {
            try (PreparedStatement comando = conexao.prepareStatement(SELECT + filtro)) {
                comando.setObject(1, valor);
                List<Cliente> clientes = new ArrayList<>();
                try (ResultSet linhas = comando.executeQuery()) {
                    while (linhas.next()) {
                        clientes.add(Mapeadores.cliente(linhas));
                    }
                }
                return clientes;
            }
        });
    }

    private static void inserir(Connection conexao, Cliente cliente) throws SQLException {
        try (PreparedStatement comando = conexao.prepareStatement(
                "INSERT INTO cliente (nome, cpf, telefone, ativo) VALUES (?, ?, ?, ?) RETURNING id")) {
            preencher(comando, cliente);
            cliente.atribuirId(Mapeadores.idGerado(comando));
        }
    }

    private static void atualizar(Connection conexao, Cliente cliente) throws SQLException {
        try (PreparedStatement comando = conexao.prepareStatement(
                "UPDATE cliente SET nome = ?, cpf = ?, telefone = ?, ativo = ? WHERE id = ?")) {
            preencher(comando, cliente);
            comando.setLong(5, cliente.getId().orElseThrow());
            comando.executeUpdate();
        }
    }

    private static void preencher(PreparedStatement comando, Cliente cliente) throws SQLException {
        comando.setString(1, cliente.getNome());
        Mapeadores.definirOuNulo(comando, 2, cliente.getCpf().map(Cpf::getDigitos).orElse(null), Types.CHAR);
        Mapeadores.definirOuNulo(comando, 3, cliente.getTelefone().orElse(null), Types.VARCHAR);
        comando.setBoolean(4, cliente.estaAtivo());
    }
}
