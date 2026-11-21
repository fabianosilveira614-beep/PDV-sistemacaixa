package br.com.ferragem.dominio.modelo.produto;

import br.com.ferragem.dominio.excecao.DadoObrigatorioException;
import br.com.ferragem.dominio.excecao.PrecoInvalidoException;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Cria o tipo certo de produto a partir da unidade escolhida no cadastro.
 *
 * <p>É o <b>único</b> lugar do sistema que conhece todas as subclasses de {@link Produto}. Por ser
 * um {@code switch} sobre o enum, o compilador avisa se alguém criar uma unidade nova e esquecer
 * de tratá-la aqui.</p>
 */
public final class FabricaProduto {

    private FabricaProduto() {
    }

    /**
     * @param unidade      unidade de venda escolhida
     * @param codigo       código interno
     * @param codigoBarras código de barras, opcional
     * @param nome         nome
     * @param precoCusto   preço de custo
     * @param precoVenda   preço de venda
     * @return produto do tipo correspondente à unidade, com estoque zero
     * @throws DadoObrigatorioException se código ou nome estiverem vazios
     * @throws PrecoInvalidoException   se algum preço for inválido
     */
    public static Produto criar(UnidadeMedida unidade, String codigo, String codigoBarras, String nome,
                                BigDecimal precoCusto, BigDecimal precoVenda)
            throws DadoObrigatorioException, PrecoInvalidoException {
        Objects.requireNonNull(unidade, "unidade");
        return switch (unidade) {
            case UNIDADE -> new ProdutoUnidade(codigo, codigoBarras, nome, precoCusto, precoVenda);
            case KG -> new ProdutoKg(codigo, codigoBarras, nome, precoCusto, precoVenda);
            case METRO -> new ProdutoMetro(codigo, codigoBarras, nome, precoCusto, precoVenda);
        };
    }
}
