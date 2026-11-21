package br.com.ferragem.dominio.excecao;

/**
 * Lançada quando o perfil do operador não permite a ação (ex.: vender abaixo do custo, RN13).
 */
public class OperacaoNaoAutorizadaException extends RegraNegocioException {

    private static final long serialVersionUID = 1L;

    /**
     * @param mensagem explicação do que foi negado e quem pode autorizar
     */
    public OperacaoNaoAutorizadaException(String mensagem) {
        super(mensagem);
    }
}
