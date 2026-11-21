package br.com.ferragem.aplicacao.casodeuso;

import br.com.ferragem.aplicacao.porta.GerenciadorTransacao;
import br.com.ferragem.aplicacao.porta.HistoricoVendas;
import br.com.ferragem.aplicacao.porta.ProdutoRepository;
import br.com.ferragem.dominio.excecao.RegraNegocioException;
import br.com.ferragem.dominio.modelo.produto.Produto;

import java.util.Objects;

/**
 * Exclui um produto cadastrado por engano.
 *
 * <p><b>Regra:</b> produto que já foi vendido não pode ser apagado, porque as vendas antigas
 * apontam para ele e o histórico (comprovantes, relatórios, fiado) ficaria quebrado. Nesse caso
 * ele é <b>inativado</b>: sai do caixa, mas continua no histórico. É o mesmo comportamento da
 * exclusão de clientes ({@link ExcluirCliente}). Produto nunca vendido é apagado de verdade, com
 * os seus movimentos de estoque.</p>
 *
 * <p>A conferência e a exclusão rodam na mesma transação: uma venda concluída entre uma e outra
 * não deixa o produto ser apagado por baixo dela.</p>
 */
public class ExcluirProduto {

    /** O que de fato aconteceu com o produto. */
    public enum Resultado {
        /** Nunca foi vendido: apagado. */
        EXCLUIDO,
        /** Já tinha vendas: inativado no lugar de apagado. */
        INATIVADO
    }

    private final ProdutoRepository produtos;
    private final HistoricoVendas historico;
    private final GerenciadorTransacao transacao;

    /**
     * @param produtos  onde os produtos ficam
     * @param historico para saber se o produto já foi vendido
     * @param transacao controle de transação
     */
    public ExcluirProduto(ProdutoRepository produtos, HistoricoVendas historico, GerenciadorTransacao transacao) {
        this.produtos = Objects.requireNonNull(produtos);
        this.historico = Objects.requireNonNull(historico);
        this.transacao = Objects.requireNonNull(transacao);
    }

    /**
     * @param produtoId produto a excluir
     * @return {@link Resultado#EXCLUIDO} ou, se já tinha vendas, {@link Resultado#INATIVADO}
     * @throws RegraNegocioException não lançada hoje; existe para a assinatura da transação
     * @throws IllegalArgumentException se o produto não existir
     */
    public Resultado executar(long produtoId) throws RegraNegocioException {
        return transacao.executar(() -> {
            Produto produto = produtos.buscarPorId(produtoId).orElseThrow(() ->
                    new IllegalArgumentException("Produto " + produtoId + " não encontrado."));
            if (historico.produtoFoiVendido(produto)) {
                produto.inativar();
                produtos.salvar(produto);
                return Resultado.INATIVADO;
            }
            produtos.excluir(produto);
            return Resultado.EXCLUIDO;
        });
    }
}
