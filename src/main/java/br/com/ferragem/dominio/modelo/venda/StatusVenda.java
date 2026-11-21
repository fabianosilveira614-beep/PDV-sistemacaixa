package br.com.ferragem.dominio.modelo.venda;

/**
 * Situação de uma venda. A transição só acontece em um sentido: EM_ABERTO para FINALIZADA.
 */
public enum StatusVenda {
    /** Carrinho sendo montado: itens podem ser adicionados, alterados e removidos. */
    EM_ABERTO,
    /** Venda concluída: não pode mais ser alterada. */
    FINALIZADA
}
