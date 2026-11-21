package br.com.ferragem.apresentacao.comum;

import br.com.ferragem.dominio.modelo.produto.UnidadeMedida;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;

/**
 * Conversão entre o que o operador digita/vê e os tipos do sistema, no padrão brasileiro.
 *
 * <p>Fica na apresentação porque é assunto de tela: o domínio trabalha com {@link BigDecimal}
 * e não sabe o que é "R$" nem vírgula decimal.</p>
 */
public final class Formatos {

    private static final Locale BRASIL = Locale.of("pt", "BR");
    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private Formatos() {
    }

    /**
     * @param valor quantia
     * @return texto como "R$ 1.234,50"
     */
    public static String moeda(BigDecimal valor) {
        NumberFormat formato = NumberFormat.getCurrencyInstance(BRASIL);
        // o Java usa espaço não separável depois de "R$"; troca por espaço comum para não quebrar o alinhamento
        return formato.format(valor).replace('\u00A0', ' ');
    }

    /**
     * Número sem zeros inúteis e com vírgula: 2.500 vira "2,5"; 4.000 vira "4".
     *
     * @param numero quantidade ou valor
     * @return texto no padrão brasileiro
     */
    public static String numero(BigDecimal numero) {
        BigDecimal enxuto = numero.stripTrailingZeros();
        if (enxuto.scale() < 0) {
            enxuto = enxuto.setScale(0); // 1E+1 vira 10
        }
        NumberFormat formato = NumberFormat.getNumberInstance(BRASIL);
        formato.setMaximumFractionDigits(Math.max(enxuto.scale(), 0));
        formato.setMinimumFractionDigits(Math.max(enxuto.scale(), 0));
        return formato.format(enxuto);
    }

    /**
     * @param quantidade quantidade vendida ou em estoque
     * @param unidade    unidade do produto
     * @return texto como "2,5 kg", "4 un" ou "1,35 m"
     */
    public static String quantidade(BigDecimal quantidade, UnidadeMedida unidade) {
        return numero(quantidade) + " " + unidade.getSigla();
    }

    /**
     * @param momento data e hora
     * @return texto como "20/11/2026 19:30"
     */
    public static String dataHora(LocalDateTime momento) {
        return DATA_HORA.format(momento);
    }

    /**
     * Interpreta um valor digitado. Aceita "10", "10,5", "10,50", "1.234,50", "R$ 10,00" e também
     * ponto decimal ("10.50") quando não há vírgula.
     *
     * <p>Não arredonda nem corta casas: "10,555" vira 10.555 e quem recusa é a regra de negócio
     * (no máximo 2 casas para dinheiro). Assim a mensagem de erro sai de um lugar só.</p>
     *
     * @param texto o que foi digitado
     * @return o número, ou vazio se o texto não for um número
     */
    public static Optional<BigDecimal> lerNumero(String texto) {
        if (texto == null) {
            return Optional.empty();
        }
        String limpo = texto.replace('\u00A0', ' ').replace("R$", "").replace(" ", "");
        if (limpo.isEmpty()) {
            return Optional.empty();
        }
        if (limpo.contains(",")) {
            limpo = limpo.replace(".", "").replace(',', '.');
        }
        try {
            return Optional.of(new BigDecimal(limpo));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
