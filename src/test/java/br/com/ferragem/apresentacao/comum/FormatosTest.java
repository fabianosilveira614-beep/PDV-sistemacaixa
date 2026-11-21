package br.com.ferragem.apresentacao.comum;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.com.ferragem.dominio.modelo.produto.UnidadeMedida;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** O que o operador digita vira o número certo, e os valores aparecem no padrão brasileiro. */
class FormatosTest {

    private static BigDecimal lido(String texto) {
        return Formatos.lerNumero(texto).orElseThrow();
    }

    @Test
    @DisplayName("Aceita vírgula, ponto de milhar, R$ e ponto decimal")
    void lerNumerosValidos() {
        assertEquals(0, new BigDecimal("10").compareTo(lido("10")));
        assertEquals(0, new BigDecimal("10.5").compareTo(lido("10,5")));
        assertEquals(0, new BigDecimal("1234.50").compareTo(lido("1.234,50")));
        assertEquals(0, new BigDecimal("25").compareTo(lido("R$ 25,00")));
        assertEquals(0, new BigDecimal("10.50").compareTo(lido("10.50")));
        assertEquals(0, new BigDecimal("0.500").compareTo(lido(" 0,500 ")));
    }

    @Test
    @DisplayName("Não arredonda: casas a mais chegam inteiras para a regra de negócio recusar")
    void naoArredonda() {
        assertEquals(new BigDecimal("10.555"), lido("10,555"));
    }

    @Test
    @DisplayName("Texto que não é número vira vazio, não exceção")
    void lerInvalidos() {
        assertTrue(Formatos.lerNumero("").isEmpty());
        assertTrue(Formatos.lerNumero(null).isEmpty());
        assertTrue(Formatos.lerNumero("dez").isEmpty());
        assertTrue(Formatos.lerNumero("1,2,3").isEmpty());
    }

    @Test
    @DisplayName("Quantidade sem zeros inúteis, com vírgula e a sigla da unidade")
    void quantidades() {
        assertEquals("2,5 kg", Formatos.quantidade(new BigDecimal("2.500"), UnidadeMedida.KG));
        assertEquals("4 un", Formatos.quantidade(new BigDecimal("4.000"), UnidadeMedida.UNIDADE));
        assertEquals("10 un", Formatos.quantidade(new BigDecimal("10"), UnidadeMedida.UNIDADE));
        assertEquals("1,35 m", Formatos.quantidade(new BigDecimal("1.35"), UnidadeMedida.METRO));
        assertEquals("1.200 un", Formatos.quantidade(new BigDecimal("1200"), UnidadeMedida.UNIDADE));
    }

    @Test
    @DisplayName("Moeda e data no padrão brasileiro")
    void formatar() {
        assertEquals("R$ 1.234,50", Formatos.moeda(new BigDecimal("1234.5")));
        assertEquals("R$ 0,00", Formatos.moeda(BigDecimal.ZERO));
        assertEquals("20/11/2026 08:05", Formatos.dataHora(LocalDateTime.of(2026, 11, 20, 8, 5)));
    }
}
