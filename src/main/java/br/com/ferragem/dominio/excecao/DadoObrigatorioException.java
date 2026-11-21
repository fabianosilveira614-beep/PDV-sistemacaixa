package br.com.ferragem.dominio.excecao;

/**
 * Lançada quando um dado obrigatório não foi informado (nome do produto, código, nome do cliente).
 */
public class DadoObrigatorioException extends RegraNegocioException {

    private static final long serialVersionUID = 1L;

    private final String campo;

    /**
     * @param campo nome do campo, como o usuário o conhece (ex.: "nome do produto")
     */
    public DadoObrigatorioException(String campo) {
        super("O campo \"" + campo + "\" é obrigatório.");
        this.campo = campo;
    }

    /** @return nome do campo que faltou, útil para a tela destacar o campo certo */
    public String getCampo() {
        return campo;
    }
}
