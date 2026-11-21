package br.com.ferragem.infraestrutura.persistencia.jdbc;

import br.com.ferragem.aplicacao.porta.ProdutoRepository;
import br.com.ferragem.dominio.modelo.produto.Produto;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Produtos no PostgreSQL (tabela {@code produto}).
 *
 * <p><b>Bloqueio de linha:</b> dentro de uma transação, as buscas de um produto específico usam
 * {@code SELECT ... FOR UPDATE}. Assim, se duas operações tentarem baixar o estoque do mesmo
 * produto ao mesmo tempo, a segunda espera a primeira terminar e lê o estoque já atualizado,
 * em vez de as duas lerem "10" e uma sobrescrever a outra (problema da "atualização perdida").</p>
 */
public class ProdutoRepositoryJdbc implements ProdutoRepository {

    private static final String SELECT = "SELECT " + Mapeadores.COLUNAS_PRODUTO + " FROM produto p ";

    private final ContextoJdbc contexto;

    /**
     * @param contexto acesso ao banco; não pode ser nulo
     */
    public ProdutoRepositoryJdbc(ContextoJdbc contexto) {
        this.contexto = Objects.requireNonNull(contexto, "contexto");
    }

    @Override
    public void salvar(Produto produto) {
        contexto.usar(conexao -> {
            if (produto.getId().isEmpty()) {
                inserir(conexao, produto);
            } else {
                atualizar(conexao, produto);
            }
            return null;
        });
    }

    @Override
    public Optional<Produto> buscarPorCodigo(String codigo) {
        return buscarUm("WHERE p.codigo = ? AND p.ativo", codigo);
    }

    @Override
    public Optional<Produto> buscarPorCodigoBarras(String codigoBarras) {
        return buscarUm("WHERE p.codigo_barras = ? AND p.ativo", codigoBarras);
    }

    @Override
    public Optional<Produto> buscarPorId(long id) {
        return buscarUm("WHERE p.id = ?", id);
    }

    @Override
    public List<Produto> buscarPorNome(String trecho) {
        return contexto.usar(conexao -> {
            try (PreparedStatement comando = conexao.prepareStatement(
                    SELECT + "WHERE p.ativo AND p.nome ILIKE ? ORDER BY p.nome")) {
                comando.setString(1, "%" + escaparLike(trecho) + "%");
                return lerTodos(comando);
            }
        });
    }

    @Override
    public List<Produto> listar(String trecho, boolean incluirInativos) {
        String sql = SELECT + "WHERE (? OR p.ativo) AND (p.nome ILIKE ? OR p.codigo ILIKE ?) ORDER BY p.nome";
        String padrao = "%" + escaparLike(trecho) + "%";
        return contexto.usar(conexao -> {
            try (PreparedStatement comando = conexao.prepareStatement(sql)) {
                comando.setBoolean(1, incluirInativos);
                comando.setString(2, padrao);
                comando.setString(3, padrao);
                return lerTodos(comando);
            }
        });
    }

    @Override
    public void excluir(Produto produto) {
        long id = produto.getId().orElseThrow(() -> new IllegalArgumentException("Produto sem id."));
        contexto.usar(conexao -> {
            // os movimentos (estoque inicial, ajustes) pertencem ao cadastro errado e vão junto;
            // se houver venda, o banco recusa (chave estrangeira em item_venda): defesa em profundidade
            try (PreparedStatement movimentos = conexao.prepareStatement(
                    "DELETE FROM movimento_estoque WHERE produto_id = ?");
                 PreparedStatement apagar = conexao.prepareStatement("DELETE FROM produto WHERE id = ?")) {
                movimentos.setLong(1, id);
                movimentos.executeUpdate();
                apagar.setLong(1, id);
                return apagar.executeUpdate();
            }
        });
    }

    @Override
    public boolean existeCodigo(String codigo, Long ignorarId) {
        return existe("codigo", codigo, ignorarId);
    }

    @Override
    public boolean existeCodigoBarras(String codigoBarras, Long ignorarId) {
        return existe("codigo_barras", codigoBarras, ignorarId);
    }

    private Optional<Produto> buscarUm(String filtro, Object valor) {
        String bloqueio = contexto.emTransacao() ? " FOR UPDATE" : "";
        return contexto.usar(conexao -> {
            try (PreparedStatement comando = conexao.prepareStatement(SELECT + filtro + bloqueio)) {
                comando.setObject(1, valor);
                return lerTodos(comando).stream().findFirst();
            }
        });
    }

    private boolean existe(String coluna, String valor, Long ignorarId) {
        // a coluna vem de constante interna, nunca do usuário: concatenar aqui não abre SQL injection
        String sql = "SELECT EXISTS (SELECT 1 FROM produto WHERE " + coluna + " = ? AND id <> COALESCE(?, -1))";
        return contexto.usar(conexao -> {
            try (PreparedStatement comando = conexao.prepareStatement(sql)) {
                comando.setString(1, valor);
                Mapeadores.definirOuNulo(comando, 2, ignorarId, Types.BIGINT);
                try (ResultSet resultado = comando.executeQuery()) {
                    resultado.next();
                    return resultado.getBoolean(1);
                }
            }
        });
    }

    private static void inserir(java.sql.Connection conexao, Produto produto) throws SQLException {
        String sql = """
                INSERT INTO produto (codigo, codigo_barras, nome, unidade, preco_custo, preco_venda,
                                     quantidade_estoque, ativo, estoque_minimo)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id""";
        try (PreparedStatement comando = conexao.prepareStatement(sql)) {
            preencher(comando, produto);
            produto.atribuirId(Mapeadores.idGerado(comando));
        }
    }

    private static void atualizar(java.sql.Connection conexao, Produto produto) throws SQLException {
        String sql = """
                UPDATE produto SET codigo = ?, codigo_barras = ?, nome = ?, unidade = ?, preco_custo = ?,
                                   preco_venda = ?, quantidade_estoque = ?, ativo = ?, estoque_minimo = ?
                WHERE id = ?""";
        try (PreparedStatement comando = conexao.prepareStatement(sql)) {
            preencher(comando, produto);
            comando.setLong(10, produto.getId().orElseThrow());
            comando.executeUpdate();
        }
    }

    private static void preencher(PreparedStatement comando, Produto produto) throws SQLException {
        comando.setString(1, produto.getCodigo());
        Mapeadores.definirOuNulo(comando, 2, produto.getCodigoBarras().orElse(null), Types.VARCHAR);
        comando.setString(3, produto.getNome());
        comando.setString(4, produto.getUnidade().name());
        comando.setBigDecimal(5, produto.getPrecoCusto());
        comando.setBigDecimal(6, produto.getPrecoVenda());
        comando.setBigDecimal(7, produto.getQuantidadeEstoque());
        comando.setBoolean(8, produto.estaAtivo());
        Mapeadores.definirOuNulo(comando, 9, produto.getEstoqueMinimo().orElse(null), Types.NUMERIC);
    }

    private static List<Produto> lerTodos(PreparedStatement comando) throws SQLException {
        List<Produto> produtos = new ArrayList<>();
        try (ResultSet linhas = comando.executeQuery()) {
            while (linhas.next()) {
                produtos.add(Mapeadores.produto(linhas));
            }
        }
        return produtos;
    }

    /** Impede que "%" ou "_" digitados pelo usuário virem curingas do LIKE. */
    static String escaparLike(String texto) {
        return texto.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
