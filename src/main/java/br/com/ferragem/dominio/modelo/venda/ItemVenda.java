package br.com.ferragem.dominio.modelo.venda;

import br.com.ferragem.dominio.modelo.produto.Produto;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

import static br.com.ferragem.dominio.comum.Decimais.CASAS_DINHEIRO;

/**
 * Uma linha do carrinho: qual produto, quanto e por qual preço.
 *
 * <p>O preço unitário é <b>copiado</b> do produto no momento da venda. Se o preço do produto
 * mudar amanhã, as vendas de hoje continuam com o valor cobrado de verdade.</p>
 *
 * <p><b>Encapsulamento:</b> o construtor e os métodos que alteram o item não são públicos, só
 * a {@link Venda} (mesmo pacote) os usa. Qualquer outra classe só consegue ler o item, e toda
 * alteração passa pelas validações da venda.</p>
 */
public class ItemVenda {

    private final Produto produto;
    private BigDecimal quantidade;
    private BigDecimal precoUnitario;

    ItemVenda(Produto produto, BigDecimal quantidade, BigDecimal precoUnitario) {
        this.produto = Objects.requireNonNull(produto, "produto");
        this.quantidade = Objects.requireNonNull(quantidade, "quantidade");
        this.precoUnitario = Objects.requireNonNull(precoUnitario, "precoUnitario");
    }

    /**
     * Subtotal da linha: quantidade × preço unitário, arredondado para centavos (HALF_UP).
     * Exemplo: 0,333 kg × R$ 9,99 = 3,32667, que vira R$ 3,33.
     *
     * @return subtotal com 2 casas decimais
     */
    public BigDecimal calcularSubtotal() {
        return quantidade.multiply(precoUnitario).setScale(CASAS_DINHEIRO, RoundingMode.HALF_UP);
    }

    /**
     * @param codigoProduto código de um produto
     * @return {@code true} se esta linha é desse produto
     */
    public boolean ehDoProduto(String codigoProduto) {
        return produto.getCodigo().equals(codigoProduto);
    }

    void alterarQuantidade(BigDecimal novaQuantidade) {
        this.quantidade = Objects.requireNonNull(novaQuantidade);
    }

    void alterarPrecoUnitario(BigDecimal novoPreco) {
        this.precoUnitario = Objects.requireNonNull(novoPreco);
    }

    /** @return produto vendido */
    public Produto getProduto() {
        return produto;
    }

    /** @return quantidade vendida, na unidade do produto */
    public BigDecimal getQuantidade() {
        return quantidade;
    }

    /** @return preço unitário cobrado nesta venda */
    public BigDecimal getPrecoUnitario() {
        return precoUnitario;
    }

    @Override
    public String toString() {
        return produto.getNome() + " " + quantidade.toPlainString() + " " + produto.getUnidade().getSigla()
                + " x " + precoUnitario.toPlainString() + " = " + calcularSubtotal().toPlainString();
    }
}
