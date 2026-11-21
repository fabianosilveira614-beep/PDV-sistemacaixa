package br.com.ferragem.infraestrutura.persistencia.jdbc;

import br.com.ferragem.aplicacao.porta.MovimentoEstoqueRepository;
import br.com.ferragem.dominio.modelo.produto.MovimentoEstoque;

import java.sql.PreparedStatement;
import java.sql.Types;
import java.util.Objects;

/**
 * Histórico de entradas e saídas de estoque (tabela {@code movimento_estoque}). Só insere:
 * movimento registrado não se altera nem se apaga, é a trilha de auditoria do estoque.
 */
public class MovimentoEstoqueRepositoryJdbc implements MovimentoEstoqueRepository {

    private final ContextoJdbc contexto;

    /**
     * @param contexto acesso ao banco; não pode ser nulo
     */
    public MovimentoEstoqueRepositoryJdbc(ContextoJdbc contexto) {
        this.contexto = Objects.requireNonNull(contexto, "contexto");
    }

    @Override
    public void registrar(MovimentoEstoque movimento) {
        String sql = """
                INSERT INTO movimento_estoque (produto_id, venda_id, tipo, quantidade, motivo, data_hora)
                VALUES (?, ?, ?, ?, ?, ?)""";
        contexto.usar(conexao -> {
            try (PreparedStatement comando = conexao.prepareStatement(sql)) {
                comando.setLong(1, movimento.produto().getId().orElseThrow(() ->
                        new IllegalStateException("Produto sem id: salve o produto antes do movimento.")));
                Mapeadores.definirOuNulo(comando, 2, movimento.vendaId(), Types.BIGINT);
                comando.setString(3, movimento.tipo().name());
                comando.setBigDecimal(4, movimento.quantidade());
                comando.setString(5, movimento.motivo());
                comando.setObject(6, movimento.dataHora());
                return comando.executeUpdate();
            }
        });
    }
}
