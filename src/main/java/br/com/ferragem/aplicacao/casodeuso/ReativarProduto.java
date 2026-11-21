package br.com.ferragem.aplicacao.casodeuso;

import br.com.ferragem.aplicacao.porta.ProdutoRepository;
import br.com.ferragem.dominio.modelo.produto.Produto;

import java.util.Objects;

/**
 * Devolve à venda um produto inativado. Sem isto, um produto tirado de linha por engano ficaria
 * inativo para sempre (e não dá para cadastrar outro com o mesmo código, que é único).
 */
public class ReativarProduto {

    private final ProdutoRepository produtos;

    /**
     * @param produtos onde os produtos ficam
     */
    public ReativarProduto(ProdutoRepository produtos) {
        this.produtos = Objects.requireNonNull(produtos);
    }

    /**
     * @param produtoId produto a reativar
     * @throws IllegalArgumentException se o produto não existir
     */
    public void executar(long produtoId) {
        Produto produto = produtos.buscarPorId(produtoId).orElseThrow(() ->
                new IllegalArgumentException("Produto " + produtoId + " não encontrado."));
        produto.reativar();
        produtos.salvar(produto);
    }
}
