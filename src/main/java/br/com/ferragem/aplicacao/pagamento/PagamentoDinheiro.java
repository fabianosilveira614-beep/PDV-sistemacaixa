package br.com.ferragem.aplicacao.pagamento;

import br.com.ferragem.dominio.comum.Decimais;
import br.com.ferragem.dominio.excecao.PagamentoInsuficienteException;
import br.com.ferragem.dominio.excecao.RegraNegocioException;
import br.com.ferragem.dominio.excecao.ValorPagamentoInvalidoException;
import br.com.ferragem.dominio.modelo.venda.FormaPagamento;

import java.math.BigDecimal;

/**
 * Pagamento em dinheiro: confere o valor entregue e calcula o troco (RN09).
 */
public class PagamentoDinheiro implements ProcessadorPagamento {

    @Override
    public FormaPagamento forma() {
        return FormaPagamento.DINHEIRO;
    }

    /**
     * @param total         valor a pagar
     * @param valorRecebido dinheiro entregue; se nulo, considera-se o valor exato
     * @return recebido e troco ({@code recebido − total})
     * @throws PagamentoInsuficienteException se o dinheiro entregue for menor que o total
     */
    @Override
    public ResultadoPagamento processar(BigDecimal total, BigDecimal valorRecebido)
            throws RegraNegocioException {
        BigDecimal recebido = valorRecebido == null ? total : valorRecebido;
        if (Decimais.casasDecimais(recebido) > Decimais.CASAS_DINHEIRO) {
            throw new ValorPagamentoInvalidoException("O valor recebido aceita no máximo 2 casas decimais (centavos).");
        }
        if (recebido.compareTo(total) < 0) {
            throw new PagamentoInsuficienteException(total, recebido);
        }
        return new ResultadoPagamento(recebido, recebido.subtract(total), "");
    }
}
