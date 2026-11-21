package br.com.ferragem.dominio.modelo.produto;

/**
 * Motivo de uma mudança no estoque.
 */
public enum TipoMovimentoEstoque {
    /** Saída por venda confirmada. */
    SAIDA_VENDA,
    /** Entrada de mercadoria comprada do fornecedor. */
    ENTRADA_COMPRA,
    /** Correção de contagem (pode ser para mais ou para menos). */
    AJUSTE,
    /** Perda, quebra ou avaria. */
    PERDA
}
