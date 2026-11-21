package br.com.ferragem.dominio.modelo.caixa;

/**
 * Situação de uma sessão de caixa. A transição só acontece em um sentido: ABERTO para FECHADO.
 */
public enum StatusCaixa {
    /** Aceita vendas, recebimentos, sangrias e suprimentos. */
    ABERTO,
    /** Conferido e encerrado; não aceita mais nada. */
    FECHADO
}
