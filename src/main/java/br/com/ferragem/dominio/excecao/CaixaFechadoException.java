package br.com.ferragem.dominio.excecao;

/**
 * Lançada ao tentar movimentar ou fechar um caixa que não está aberto (RN08).
 */
public class CaixaFechadoException extends RegraNegocioException {

    private static final long serialVersionUID = 1L;

    /** Cria a exceção com a mensagem padrão. */
    public CaixaFechadoException() {
        super("O caixa está fechado. Abra o caixa para registrar movimentações.");
    }
}
