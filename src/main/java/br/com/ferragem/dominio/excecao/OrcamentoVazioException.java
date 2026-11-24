package br.com.ferragem.dominio.excecao;

/**
 * Lançada ao tentar fazer orçamento de um carrinho sem itens.
 */
public class OrcamentoVazioException extends RegraNegocioException {

    private static final long serialVersionUID = 1L;

    /** Cria a exceção com a mensagem padrão. */
    public OrcamentoVazioException() {
        super("O carrinho está vazio: passe os produtos antes de fazer o orçamento.");
    }
}
