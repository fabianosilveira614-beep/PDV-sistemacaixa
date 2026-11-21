package br.com.ferragem.dominio.excecao;

/**
 * Lançada ao tentar abrir um caixa quando já existe outro aberto (RN08).
 */
public class CaixaJaAbertoException extends RegraNegocioException {

    private static final long serialVersionUID = 1L;

    /** Cria a exceção com a mensagem padrão. */
    public CaixaJaAbertoException() {
        super("Já existe um caixa aberto. Feche-o antes de abrir outro.");
    }
}
