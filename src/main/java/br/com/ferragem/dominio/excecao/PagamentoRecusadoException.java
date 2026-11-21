package br.com.ferragem.dominio.excecao;

/**
 * Lançada quando a operadora do cartão recusa a transação.
 */
public class PagamentoRecusadoException extends RegraNegocioException {

    private static final long serialVersionUID = 1L;

    /**
     * @param motivo motivo informado pela operadora (ex.: "saldo insuficiente")
     */
    public PagamentoRecusadoException(String motivo) {
        super("Pagamento recusado pela operadora do cartão: " + motivo + ". Tente outra forma de pagamento.");
    }
}
