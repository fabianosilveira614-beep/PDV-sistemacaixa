package br.com.ferragem.dominio.modelo.fiado;

import br.com.ferragem.dominio.excecao.DebitoJaQuitadoException;
import br.com.ferragem.dominio.excecao.ValorPagamentoInvalidoException;
import br.com.ferragem.dominio.modelo.cliente.Cliente;
import br.com.ferragem.dominio.modelo.produto.ProdutoUnidade;
import br.com.ferragem.dominio.modelo.venda.FormaPagamento;
import br.com.ferragem.dominio.modelo.venda.Venda;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Testes de uma compra fiado isolada (RN10). */
class DebitoTest {

    private static final LocalDateTime COMPRA = LocalDateTime.of(2026, 11, 5, 10, 0);
    private static final LocalDateTime PAGAMENTO = LocalDateTime.of(2026, 11, 20, 16, 0);

    private Cliente seuZe;
    private Debito debito;

    private static BigDecimal valor(String texto) {
        return new BigDecimal(texto);
    }

    @BeforeEach
    void preparar() throws Exception {
        seuZe = new Cliente("Seu Zé", null, null);
        debito = new Debito(seuZe, 102, valor("100.00"), COMPRA);
    }

    @Test
    @DisplayName("Compra fiado nasce ABERTA, sem nada pago")
    void nasceAberto() {
        assertEquals(StatusDebito.ABERTO, debito.getStatus());
        assertEquals(valor("100.00"), debito.calcularSaldo());
        assertEquals(valor("0.00"), debito.getValorPago());
    }

    @Test
    @DisplayName("CT-16 compra de 100,00, paga 30,00: fica PARCIAL com saldo 70,00")
    void ct16_pagamentoParcial() throws Exception {
        debito.registrarPagamento(valor("30.00"), FormaPagamento.DINHEIRO, PAGAMENTO);

        assertEquals(StatusDebito.PARCIAL, debito.getStatus());
        assertEquals(valor("70.00"), debito.calcularSaldo());
        assertEquals(1, debito.getPagamentos().size());
    }

    @Test
    @DisplayName("CT-17 pagar o saldo exato quita; pagar de novo é recusado")
    void ct17_quitacaoEPagamentoRepetido() throws Exception {
        debito.registrarPagamento(valor("30"), FormaPagamento.DINHEIRO, PAGAMENTO);
        debito.registrarPagamento(valor("70"), FormaPagamento.PIX, PAGAMENTO);

        assertEquals(StatusDebito.QUITADO, debito.getStatus());
        assertThrows(DebitoJaQuitadoException.class,
                () -> debito.registrarPagamento(valor("1"), FormaPagamento.DINHEIRO, PAGAMENTO));
    }

    @Test
    @DisplayName("CT-18 pagamento zero ou maior que o saldo é recusado sem alterar nada")
    void ct18_valorInvalido() {
        assertThrows(ValorPagamentoInvalidoException.class,
                () -> debito.registrarPagamento(valor("0"), FormaPagamento.DINHEIRO, PAGAMENTO));
        assertThrows(ValorPagamentoInvalidoException.class,
                () -> debito.registrarPagamento(valor("100.01"), FormaPagamento.DINHEIRO, PAGAMENTO));
        assertThrows(ValorPagamentoInvalidoException.class,
                () -> debito.registrarPagamento(valor("10.005"), FormaPagamento.DINHEIRO, PAGAMENTO));

        assertEquals(StatusDebito.ABERTO, debito.getStatus());
        assertEquals(0, debito.getPagamentos().size());
    }

    @Test
    @DisplayName("Não se paga fiado com fiado")
    void naoPagaFiadoComFiado() {
        assertThrows(IllegalArgumentException.class,
                () -> debito.registrarPagamento(valor("10"), FormaPagamento.FIADO, PAGAMENTO));
    }

    @Test
    @DisplayName("Dívida gerada de uma venda fiado tem o total e o cliente da venda")
    void dividaGeradaDaVendaFiado() throws Exception {
        ProdutoUnidade cadeado = new ProdutoUnidade("CAD-01", null, "Cadeado", valor("20"), valor("35.00"));
        cadeado.reporEstoque(valor("5"));
        Venda venda = new Venda();
        venda.adicionarItem(cadeado, valor("2"));
        venda.identificarCliente(seuZe);
        venda.finalizar(FormaPagamento.FIADO, COMPRA);
        venda.atribuirId(158);

        Debito gerado = Debito.daVendaFiado(venda);

        assertEquals(valor("70.00"), gerado.getValorOriginal());
        assertEquals(158L, gerado.getVendaId());
        assertSame(seuZe, gerado.getCliente());
        assertEquals(COMPRA, gerado.getCriadoEm());
    }

    @Test
    @DisplayName("Venda que não é fiado (ou não está salva) não gera dívida")
    void vendaNaoFiadoNaoGeraDivida() throws Exception {
        ProdutoUnidade cadeado = new ProdutoUnidade("CAD-01", null, "Cadeado", valor("20"), valor("35.00"));
        cadeado.reporEstoque(valor("5"));
        Venda emDinheiro = new Venda();
        emDinheiro.adicionarItem(cadeado, valor("1"));
        emDinheiro.finalizar(FormaPagamento.DINHEIRO, COMPRA);
        emDinheiro.atribuirId(1);

        Venda fiadoSemId = new Venda();
        fiadoSemId.adicionarItem(cadeado, valor("1"));
        fiadoSemId.identificarCliente(seuZe);
        fiadoSemId.finalizar(FormaPagamento.FIADO, COMPRA);

        assertThrows(IllegalArgumentException.class, () -> Debito.daVendaFiado(emDinheiro));
        assertThrows(IllegalArgumentException.class, () -> Debito.daVendaFiado(fiadoSemId));
    }
}
