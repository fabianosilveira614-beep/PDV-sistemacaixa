package br.com.ferragem.dominio.modelo.produto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

/**
 * Registro de uma mudança no estoque: o quê, quanto, por quê e quando. É a trilha de auditoria
 * que responde "por que o estoque deste produto está assim?".
 *
 * @param produto    produto movimentado
 * @param tipo       motivo da movimentação
 * @param quantidade quantidade com sinal: positiva entrou, negativa saiu; nunca zero
 * @param motivo     texto livre (ex.: "venda #102", "nota fiscal 4521")
 * @param dataHora   momento da movimentação
 * @param vendaId    venda que causou a saída, quando houver; nulo nos demais casos
 */
public record MovimentoEstoque(Produto produto, TipoMovimentoEstoque tipo, BigDecimal quantidade,
                               String motivo, LocalDateTime dataHora, Long vendaId) {

    /**
     * Valida o movimento antes de criá-lo.
     *
     * @throws NullPointerException     se produto, tipo, quantidade ou data forem nulos
     * @throws IllegalArgumentException se a quantidade for zero
     */
    public MovimentoEstoque {
        Objects.requireNonNull(produto, "produto");
        Objects.requireNonNull(tipo, "tipo");
        Objects.requireNonNull(quantidade, "quantidade");
        Objects.requireNonNull(dataHora, "dataHora");
        if (quantidade.signum() == 0) {
            throw new IllegalArgumentException("Movimento de estoque não pode ter quantidade zero.");
        }
        motivo = motivo == null ? "" : motivo.trim();
    }

    /**
     * Cria o movimento de saída de uma venda.
     *
     * @param produto    produto vendido
     * @param quantidade quantidade vendida (positiva; será registrada como saída)
     * @param vendaId    id da venda
     * @param momento    momento da venda
     * @return movimento do tipo {@link TipoMovimentoEstoque#SAIDA_VENDA}
     */
    public static MovimentoEstoque saidaPorVenda(Produto produto, BigDecimal quantidade, long vendaId,
                                                 LocalDateTime momento) {
        return new MovimentoEstoque(produto, TipoMovimentoEstoque.SAIDA_VENDA, quantidade.negate(),
                "venda #" + vendaId, momento, vendaId);
    }

    /** @return venda que originou o movimento, se houver */
    public Optional<Long> getVendaId() {
        return Optional.ofNullable(vendaId);
    }
}
