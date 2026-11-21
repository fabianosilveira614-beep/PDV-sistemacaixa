package br.com.ferragem.dominio.excecao;

/**
 * Lançada quando um preço viola as regras de preço (RN15): custo negativo, preço de venda
 * zero ou negativo, ou mais de duas casas decimais.
 */
public class PrecoInvalidoException extends RegraNegocioException {

    private static final long serialVersionUID = 1L;

    /**
     * @param mensagem explicação do problema, pronta para o usuário
     */
    public PrecoInvalidoException(String mensagem) {
        super(mensagem);
    }
}
