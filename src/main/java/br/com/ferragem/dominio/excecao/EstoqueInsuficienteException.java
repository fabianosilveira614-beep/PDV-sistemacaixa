package br.com.ferragem.dominio.excecao;

import java.math.BigDecimal;

/**
 * Lançada quando se tenta retirar do estoque mais do que existe (RN04).
 *
 * <p>Guarda o disponível e o solicitado para a tela poder sugerir a quantidade máxima.</p>
 */
public class EstoqueInsuficienteException extends RegraNegocioException {

    private static final long serialVersionUID = 1L;

    private final BigDecimal disponivel;
    private final BigDecimal solicitado;

    /**
     * @param nomeProduto nome do produto, para a mensagem
     * @param disponivel  quantidade em estoque
     * @param solicitado  quantidade pedida
     */
    public EstoqueInsuficienteException(String nomeProduto, BigDecimal disponivel, BigDecimal solicitado) {
        super("Estoque insuficiente de \"" + nomeProduto + "\": disponível "
                + disponivel.toPlainString() + ", solicitado " + solicitado.toPlainString() + ".");
        this.disponivel = disponivel;
        this.solicitado = solicitado;
    }

    /** @return quantidade que havia em estoque */
    public BigDecimal getDisponivel() {
        return disponivel;
    }

    /** @return quantidade que foi pedida */
    public BigDecimal getSolicitado() {
        return solicitado;
    }
}
