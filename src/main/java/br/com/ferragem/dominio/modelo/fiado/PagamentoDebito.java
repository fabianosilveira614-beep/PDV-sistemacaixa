package br.com.ferragem.dominio.modelo.fiado;

import br.com.ferragem.dominio.modelo.venda.FormaPagamento;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Um pagamento feito sobre uma compra fiado: quanto, como e quando.
 *
 * <p>É um {@code record}: uma classe imutável cujo único papel é carregar dados. O Java gera
 * sozinho o construtor, os métodos de acesso ({@code valor()}, {@code forma()}...),
 * {@code equals}, {@code hashCode} e {@code toString}. Faz sentido aqui porque um pagamento
 * registrado nunca muda: se houver erro, faz-se outro lançamento, não uma edição.</p>
 *
 * @param valor    valor pago, maior que zero
 * @param forma    forma de pagamento; nunca {@link FormaPagamento#FIADO}
 * @param dataHora momento do pagamento
 */
public record PagamentoDebito(BigDecimal valor, FormaPagamento forma, LocalDateTime dataHora) {

    /**
     * Construtor compacto: valida os dados antes de o record ser criado.
     *
     * @throws NullPointerException     se algum dado for nulo
     * @throws IllegalArgumentException se o valor não for positivo ou a forma for FIADO
     */
    public PagamentoDebito {
        Objects.requireNonNull(valor, "valor");
        Objects.requireNonNull(forma, "forma");
        Objects.requireNonNull(dataHora, "dataHora");
        if (valor.signum() <= 0) {
            throw new IllegalArgumentException("O valor do pagamento deve ser positivo: " + valor);
        }
        if (forma == FormaPagamento.FIADO) {
            throw new IllegalArgumentException("Não se paga fiado com fiado.");
        }
    }
}
