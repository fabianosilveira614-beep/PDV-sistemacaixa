package br.com.ferragem.dominio.excecao;

/**
 * Lançada quando um valor informado no controle de caixa é inválido: abertura ou contagem
 * negativa, sangria ou suprimento zerado, mais de 2 casas decimais.
 */
public class ValorCaixaInvalidoException extends RegraNegocioException {

    private static final long serialVersionUID = 1L;

    /**
     * @param mensagem explicação do problema, pronta para o usuário
     */
    public ValorCaixaInvalidoException(String mensagem) {
        super(mensagem);
    }
}
