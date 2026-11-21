package br.com.ferragem.dominio.excecao;

/**
 * Lançada quando o valor de um pagamento de fiado é inválido (RN10): zero, negativo, com mais de
 * 2 casas decimais ou maior que o saldo devido.
 */
public class ValorPagamentoInvalidoException extends RegraNegocioException {

    private static final long serialVersionUID = 1L;

    /**
     * @param mensagem explicação do problema, pronta para o usuário
     */
    public ValorPagamentoInvalidoException(String mensagem) {
        super(mensagem);
    }
}
