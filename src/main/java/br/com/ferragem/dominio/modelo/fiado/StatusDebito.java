package br.com.ferragem.dominio.modelo.fiado;

/**
 * Situação de uma compra fiado. É calculada a partir dos pagamentos, nunca definida à mão.
 */
public enum StatusDebito {
    /** Nada foi pago ainda. */
    ABERTO,
    /** Parte foi paga; ainda há saldo. */
    PARCIAL,
    /** Totalmente paga. */
    QUITADO
}
