package br.com.ferragem.dominio.excecao;

/**
 * Lançada ao tentar pagar uma compra fiado que já foi totalmente quitada (RN10).
 */
public class DebitoJaQuitadoException extends RegraNegocioException {

    private static final long serialVersionUID = 1L;

    /**
     * @param descricaoDebito identificação da compra (ex.: "venda #102")
     */
    public DebitoJaQuitadoException(String descricaoDebito) {
        super("A compra fiado " + descricaoDebito + " já está quitada.");
    }
}
