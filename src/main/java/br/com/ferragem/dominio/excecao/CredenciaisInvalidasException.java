package br.com.ferragem.dominio.excecao;

/**
 * Lançada quando login ou senha não conferem.
 *
 * <p>A mensagem é a mesma nos dois casos de propósito: dizer "login não existe" ajudaria alguém a
 * descobrir quais logins são válidos.</p>
 */
public class CredenciaisInvalidasException extends RegraNegocioException {

    private static final long serialVersionUID = 1L;

    /** Cria a exceção com a mensagem padrão. */
    public CredenciaisInvalidasException() {
        super("Login ou senha incorretos.");
    }

    /**
     * @param mensagem mensagem específica (ex.: senha atual errada na troca de senha)
     */
    public CredenciaisInvalidasException(String mensagem) {
        super(mensagem);
    }
}
