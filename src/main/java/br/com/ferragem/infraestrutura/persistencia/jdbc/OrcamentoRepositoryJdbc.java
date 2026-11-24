package br.com.ferragem.infraestrutura.persistencia.jdbc;

import br.com.ferragem.aplicacao.porta.OrcamentoRepository;
import br.com.ferragem.dominio.modelo.caixa.Operador;
import br.com.ferragem.dominio.modelo.cliente.Cliente;
import br.com.ferragem.dominio.modelo.orcamento.Orcamento;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Orçamentos no PostgreSQL (tabelas {@code orcamento} e {@code item_orcamento}).
 */
public class OrcamentoRepositoryJdbc implements OrcamentoRepository {

    /** LEFT JOIN nos itens: um orçamento cujo único produto foi apagado continua aparecendo (vazio). */
    private static final String SELECT = "SELECT orc.id AS orc_id, orc.criado_em, op.nome AS atendente, "
            + "i.quantidade, i.preco_unitario, " + Mapeadores.COLUNAS_PRODUTO + ", " + Mapeadores.COLUNAS_CLIENTE + """
             FROM orcamento orc
             JOIN operador op ON op.id = orc.operador_id
             LEFT JOIN item_orcamento i ON i.orcamento_id = orc.id
             LEFT JOIN produto p ON p.id = i.produto_id
             LEFT JOIN cliente c ON c.id = orc.cliente_id
            """;

    private final ContextoJdbc contexto;

    /**
     * @param contexto acesso ao banco; não pode ser nulo
     */
    public OrcamentoRepositoryJdbc(ContextoJdbc contexto) {
        this.contexto = Objects.requireNonNull(contexto, "contexto");
    }

    @Override
    public void salvar(Orcamento orcamento, Operador autor) {
        contexto.usar(conexao -> {
            orcamento.atribuirId(inserirCabecalho(conexao, orcamento, autor));
            inserirItens(conexao, orcamento);
            return null;
        });
    }

    @Override
    public List<Orcamento> listar() {
        return contexto.usar(conexao -> ler(conexao, SELECT + " ORDER BY orc.criado_em DESC, orc.id DESC, i.id", null));
    }

    @Override
    public Optional<Orcamento> buscarPorId(long id) {
        return contexto.usar(conexao -> ler(conexao, SELECT + " WHERE orc.id = ? ORDER BY i.id", id))
                .stream().findFirst();
    }

    @Override
    public void excluir(long id) {
        contexto.usar(conexao -> {
            try (PreparedStatement comando = conexao.prepareStatement("DELETE FROM orcamento WHERE id = ?")) {
                comando.setLong(1, id); // os itens saem junto (ON DELETE CASCADE)
                return comando.executeUpdate();
            }
        });
    }

    /** Dados de um orçamento acumulados enquanto as linhas do JOIN são lidas. */
    private static final class Linha {
        private LocalDateTime criadoEm;
        private String atendente;
        private Cliente cliente;
        private final List<Orcamento.Item> itens = new ArrayList<>();
    }

    private static List<Orcamento> ler(Connection conexao, String sql, Long id) throws SQLException {
        Map<Long, Linha> lidos = new LinkedHashMap<>();
        try (PreparedStatement comando = conexao.prepareStatement(sql)) {
            if (id != null) {
                comando.setLong(1, id);
            }
            try (ResultSet linha = comando.executeQuery()) {
                while (linha.next()) {
                    long orcamentoId = linha.getLong("orc_id");
                    Linha dados = lidos.get(orcamentoId);
                    if (dados == null) {
                        dados = new Linha();
                        dados.criadoEm = linha.getObject("criado_em", LocalDateTime.class);
                        dados.atendente = linha.getString("atendente");
                        dados.cliente = linha.getObject("c_id") == null ? null : Mapeadores.cliente(linha);
                        lidos.put(orcamentoId, dados);
                    }
                    if (linha.getObject("p_id") != null) {
                        dados.itens.add(new Orcamento.Item(Mapeadores.produto(linha),
                                linha.getBigDecimal("quantidade"), linha.getBigDecimal("preco_unitario")));
                    }
                }
            }
        }
        List<Orcamento> resultado = new ArrayList<>();
        lidos.forEach((orcamentoId, d) ->
                resultado.add(Orcamento.restaurar(orcamentoId, d.criadoEm, d.cliente, d.atendente, d.itens)));
        return resultado;
    }

    private static long inserirCabecalho(Connection conexao, Orcamento orcamento, Operador autor) throws SQLException {
        try (PreparedStatement comando = conexao.prepareStatement(
                "INSERT INTO orcamento (criado_em, cliente_id, operador_id) VALUES (?, ?, ?) RETURNING id")) {
            comando.setObject(1, orcamento.getCriadoEm());
            Mapeadores.definirOuNulo(comando, 2, orcamento.getCliente().flatMap(Cliente::getId).orElse(null),
                    Types.BIGINT);
            comando.setLong(3, autor.getId().orElseThrow(() -> new IllegalStateException("Operador sem id.")));
            return Mapeadores.idGerado(comando);
        }
    }

    private static void inserirItens(Connection conexao, Orcamento orcamento) throws SQLException {
        String sql = "INSERT INTO item_orcamento (orcamento_id, produto_id, quantidade, preco_unitario) VALUES (?, ?, ?, ?)";
        try (PreparedStatement comando = conexao.prepareStatement(sql)) {
            for (Orcamento.Item item : orcamento.getItens()) {
                comando.setLong(1, orcamento.getId().orElseThrow());
                comando.setLong(2, item.produto().getId().orElseThrow(() ->
                        new IllegalStateException("Produto \"" + item.produto().getNome() + "\" sem id.")));
                comando.setBigDecimal(3, item.quantidade());
                comando.setBigDecimal(4, item.precoUnitario());
                comando.addBatch();
            }
            comando.executeBatch();
        }
    }
}
