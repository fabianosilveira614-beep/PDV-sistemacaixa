package br.com.ferragem.dominio.excecao;

/**
 * Lançada quando se tenta vender fiado sem cliente identificado ou para cliente inativo (RN01).
 */
public class FiadoNaoPermitidoException extends RegraNegocioException {

    private static final long serialVersionUID = 1L;

    /**
     * @param motivo explicação específica (sem cliente, cliente inativo)
     */
    public FiadoNaoPermitidoException(String motivo) {
        super("Venda fiado não permitida: " + motivo);
    }
}
