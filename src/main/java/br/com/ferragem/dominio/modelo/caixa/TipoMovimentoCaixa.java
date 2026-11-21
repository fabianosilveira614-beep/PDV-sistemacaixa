package br.com.ferragem.dominio.modelo.caixa;

/**
 * Tipos de movimentação de um caixa. Cada tipo sabe se o dinheiro entra ou sai.
 */
public enum TipoMovimentoCaixa {

    /** Venda paga em dinheiro, Pix ou cartão (venda fiado não gera movimento). */
    ENTRADA_VENDA(1),
    /** Cliente pagando compras fiado. */
    ENTRADA_PAGAMENTO_DEBITO(1),
    /** Reforço de dinheiro na gaveta (ex.: troco). */
    SUPRIMENTO(1),
    /** Retirada de dinheiro da gaveta (ex.: levar ao cofre). */
    SANGRIA(-1);

    private final int sinal;

    TipoMovimentoCaixa(int sinal) {
        this.sinal = sinal;
    }

    /** @return {@code +1} se o dinheiro entra, {@code -1} se sai */
    public int getSinal() {
        return sinal;
    }
}
