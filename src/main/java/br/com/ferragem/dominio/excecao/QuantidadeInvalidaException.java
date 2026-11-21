package br.com.ferragem.dominio.excecao;

/**
 * Lançada quando uma quantidade não é aceita pelo tipo do produto (RN06): zero, negativa
 * ou com mais casas decimais do que a unidade permite (ex.: 1,5 parafuso).
 */
public class QuantidadeInvalidaException extends RegraNegocioException {

    private static final long serialVersionUID = 1L;

    /**
     * @param mensagem explicação do problema, pronta para o usuário
     */
    public QuantidadeInvalidaException(String mensagem) {
        super(mensagem);
    }
}
