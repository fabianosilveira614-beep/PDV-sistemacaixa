package br.com.ferragem.infraestrutura.persistencia.jdbc;

import br.com.ferragem.aplicacao.porta.DebitoRepository;
import br.com.ferragem.dominio.excecao.RegraNegocioException;
import br.com.ferragem.dominio.modelo.cliente.Cliente;
import br.com.ferragem.dominio.modelo.fiado.Debito;
import br.com.ferragem.dominio.modelo.fiado.PagamentoDebito;
import br.com.ferragem.dominio.modelo.venda.FormaPagamento;
import br.com.ferragem.infraestrutura.persistencia.PersistenciaException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Dívidas de fiado no PostgreSQL (tabelas {@code debito} e {@code pagamento_debito}).
 *
 * <p>Mesma estratégia do caixa: o débito é recriado e cada pagamento gravado é "repetido" com
 * {@link Debito#registrarPagamento}, de modo que saldo e status sempre saem da regra do domínio.
 * As colunas {@code valor_pago} e {@code status} da tabela são uma cópia, gravada só para
 * facilitar relatórios em SQL; quem manda é a lista de pagamentos.</p>
 *
 * <p>Cada pagamento é ligado à sessão de caixa aberta no momento da gravação, que é onde o
 * dinheiro entrou.</p>
 */
public class DebitoRepositoryJdbc implements DebitoRepository {

    private final ContextoJdbc contexto;

    /**
     * @param contexto acesso ao banco; não pode ser nulo
     */
    public DebitoRepositoryJdbc(ContextoJdbc contexto) {
        this.contexto = Objects.requireNonNull(contexto, "contexto");
    }

    @Override
    public List<Debito> buscarPorCliente(Cliente cliente) {
        long clienteId = cliente.getId().orElseThrow(() -> new IllegalArgumentException("Cliente sem id."));
        String sql = """
                SELECT id, venda_id, valor_original, criado_em FROM debito
                 WHERE cliente_id = ? ORDER BY criado_em, id""";
        return contexto.usar(conexao -> {
            List<Debito> debitos = new ArrayList<>();
            try (PreparedStatement comando = conexao.prepareStatement(sql)) {
                comando.setLong(1, clienteId);
                try (ResultSet linha = comando.executeQuery()) {
                    while (linha.next()) {
                        long id = linha.getLong("id");
                        Debito debito = new Debito(cliente, linha.getLong("venda_id"),
                                linha.getBigDecimal("valor_original"),
                                linha.getObject("criado_em", LocalDateTime.class));
                        debito.atribuirId(id);
                        debitos.add(debito);
                    }
                }
            }
            for (Debito debito : debitos) {
                repetirPagamentos(conexao, debito);
            }
            return debitos;
        });
    }

    @Override
    public void salvar(Debito debito) {
        contexto.usar(conexao -> {
            if (debito.getId().isEmpty()) {
                inserir(conexao, debito);
            } else {
                atualizar(conexao, debito);
            }
            inserirPagamentosNovos(conexao, debito);
            return null;
        });
    }

    private static void repetirPagamentos(Connection conexao, Debito debito) throws SQLException {
        long id = debito.getId().orElseThrow();
        try (PreparedStatement comando = conexao.prepareStatement(
                "SELECT valor, forma_pagamento, data_hora FROM pagamento_debito WHERE debito_id = ? ORDER BY id")) {
            comando.setLong(1, id);
            try (ResultSet linha = comando.executeQuery()) {
                while (linha.next()) {
                    debito.registrarPagamento(linha.getBigDecimal("valor"),
                            FormaPagamento.valueOf(linha.getString("forma_pagamento")),
                            linha.getObject("data_hora", LocalDateTime.class));
                }
            }
        } catch (RegraNegocioException e) {
            throw Mapeadores.dadoInvalido("débito", id, e);
        }
    }

    private static void inserir(Connection conexao, Debito debito) throws SQLException {
        String sql = """
                INSERT INTO debito (cliente_id, venda_id, valor_original, valor_pago, status, criado_em)
                VALUES (?, ?, ?, ?, ?, ?) RETURNING id""";
        try (PreparedStatement comando = conexao.prepareStatement(sql)) {
            comando.setLong(1, debito.getCliente().getId().orElseThrow(() ->
                    new IllegalStateException("Cliente sem id não pode ter dívida.")));
            comando.setLong(2, debito.getVendaId());
            comando.setBigDecimal(3, debito.getValorOriginal());
            comando.setBigDecimal(4, debito.getValorPago());
            comando.setString(5, debito.getStatus().name());
            comando.setObject(6, debito.getCriadoEm());
            debito.atribuirId(Mapeadores.idGerado(comando));
        }
    }

    private static void atualizar(Connection conexao, Debito debito) throws SQLException {
        try (PreparedStatement comando = conexao.prepareStatement(
                "UPDATE debito SET valor_pago = ?, status = ? WHERE id = ?")) {
            comando.setBigDecimal(1, debito.getValorPago());
            comando.setString(2, debito.getStatus().name());
            comando.setLong(3, debito.getId().orElseThrow());
            comando.executeUpdate();
        }
    }

    private static void inserirPagamentosNovos(Connection conexao, Debito debito) throws SQLException {
        long debitoId = debito.getId().orElseThrow();
        List<PagamentoDebito> todos = debito.getPagamentos();
        int jaGravados = contarPagamentos(conexao, debitoId);
        if (jaGravados == todos.size()) {
            return;
        }
        long sessaoId = sessaoAberta(conexao);
        String sql = """
                INSERT INTO pagamento_debito (debito_id, sessao_caixa_id, valor, forma_pagamento, data_hora)
                VALUES (?, ?, ?, ?, ?)""";
        try (PreparedStatement comando = conexao.prepareStatement(sql)) {
            for (PagamentoDebito pagamento : todos.subList(jaGravados, todos.size())) {
                comando.setLong(1, debitoId);
                comando.setLong(2, sessaoId);
                comando.setBigDecimal(3, pagamento.valor());
                comando.setString(4, pagamento.forma().name());
                comando.setObject(5, pagamento.dataHora());
                comando.addBatch();
            }
            comando.executeBatch();
        }
    }

    private static int contarPagamentos(Connection conexao, long debitoId) throws SQLException {
        try (PreparedStatement comando = conexao.prepareStatement(
                "SELECT count(*) FROM pagamento_debito WHERE debito_id = ?")) {
            comando.setLong(1, debitoId);
            try (ResultSet resultado = comando.executeQuery()) {
                resultado.next();
                return resultado.getInt(1);
            }
        }
    }

    private static long sessaoAberta(Connection conexao) throws SQLException {
        try (PreparedStatement comando = conexao.prepareStatement(
                "SELECT id FROM sessao_caixa WHERE status = 'ABERTO'");
             ResultSet resultado = comando.executeQuery()) {
            if (!resultado.next()) {
                throw new PersistenciaException("Pagamento de fiado sem caixa aberto para registrá-lo.", null);
            }
            return resultado.getLong(1);
        }
    }
}
