package br.com.ferragem.dominio.excecao;

/**
 * Base de todas as exceções de regra de negócio do sistema.
 *
 * <p>É <b>verificada</b> (estende {@link Exception}): o compilador obriga quem chama uma operação
 * que pode violar uma regra a tratar ou repassar o erro. Isso é proposital: uma regra violada é
 * uma situação esperada (o operador digitou uma quantidade maior que o estoque), e a interface
 * precisa mostrar uma mensagem clara ao usuário.</p>
 *
 * <p>É <b>abstrata</b> porque não existe "erro de regra genérico": cada violação tem sua própria
 * subclasse, e a tela pode tratá-las todas pelo tipo geral, com polimorfismo.</p>
 */
public abstract class RegraNegocioException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * @param mensagem texto pronto para ser exibido ao usuário
     */
    protected RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
