package br.com.ferragem.dominio.modelo.caixa;

import br.com.ferragem.dominio.comum.Decimais;
import br.com.ferragem.dominio.modelo.venda.FormaPagamento;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Uma movimentação registrada no caixa. Imutável: um movimento errado se corrige com outro
 * movimento, nunca apagando ou editando o anterior (assim a conferência sempre fecha).
 *
 * @param tipo      entrada de venda, entrada de fiado, suprimento ou sangria
 * @param forma     forma do dinheiro; suprimento e sangria são sempre em DINHEIRO; nunca FIADO
 * @param valor     valor positivo; o sentido (entra ou sai) vem do tipo
 * @param descricao texto livre (ex.: "venda #102", "levado ao cofre")
 * @param dataHora  momento do movimento
 */
public record MovimentoCaixa(TipoMovimentoCaixa tipo, FormaPagamento forma, BigDecimal valor,
                             String descricao, LocalDateTime dataHora) {

    /**
     * Valida o movimento antes de criá-lo.
     *
     * @throws NullPointerException     se tipo, forma, valor ou data forem nulos
     * @throws IllegalArgumentException se o valor não for positivo ou tiver mais de 2 casas, a forma for
     *                                  FIADO ou um suprimento/sangria não for em dinheiro
     */
    public MovimentoCaixa {
        Objects.requireNonNull(tipo, "tipo");
        Objects.requireNonNull(forma, "forma");
        Objects.requireNonNull(valor, "valor");
        Objects.requireNonNull(dataHora, "dataHora");
        if (valor.signum() <= 0 || Decimais.casasDecimais(valor) > Decimais.CASAS_DINHEIRO) {
            throw new IllegalArgumentException("Valor de movimento inválido: " + valor);
        }
        valor = valor.setScale(Decimais.CASAS_DINHEIRO);
        if (forma == FormaPagamento.FIADO) {
            throw new IllegalArgumentException("Venda fiado não entra no caixa: nenhum dinheiro foi recebido.");
        }
        boolean manual = tipo == TipoMovimentoCaixa.SUPRIMENTO || tipo == TipoMovimentoCaixa.SANGRIA;
        if (manual && forma != FormaPagamento.DINHEIRO) {
            throw new IllegalArgumentException("Suprimento e sangria são sempre em dinheiro.");
        }
        descricao = descricao == null ? "" : descricao.trim();
    }

    /**
     * Efeito do movimento no dinheiro físico da gaveta. Pix e cartão não passam pela gaveta.
     *
     * @return valor com sinal (+ entra, − sai), ou zero se não for em dinheiro
     */
    public BigDecimal efeitoNaGaveta() {
        if (forma != FormaPagamento.DINHEIRO) {
            return BigDecimal.ZERO;
        }
        return tipo.getSinal() > 0 ? valor : valor.negate();
    }

    /** @return {@code true} se for entrada de venda ou de fiado (dinheiro de cliente) */
    public boolean ehRecebimento() {
        return tipo == TipoMovimentoCaixa.ENTRADA_VENDA || tipo == TipoMovimentoCaixa.ENTRADA_PAGAMENTO_DEBITO;
    }
}
