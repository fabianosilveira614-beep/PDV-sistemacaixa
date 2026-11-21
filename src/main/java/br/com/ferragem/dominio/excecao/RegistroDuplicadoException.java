package br.com.ferragem.dominio.excecao;

/**
 * Lançada quando um dado que deve ser único já está em uso (RN03): código ou código de barras
 * de produto, CPF de cliente, login de operador.
 */
public class RegistroDuplicadoException extends RegraNegocioException {

    private static final long serialVersionUID = 1L;

    /**
     * @param mensagem explicação do que está repetido, pronta para o usuário
     */
    public RegistroDuplicadoException(String mensagem) {
        super(mensagem);
    }
}
