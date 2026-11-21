package br.com.ferragem.dominio.excecao;

import java.math.BigDecimal;

/**
 * Lançada quando uma sangria pede mais dinheiro do que há na gaveta.
 *
 * <p>Considera só o dinheiro em espécie: entradas em Pix ou cartão não estão na gaveta.</p>
 */
public class SaldoCaixaInsuficienteException extends RegraNegocioException {

    private static final long serialVersionUID = 1L;

    /**
     * @param emDinheiro dinheiro que deveria estar na gaveta
     * @param solicitado valor que se tentou retirar
     */
    public SaldoCaixaInsuficienteException(BigDecimal emDinheiro, BigDecimal solicitado) {
        super("Sangria de R$ " + solicitado.toPlainString() + " maior que o dinheiro na gaveta (R$ "
                + emDinheiro.toPlainString() + ").");
    }
}
