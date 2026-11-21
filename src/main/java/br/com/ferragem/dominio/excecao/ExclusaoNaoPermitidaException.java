package br.com.ferragem.dominio.excecao;

/**
 * Lançada ao tentar excluir um cliente que ainda deve compras fiado (RN12).
 */
public class ExclusaoNaoPermitidaException extends RegraNegocioException {

    private static final long serialVersionUID = 1L;

    /**
     * @param mensagem motivo da recusa, pronto para o usuário
     */
    public ExclusaoNaoPermitidaException(String mensagem) {
        super(mensagem);
    }
}
