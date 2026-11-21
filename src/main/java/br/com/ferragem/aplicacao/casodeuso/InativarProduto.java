package br.com.ferragem.aplicacao.casodeuso;

import br.com.ferragem.aplicacao.porta.ProdutoRepository;
import br.com.ferragem.dominio.modelo.produto.Produto;

import java.util.Objects;

/**
 * Tira um produto de linha (exclusão lógica, RN12): some das buscas do PDV, mas continua no
 * banco para o histórico das vendas antigas.
 */
public class InativarProduto {

    private final ProdutoRepository produtos;

    /**
     * @param produtos produtos
     */
    public InativarProduto(ProdutoRepository produtos) {
        this.produtos = Objects.requireNonNull(produtos);
    }

    /**
     * @param produtoId produto a inativar
     * @throws IllegalArgumentException se o produto não existir
     */
    public void executar(long produtoId) {
        Produto produto = produtos.buscarPorId(produtoId).orElseThrow(() ->
                new IllegalArgumentException("Produto " + produtoId + " não encontrado."));
        produto.inativar();
        produtos.salvar(produto);
    }
}
