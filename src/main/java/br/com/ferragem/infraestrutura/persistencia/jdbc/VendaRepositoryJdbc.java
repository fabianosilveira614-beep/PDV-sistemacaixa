package br.com.ferragem.infraestrutura.persistencia.jdbc;

import br.com.ferragem.aplicacao.pagamento.ResultadoPagamento;
import br.com.ferragem.aplicacao.porta.HistoricoVendas;
import br.com.ferragem.aplicacao.porta.RegistroVendas;
import br.com.ferragem.dominio.modelo.caixa.Operador;
import br.com.ferragem.dominio.modelo.caixa.SessaoCaixa;
import br.com.ferragem.dominio.modelo.cliente.Cliente;
import br.com.ferragem.dominio.modelo.produto.Produto;
import br.com.ferragem.dominio.modelo.venda.FormaPagamento;
import br.com.ferragem.dominio.modelo.venda.ItemVenda;
import br.com.ferragem.dominio.modelo.venda.Venda;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Vendas no PostgreSQL (tabelas {@code venda} e {@code item_venda}).
 *
 * <p>Implementa duas portas separadas (princípio da segregação de interfaces): quem registra
 * vendas ({@link RegistroVendas}) e quem consulta o histórico ({@link HistoricoVendas}). As
 * duas falam com as mesmas tabelas, por isso moram na mesma classe; mas cada caso de uso só
 * enxerga a interface de que precisa.</p>
 *
 * <p>O histórico é lido com uma única consulta (venda + itens + produtos), em vez de uma
 * consulta por venda, e montado com {@link Venda#restaurar}.</p>
 */
public class VendaRepositoryJdbc implements RegistroVendas, HistoricoVendas {

    private final ContextoJdbc contexto;

    /**
     * @param contexto acesso ao banco; não pode ser nulo
     */
    public VendaRepositoryJdbc(ContextoJdbc contexto) {
        this.contexto = Objects.requireNonNull(contexto, "contexto");
    }

    @Override
    public void salvar(Venda venda, SessaoCaixa sessao, Operador operador, ResultadoPagamento pagamento) {
        contexto.usar(conexao -> {
            venda.atribuirId(inserirVenda(conexao, venda, sessao, operador, pagamento));
            inserirItens(conexao, venda);
            return null;
        });
    }

    @Override
    public List<Venda> buscarPorCliente(Cliente cliente, LocalDate inicio, LocalDate fim) {
        long clienteId = idDe(cliente);
        String sql = "SELECT v.id AS v_id, v.data_hora, v.forma_pagamento, "
                + "i.quantidade, i.preco_unitario, i.custo_unitario, "
                + Mapeadores.COLUNAS_PRODUTO + """
                 FROM venda v
                 JOIN item_venda i ON i.venda_id = v.id
                 JOIN produto p ON p.id = i.produto_id
                WHERE v.cliente_id = ? AND v.data_hora >= ? AND v.data_hora < ?
                ORDER BY v.data_hora DESC, v.id DESC, i.id""";
        return contexto.usar(conexao -> {
            Map<Long, Linha> vendas = new LinkedHashMap<>();
            try (PreparedStatement comando = conexao.prepareStatement(sql)) {
                comando.setLong(1, clienteId);
                comando.setObject(2, inicio.atStartOfDay());
                comando.setObject(3, fim.plusDays(1).atStartOfDay());
                try (ResultSet linha = comando.executeQuery()) {
                    while (linha.next()) {
                        Linha venda = vendas.computeIfAbsent(linha.getLong("v_id"), id -> new Linha());
                        venda.dataHora = linha.getObject("data_hora", LocalDateTime.class);
                        venda.forma = FormaPagamento.valueOf(linha.getString("forma_pagamento"));
                        venda.itens.add(new Venda.ItemRegistrado(Mapeadores.produto(linha),
                                linha.getBigDecimal("quantidade"), linha.getBigDecimal("preco_unitario"),
                                linha.getBigDecimal("custo_unitario")));
                    }
                }
            }
            List<Venda> resultado = new ArrayList<>();
            vendas.forEach((id, dados) ->
                    resultado.add(Venda.restaurar(id, cliente, dados.forma, dados.dataHora, dados.itens)));
            return resultado;
        });
    }

    @Override
    public boolean clienteTemVendas(Cliente cliente) {
        long clienteId = idDe(cliente);
        return contexto.usar(conexao -> {
            try (PreparedStatement comando = conexao.prepareStatement(
                    "SELECT EXISTS (SELECT 1 FROM venda WHERE cliente_id = ?)")) {
                comando.setLong(1, clienteId);
                try (ResultSet resultado = comando.executeQuery()) {
                    resultado.next();
                    return resultado.getBoolean(1);
                }
            }
        });
    }

    @Override
    public Optional<Venda> buscarPorId(long vendaId) {
        String sql = "SELECT v.id AS v_id, v.data_hora, v.forma_pagamento, "
                + "i.quantidade, i.preco_unitario, i.custo_unitario, "
                + Mapeadores.COLUNAS_PRODUTO + ", " + Mapeadores.COLUNAS_CLIENTE + """
                 FROM venda v
                 JOIN item_venda i ON i.venda_id = v.id
                 JOIN produto p ON p.id = i.produto_id
                 LEFT JOIN cliente c ON c.id = v.cliente_id
                WHERE v.id = ?
                ORDER BY i.id""";
        return contexto.usar(conexao -> {
            Linha dados = null;
            Cliente cliente = null;
            try (PreparedStatement comando = conexao.prepareStatement(sql)) {
                comando.setLong(1, vendaId);
                try (ResultSet linha = comando.executeQuery()) {
                    while (linha.next()) {
                        if (dados == null) {
                            dados = new Linha();
                            dados.dataHora = linha.getObject("data_hora", LocalDateTime.class);
                            dados.forma = FormaPagamento.valueOf(linha.getString("forma_pagamento"));
                            cliente = linha.getObject("c_id") == null ? null : Mapeadores.cliente(linha);
                        }
                        dados.itens.add(new Venda.ItemRegistrado(Mapeadores.produto(linha),
                                linha.getBigDecimal("quantidade"), linha.getBigDecimal("preco_unitario"),
                                linha.getBigDecimal("custo_unitario")));
                    }
                }
            }
            return dados == null ? Optional.<Venda>empty()
                    : Optional.of(Venda.restaurar(vendaId, cliente, dados.forma, dados.dataHora, dados.itens));
        });
    }

    @Override
    public List<VendaDoPeriodo> buscarPorPeriodo(LocalDate inicio, LocalDate fim) {
        String sql = "SELECT v.id AS v_id, v.data_hora, v.forma_pagamento, o.nome AS operador_nome, "
                + "i.quantidade, i.preco_unitario, i.custo_unitario, "
                + Mapeadores.COLUNAS_PRODUTO + ", " + Mapeadores.COLUNAS_CLIENTE + """
                 FROM venda v
                 JOIN operador o ON o.id = v.operador_id
                 JOIN item_venda i ON i.venda_id = v.id
                 JOIN produto p ON p.id = i.produto_id
                 LEFT JOIN cliente c ON c.id = v.cliente_id
                WHERE v.data_hora >= ? AND v.data_hora < ?
                ORDER BY v.data_hora DESC, v.id DESC, i.id""";
        return contexto.usar(conexao -> {
            Map<Long, Linha> vendas = new LinkedHashMap<>();
            try (PreparedStatement comando = conexao.prepareStatement(sql)) {
                comando.setObject(1, inicio.atStartOfDay());
                comando.setObject(2, fim.plusDays(1).atStartOfDay());
                try (ResultSet linha = comando.executeQuery()) {
                    while (linha.next()) {
                        Linha venda = vendas.get(linha.getLong("v_id"));
                        if (venda == null) {
                            venda = new Linha();
                            venda.dataHora = linha.getObject("data_hora", LocalDateTime.class);
                            venda.forma = FormaPagamento.valueOf(linha.getString("forma_pagamento"));
                            venda.operador = linha.getString("operador_nome");
                            venda.cliente = linha.getObject("c_id") == null ? null : Mapeadores.cliente(linha);
                            vendas.put(linha.getLong("v_id"), venda);
                        }
                        venda.itens.add(new Venda.ItemRegistrado(Mapeadores.produto(linha),
                                linha.getBigDecimal("quantidade"), linha.getBigDecimal("preco_unitario"),
                                linha.getBigDecimal("custo_unitario")));
                    }
                }
            }
            List<VendaDoPeriodo> resultado = new ArrayList<>();
            vendas.forEach((id, dados) -> resultado.add(new VendaDoPeriodo(
                    Venda.restaurar(id, dados.cliente, dados.forma, dados.dataHora, dados.itens), dados.operador)));
            return resultado;
        });
    }

    @Override
    public boolean produtoFoiVendido(Produto produto) {
        long produtoId = produto.getId().orElseThrow(() -> new IllegalArgumentException("Produto sem id."));
        return contexto.usar(conexao -> {
            try (PreparedStatement comando = conexao.prepareStatement(
                    "SELECT EXISTS (SELECT 1 FROM item_venda WHERE produto_id = ?)")) {
                comando.setLong(1, produtoId);
                try (ResultSet resultado = comando.executeQuery()) {
                    resultado.next();
                    return resultado.getBoolean(1);
                }
            }
        });
    }

    /** Dados de uma venda acumulados enquanto as linhas do JOIN são lidas. */
    private static final class Linha {
        private LocalDateTime dataHora;
        private FormaPagamento forma;
        private Cliente cliente;
        private String operador;
        private final List<Venda.ItemRegistrado> itens = new ArrayList<>();
    }

    private static long inserirVenda(Connection conexao, Venda venda, SessaoCaixa sessao, Operador operador,
                                     ResultadoPagamento pagamento) throws SQLException {
        String sql = """
                INSERT INTO venda (data_hora, cliente_id, operador_id, sessao_caixa_id, forma_pagamento,
                                   valor_total, valor_recebido, troco, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id""";
        try (PreparedStatement comando = conexao.prepareStatement(sql)) {
            comando.setObject(1, venda.getDataHora().orElseThrow(() ->
                    new IllegalStateException("Só venda finalizada pode ser gravada.")));
            Mapeadores.definirOuNulo(comando, 2, venda.getCliente().flatMap(Cliente::getId).orElse(null),
                    Types.BIGINT);
            comando.setLong(3, operador.getId().orElseThrow(() ->
                    new IllegalStateException("Operador sem id.")));
            comando.setLong(4, sessao.getId().orElseThrow(() ->
                    new IllegalStateException("Sessão de caixa sem id.")));
            comando.setString(5, venda.getFormaPagamento().orElseThrow().name());
            comando.setBigDecimal(6, venda.calcularTotal());
            Mapeadores.definirOuNulo(comando, 7, pagamento.valorRecebido(), Types.NUMERIC);
            Mapeadores.definirOuNulo(comando, 8, pagamento.troco(), Types.NUMERIC);
            comando.setString(9, venda.getStatus().name());
            return Mapeadores.idGerado(comando);
        }
    }

    private static void inserirItens(Connection conexao, Venda venda) throws SQLException {
        String sql = "INSERT INTO item_venda (venda_id, produto_id, quantidade, preco_unitario, custo_unitario) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement comando = conexao.prepareStatement(sql)) {
            for (ItemVenda item : venda.getItens()) {
                comando.setLong(1, venda.getId().orElseThrow());
                comando.setLong(2, item.getProduto().getId().orElseThrow(() ->
                        new IllegalStateException("Produto \"" + item.getProduto().getNome() + "\" sem id.")));
                comando.setBigDecimal(3, item.getQuantidade());
                comando.setBigDecimal(4, item.getPrecoUnitario());
                Mapeadores.definirOuNulo(comando, 5, item.getCustoUnitario().orElse(null), Types.NUMERIC);
                comando.addBatch();
            }
            comando.executeBatch();
        }
    }

    private static long idDe(Cliente cliente) {
        return cliente.getId().orElseThrow(() -> new IllegalArgumentException("Cliente sem id."));
    }
}
