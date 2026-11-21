package br.com.ferragem.dominio.excecao;

/**
 * Lançada quando um CPF não tem 11 dígitos ou seus dígitos verificadores não conferem (RN16).
 */
public class CpfInvalidoException extends RegraNegocioException {

    private static final long serialVersionUID = 1L;

    /**
     * @param valorInformado texto digitado pelo usuário, repetido na mensagem para ele conferir
     */
    public CpfInvalidoException(String valorInformado) {
        super("CPF inválido: \"" + valorInformado + "\". Confira os números digitados.");
    }
}
