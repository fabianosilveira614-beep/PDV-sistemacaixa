package br.com.ferragem.dominio.modelo.produto;

import br.com.ferragem.dominio.excecao.DadoObrigatorioException;
import br.com.ferragem.dominio.excecao.PrecoInvalidoException;
import br.com.ferragem.dominio.excecao.QuantidadeInvalidaException;

import java.math.BigDecimal;

/**
 * Produto vendido a granel por peso: prego, arame, grampo.
 * Aceita até 3 casas decimais, a precisão de uma balança em gramas (0,250 kg = 250 g).
 */
public class ProdutoKg extends Produto {

    private static final int CASAS_PERMITIDAS = 3;

    /**
     * @param codigo       código interno, obrigatório
     * @param codigoBarras código de barras, opcional
     * @param nome         nome, obrigatório
     * @param precoCusto   preço de custo por quilo
     * @param precoVenda   preço de venda por quilo
     * @throws DadoObrigatorioException se código ou nome estiverem vazios
     * @throws PrecoInvalidoException   se algum preço for inválido
     */
    public ProdutoKg(String codigo, String codigoBarras, String nome,
                     BigDecimal precoCusto, BigDecimal precoVenda)
            throws DadoObrigatorioException, PrecoInvalidoException {
        super(codigo, codigoBarras, nome, precoCusto, precoVenda);
    }

    @Override
    protected void verificarFracionamento(BigDecimal quantidade) throws QuantidadeInvalidaException {
        if (casasDecimais(quantidade) > CASAS_PERMITIDAS) {
            throw new QuantidadeInvalidaException("\"" + getNome() + "\" é vendido por quilo: "
                    + "use no máximo 3 casas decimais, a precisão em gramas (recebido "
                    + quantidade.toPlainString() + ").");
        }
    }

    @Override
    public UnidadeMedida getUnidade() {
        return UnidadeMedida.KG;
    }
}
