package br.com.ferragem.dominio.excecao;

/**
 * Lançada ao concluir uma venda com um produto que deixou de ser vendido (foi inativado no
 * cadastro) depois de entrar no carrinho.
 */
public class ProdutoIndisponivelException extends RegraNegocioException {

    private static final long serialVersionUID = 1L;

    /**
     * @param nomeProduto nome do produto que saiu de venda
     */
    public ProdutoIndisponivelException(String nomeProduto) {
        super("\"" + nomeProduto + "\" não está mais à venda (foi retirado do cadastro). "
                + "Remova o item do carrinho para concluir.");
    }
}
