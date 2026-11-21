package br.com.ferragem.dominio.modelo.caixa;

/**
 * Resultado da conferência do caixa (RN14).
 */
public enum SituacaoFechamento {
    /** Contado igual ao esperado. */
    CONFERE,
    /** Contado menor que o esperado: falta dinheiro. */
    QUEBRA,
    /** Contado maior que o esperado: sobra dinheiro. */
    SOBRA
}
