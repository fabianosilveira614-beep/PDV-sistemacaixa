package br.com.ferragem.dominio.excecao;

import java.math.BigDecimal;

/**
 * Lançada quando o dinheiro entregue pelo cliente é menor que o valor a pagar (RN09).
 */
public class PagamentoInsuficienteException extends RegraNegocioException {

    private static final long serialVersionUID = 1L;

    /**
     * @param total    valor a pagar
     * @param recebido valor entregue pelo cliente
     */
    public PagamentoInsuficienteException(BigDecimal total, BigDecimal recebido) {
        super("Valor recebido (R$ " + recebido.toPlainString() + ") é menor que o total (R$ "
                + total.toPlainString() + ").");
    }
}
