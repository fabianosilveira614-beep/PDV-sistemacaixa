package br.com.ferragem.dominio.excecao;

/**
 * Lançada quando a data inicial de uma consulta é posterior à data final.
 */
public class PeriodoInvalidoException extends RegraNegocioException {

    private static final long serialVersionUID = 1L;

    /** Cria a exceção com a mensagem padrão. */
    public PeriodoInvalidoException() {
        super("A data inicial deve ser anterior ou igual à data final.");
    }
}
