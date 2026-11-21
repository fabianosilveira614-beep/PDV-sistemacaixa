package br.com.ferragem.dominio.excecao;

/**
 * Lançada quando a senha de um operador não atende ao tamanho mínimo.
 */
public class SenhaFracaException extends RegraNegocioException {

    private static final long serialVersionUID = 1L;

    /**
     * @param tamanhoMinimo quantidade mínima de caracteres
     */
    public SenhaFracaException(int tamanhoMinimo) {
        super("A senha deve ter pelo menos " + tamanhoMinimo + " caracteres.");
    }

    /**
     * @param motivo explicação de por que a senha foi recusada
     */
    public SenhaFracaException(String motivo) {
        super(motivo);
    }
}
