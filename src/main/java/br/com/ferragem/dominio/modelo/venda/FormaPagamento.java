package br.com.ferragem.dominio.modelo.venda;

/**
 * Formas de pagamento aceitas no caixa.
 *
 * <p>Além de listar os valores possíveis, o enum carrega uma regra: quais formas exigem um
 * cliente identificado. Assim a {@link Venda} pergunta à forma de pagamento, em vez de ter um
 * {@code if (forma == FIADO)} espalhado pelo código.</p>
 */
public enum FormaPagamento {

    /** Dinheiro em espécie; calcula troco. */
    DINHEIRO("Dinheiro", false),
    /** Pix (simulado). */
    PIX("Pix", false),
    /** Cartão de crédito (simulado). */
    CREDITO("Crédito", false),
    /** Cartão de débito (simulado). */
    DEBITO("Débito", false),
    /** Venda a prazo para cliente cadastrado; gera um débito (RN01). */
    FIADO("Fiado", true);

    private final String descricao;
    private final boolean exigeCliente;

    FormaPagamento(String descricao, boolean exigeCliente) {
        this.descricao = descricao;
        this.exigeCliente = exigeCliente;
    }

    /** @return nome para exibir na tela */
    public String getDescricao() {
        return descricao;
    }

    /** @return {@code true} se esta forma só pode ser usada com cliente cadastrado */
    public boolean exigeCliente() {
        return exigeCliente;
    }
}
