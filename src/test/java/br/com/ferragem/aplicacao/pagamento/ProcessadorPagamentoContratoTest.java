package br.com.ferragem.aplicacao.pagamento;

import br.com.ferragem.aplicacao.memoria.OperadoraCartaoFalsa;
import br.com.ferragem.dominio.excecao.PagamentoInsuficienteException;
import br.com.ferragem.dominio.excecao.ValorPagamentoInvalidoException;
import br.com.ferragem.dominio.modelo.venda.FormaPagamento;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Contrato das estratégias de pagamento (substituição de Liskov) e regras de cada uma.
 */
class ProcessadorPagamentoContratoTest {

    private static final BigDecimal DEZ = new BigDecimal("10.00");

    static Stream<ProcessadorPagamento> todasAsEstrategias() {
        OperadoraCartaoFalsa operadora = new OperadoraCartaoFalsa();
        return Stream.of(new PagamentoDinheiro(), new PagamentoPix(), new PagamentoFiado(),
                new PagamentoCartao(FormaPagamento.CREDITO, operadora),
                new PagamentoCartao(FormaPagamento.DEBITO, operadora));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("todasAsEstrategias")
    void todaEstrategiaDevolveResultadoValidoSemTrocoNoValorExato(ProcessadorPagamento estrategia) throws Exception {
        ResultadoPagamento resultado = estrategia.processar(DEZ, DEZ);

        assertNotNull(resultado);
        assertEquals(0, resultado.troco().signum());
        assertNotNull(resultado.codigoAutorizacao());
    }

    @Test
    @DisplayName("CT-14 dinheiro: total 37,40, recebido 50,00 dá troco 12,60")
    void ct14_troco() throws Exception {
        ResultadoPagamento resultado = new PagamentoDinheiro().processar(new BigDecimal("37.40"), new BigDecimal("50.00"));

        assertEquals(new BigDecimal("12.60"), resultado.troco());
    }

    @Test
    @DisplayName("CT-15 dinheiro: recebido 30,00 para total 37,40 é recusado")
    void ct15_dinheiroInsuficiente() {
        assertThrows(PagamentoInsuficienteException.class,
                () -> new PagamentoDinheiro().processar(new BigDecimal("37.40"), new BigDecimal("30.00")));
    }

    @Test
    @DisplayName("Dinheiro: valor recebido com mais de 2 casas é recusado (o troco teria fração de centavo)")
    void dinheiroComCentavoFracionado() {
        assertThrows(ValorPagamentoInvalidoException.class,
                () -> new PagamentoDinheiro().processar(new BigDecimal("6.00"), new BigDecimal("10.555")));
    }

    @Test
    @DisplayName("Catálogo devolve a estratégia da forma e recusa duplicadas ou ausentes")
    void catalogo() {
        PagamentoPix pix = new PagamentoPix();
        ProcessadoresPagamento catalogo = new ProcessadoresPagamento(List.of(pix, new PagamentoDinheiro()));

        assertSame(pix, catalogo.para(FormaPagamento.PIX));
        assertThrows(IllegalArgumentException.class, () -> catalogo.para(FormaPagamento.FIADO));
        assertThrows(IllegalArgumentException.class,
                () -> new ProcessadoresPagamento(List.of(new PagamentoPix(), new PagamentoPix())));
    }

    @Test
    @DisplayName("Cartão só aceita crédito ou débito")
    void cartaoSoCreditoOuDebito() {
        assertThrows(IllegalArgumentException.class,
                () -> new PagamentoCartao(FormaPagamento.PIX, new OperadoraCartaoFalsa()));
    }
}
