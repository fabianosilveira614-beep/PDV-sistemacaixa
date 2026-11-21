package br.com.ferragem.dominio.modelo.produto;

import br.com.ferragem.dominio.excecao.DadoObrigatorioException;
import br.com.ferragem.dominio.excecao.PrecoInvalidoException;
import br.com.ferragem.dominio.excecao.QuantidadeInvalidaException;

import java.math.BigDecimal;

/**
 * Produto vendido em peças inteiras: parafuso, cadeado, martelo.
 * Só aceita quantidades inteiras (não existe meio cadeado).
 */
public class ProdutoUnidade extends Produto {

    /**
     * @param codigo       código interno, obrigatório
     * @param codigoBarras código de barras, opcional
     * @param nome         nome, obrigatório
     * @param precoCusto   preço de custo
     * @param precoVenda   preço de venda
     * @throws DadoObrigatorioException se código ou nome estiverem vazios
     * @throws PrecoInvalidoException   se algum preço for inválido
     */
    public ProdutoUnidade(String codigo, String codigoBarras, String nome,
                          BigDecimal precoCusto, BigDecimal precoVenda)
            throws DadoObrigatorioException, PrecoInvalidoException {
        super(codigo, codigoBarras, nome, precoCusto, precoVenda);
    }

    @Override
    protected void verificarFracionamento(BigDecimal quantidade) throws QuantidadeInvalidaException {
        if (casasDecimais(quantidade) > 0) {
            throw new QuantidadeInvalidaException("\"" + getNome() + "\" é vendido por unidade: "
                    + "informe um número inteiro (recebido " + quantidade.toPlainString() + ").");
        }
    }

    @Override
    public UnidadeMedida getUnidade() {
        return UnidadeMedida.UNIDADE;
    }
}
