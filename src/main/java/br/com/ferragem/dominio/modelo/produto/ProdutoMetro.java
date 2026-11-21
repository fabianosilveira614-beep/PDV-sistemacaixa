package br.com.ferragem.dominio.modelo.produto;

import br.com.ferragem.dominio.excecao.DadoObrigatorioException;
import br.com.ferragem.dominio.excecao.PrecoInvalidoException;
import br.com.ferragem.dominio.excecao.QuantidadeInvalidaException;

import java.math.BigDecimal;

/**
 * Produto cortado no comprimento: corrente, fio, mangueira.
 * Aceita até 2 casas decimais, a precisão de uma trena em centímetros (1,35 m).
 */
public class ProdutoMetro extends Produto {

    private static final int CASAS_PERMITIDAS = 2;

    /**
     * @param codigo       código interno, obrigatório
     * @param codigoBarras código de barras, opcional
     * @param nome         nome, obrigatório
     * @param precoCusto   preço de custo por metro
     * @param precoVenda   preço de venda por metro
     * @throws DadoObrigatorioException se código ou nome estiverem vazios
     * @throws PrecoInvalidoException   se algum preço for inválido
     */
    public ProdutoMetro(String codigo, String codigoBarras, String nome,
                        BigDecimal precoCusto, BigDecimal precoVenda)
            throws DadoObrigatorioException, PrecoInvalidoException {
        super(codigo, codigoBarras, nome, precoCusto, precoVenda);
    }

    @Override
    protected void verificarFracionamento(BigDecimal quantidade) throws QuantidadeInvalidaException {
        if (casasDecimais(quantidade) > CASAS_PERMITIDAS) {
            throw new QuantidadeInvalidaException("\"" + getNome() + "\" é vendido por metro: "
                    + "use no máximo 2 casas decimais, a precisão em centímetros (recebido "
                    + quantidade.toPlainString() + ").");
        }
    }

    @Override
    public UnidadeMedida getUnidade() {
        return UnidadeMedida.METRO;
    }
}
