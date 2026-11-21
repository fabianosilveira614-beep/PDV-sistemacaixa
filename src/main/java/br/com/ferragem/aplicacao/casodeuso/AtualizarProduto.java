package br.com.ferragem.aplicacao.casodeuso;

import br.com.ferragem.aplicacao.porta.ProdutoRepository;
import br.com.ferragem.dominio.excecao.RegistroDuplicadoException;
import br.com.ferragem.dominio.excecao.RegraNegocioException;
import br.com.ferragem.dominio.modelo.produto.FabricaProduto;
import br.com.ferragem.dominio.modelo.produto.Produto;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Edita código, código de barras, nome e preços de um produto. A unidade não muda (um produto por
 * quilo não vira produto por unidade: seria outro produto). O estoque só muda por
 * {@link AjustarEstoque} ou por venda.
 */
public class AtualizarProduto {

    private final ProdutoRepository produtos;

    /**
     * @param produtos produtos
     */
    public AtualizarProduto(ProdutoRepository produtos) {
        this.produtos = Objects.requireNonNull(produtos);
    }

    /**
     * Dados do formulário de edição.
     *
     * @param produtoId     produto editado
     * @param codigo        novo código
     * @param codigoBarras  novo código de barras (vazio remove)
     * @param nome          novo nome
     * @param precoCusto    novo preço de custo
     * @param precoVenda    novo preço de venda
     * @param estoqueMinimo  novo estoque mínimo; nulo desliga o controle de reposição
     */
    public record Comando(long produtoId, String codigo, String codigoBarras, String nome,
                          BigDecimal precoCusto, BigDecimal precoVenda, BigDecimal estoqueMinimo) {
    }

    /**
     * @param comando novos dados
     * @return o produto atualizado
     * @throws RegraNegocioException dado obrigatório vazio, preço inválido ou código já usado por outro
     *                               produto; nesses casos o produto não é alterado
     * @throws IllegalArgumentException se o produto não existir
     */
    public Produto executar(Comando comando) throws RegraNegocioException {
        Produto produto = produtos.buscarPorId(comando.produtoId()).orElseThrow(() ->
                new IllegalArgumentException("Produto " + comando.produtoId() + " não encontrado."));

        // Monta um produto "rascunho" só para validar todos os campos de uma vez: se algo estiver
        // errado, a exceção sai daqui e o produto real nem é tocado.
        Produto rascunho = FabricaProduto.criar(produto.getUnidade(), comando.codigo(), comando.codigoBarras(),
                comando.nome(), comando.precoCusto(), comando.precoVenda());
        rascunho.definirEstoqueMinimo(comando.estoqueMinimo());
        exigirCodigosLivres(rascunho, comando.produtoId());

        produto.alterarIdentificacao(rascunho.getCodigo(), rascunho.getCodigoBarras().orElse(null), rascunho.getNome());
        produto.alterarPrecos(rascunho.getPrecoCusto(), rascunho.getPrecoVenda());
        produto.definirEstoqueMinimo(rascunho.getEstoqueMinimo().orElse(null));
        produtos.salvar(produto);
        return produto;
    }

    private void exigirCodigosLivres(Produto rascunho, long proprioId) throws RegistroDuplicadoException {
        if (produtos.existeCodigo(rascunho.getCodigo(), proprioId)) {
            throw new RegistroDuplicadoException("Já existe outro produto com o código " + rascunho.getCodigo() + ".");
        }
        if (rascunho.getCodigoBarras().filter(barras -> produtos.existeCodigoBarras(barras, proprioId)).isPresent()) {
            throw new RegistroDuplicadoException("Já existe outro produto com este código de barras.");
        }
    }
}
