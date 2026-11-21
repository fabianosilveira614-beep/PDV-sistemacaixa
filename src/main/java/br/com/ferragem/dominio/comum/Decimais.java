package br.com.ferragem.dominio.comum;

import java.math.BigDecimal;

/**
 * Constantes e operações com números decimais usadas por várias classes do domínio.
 *
 * <p>Centraliza regras que antes estavam repetidas em {@code Produto}, {@code Venda} e
 * {@code ItemVenda}: se a precisão do dinheiro mudar, muda só aqui.</p>
 */
public final class Decimais {

    /** Casas decimais do dinheiro (centavos). */
    public static final int CASAS_DINHEIRO = 2;

    private Decimais() {
    }

    /**
     * Conta as casas decimais significativas: {@code 2.50} tem 1, {@code 3} tem 0, {@code 0.250} tem 2.
     *
     * @param valor número a analisar
     * @return quantidade de casas decimais, ignorando zeros no final
     */
    public static int casasDecimais(BigDecimal valor) {
        return Math.max(0, valor.stripTrailingZeros().scale());
    }
}
