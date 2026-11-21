package br.com.ferragem.dominio.excecao;

/**
 * Lançada ao tentar finalizar uma venda sem nenhum item no carrinho (RN05).
 */
public class VendaVaziaException extends RegraNegocioException {

    private static final long serialVersionUID = 1L;

    /** Cria a exceção com a mensagem padrão. */
    public VendaVaziaException() {
        super("Não é possível finalizar uma venda sem itens. Adicione ao menos um produto.");
    }
}
