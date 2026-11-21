package br.com.ferragem.aplicacao.pagamento;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Resultado do recebimento de um valor.
 *
 * @param valorRecebido      quanto o cliente entregou (em dinheiro pode ser mais que o total; no fiado é zero)
 * @param troco              quanto devolver ao cliente; zero fora do dinheiro
 * @param codigoAutorizacao  código da operadora no cartão; vazio nas demais formas
 */
public record ResultadoPagamento(BigDecimal valorRecebido, BigDecimal troco, String codigoAutorizacao) {

    /** Valida e normaliza os dados. */
    public ResultadoPagamento {
        Objects.requireNonNull(valorRecebido, "valorRecebido");
        Objects.requireNonNull(troco, "troco");
        codigoAutorizacao = codigoAutorizacao == null ? "" : codigoAutorizacao;
    }

    /**
     * Pagamento exato, sem troco e sem autorização (Pix).
     *
     * @param valor valor pago
     * @return resultado com troco zero
     */
    static ResultadoPagamento exato(BigDecimal valor) {
        return new ResultadoPagamento(valor, BigDecimal.ZERO.setScale(2), "");
    }
}
