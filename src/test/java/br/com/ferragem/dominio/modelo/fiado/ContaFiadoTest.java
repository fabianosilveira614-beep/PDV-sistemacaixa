package br.com.ferragem.dominio.modelo.fiado;

import br.com.ferragem.dominio.excecao.ValorPagamentoInvalidoException;
import br.com.ferragem.dominio.modelo.cliente.Cliente;
import br.com.ferragem.dominio.modelo.venda.FormaPagamento;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes da tela de fiado do cliente: total devido e as três formas de pagar.
 * O cenário é o do Seu Zé: compras #102 (R$ 100), #131 (R$ 80) e #158 (R$ 45).
 */
class ContaFiadoTest {

    private static final LocalDateTime HOJE = LocalDateTime.of(2026, 11, 20, 16, 0);

    private Debito venda102;
    private Debito venda131;
    private Debito venda158;
    private ContaFiado conta;

    private static BigDecimal valor(String texto) {
        return new BigDecimal(texto);
    }

    @BeforeEach
    void preparar() throws Exception {
        Cliente seuZe = new Cliente("Seu Zé", null, null);
        venda102 = new Debito(seuZe, 102, valor("100.00"), LocalDateTime.of(2026, 11, 5, 9, 0));
        venda131 = new Debito(seuZe, 131, valor("80.00"), LocalDateTime.of(2026, 11, 12, 9, 0));
        venda158 = new Debito(seuZe, 158, valor("45.00"), LocalDateTime.of(2026, 11, 18, 9, 0));
        // fora de ordem de propósito: a conta precisa ordenar da mais antiga para a mais nova
        conta = new ContaFiado(seuZe, List.of(venda158, venda102, venda131));
    }

    @Test
    @DisplayName("CT-19 total devido é a soma dos saldos (RN11): 100 + 80 + 45 = 225")
    void ct19_totalDevido() throws Exception {
        assertEquals(valor("225.00"), conta.calcularSaldoDevedor());

        venda102.registrarPagamento(valor("30"), FormaPagamento.DINHEIRO, HOJE);

        assertEquals(valor("195.00"), conta.calcularSaldoDevedor());
    }

    @Test
    @DisplayName("Pendentes aparecem da compra mais antiga para a mais nova")
    void pendentesEmOrdemCronologica() {
        assertEquals(List.of(venda102, venda131, venda158), conta.getPendentes());
    }

    @Test
    @DisplayName("Selecionadas #102 e #131, paga 150: #102 quitada, #131 com saldo 30, #158 intacta")
    void pagarSelecionadasDistribuiDaMaisAntiga() throws Exception {
        List<PagamentoAplicado> aplicados = conta.pagarSelecionadas(
                List.of(venda131, venda102), valor("150.00"), FormaPagamento.DINHEIRO, HOJE);

        assertEquals(StatusDebito.QUITADO, venda102.getStatus());
        assertEquals(StatusDebito.PARCIAL, venda131.getStatus());
        assertEquals(valor("30.00"), venda131.calcularSaldo());
        assertEquals(StatusDebito.ABERTO, venda158.getStatus());
        assertEquals(valor("75.00"), conta.calcularSaldoDevedor());

        assertEquals(2, aplicados.size());
        assertSame(venda102, aplicados.get(0).debito());
        assertEquals(valor("100.00"), aplicados.get(0).pagamento().valor());
        assertEquals(valor("50.00"), aplicados.get(1).pagamento().valor());
    }

    @Test
    @DisplayName("Compra quitada sai das pendentes, mas continua no histórico")
    void quitadaVaiParaOHistorico() throws Exception {
        conta.pagarSelecionadas(List.of(venda102), valor("100"), FormaPagamento.PIX, HOJE);

        assertEquals(List.of(venda131, venda158), conta.getPendentes());
        assertEquals(List.of(venda102), conta.getQuitadas());
    }

    @Test
    @DisplayName("Pagar só uma compra, em parte, não mexe nas outras")
    void pagarUmaCompraEmParte() throws Exception {
        conta.pagarSelecionadas(List.of(venda131), valor("20"), FormaPagamento.DEBITO, HOJE);

        assertEquals(valor("60.00"), venda131.calcularSaldo());
        assertEquals(StatusDebito.ABERTO, venda102.getStatus());
        assertEquals(StatusDebito.ABERTO, venda158.getStatus());
    }

    @Test
    @DisplayName("Valor livre de 150 sem escolher: abate #102 inteira e 50 da #131")
    void valorLivreAbateDaMaisAntiga() throws Exception {
        conta.pagarValorLivre(valor("150"), FormaPagamento.DINHEIRO, HOJE);

        assertEquals(StatusDebito.QUITADO, venda102.getStatus());
        assertEquals(valor("30.00"), venda131.calcularSaldo());
        assertEquals(valor("45.00"), venda158.calcularSaldo());
    }

    @Test
    @DisplayName("Quitar tudo zera a conta")
    void quitarTudo() throws Exception {
        List<PagamentoAplicado> aplicados = conta.quitarTudo(FormaPagamento.PIX, HOJE);

        assertEquals(3, aplicados.size());
        assertEquals(valor("0.00"), conta.calcularSaldoDevedor());
        assertTrue(conta.getPendentes().isEmpty());
    }

    @Test
    @DisplayName("Pagar mais que o total das selecionadas é recusado e nenhuma compra muda")
    void valorMaiorQueOSelecionadoEhRecusado() {
        assertThrows(ValorPagamentoInvalidoException.class, () -> conta.pagarSelecionadas(
                List.of(venda102, venda131), valor("180.01"), FormaPagamento.DINHEIRO, HOJE));

        assertEquals(valor("225.00"), conta.calcularSaldoDevedor());
        assertEquals(StatusDebito.ABERTO, venda102.getStatus());
    }

    @Test
    @DisplayName("Selecionar compra quitada ou seleção vazia é erro de uso")
    void selecaoInvalida() throws Exception {
        conta.pagarSelecionadas(List.of(venda102), valor("100"), FormaPagamento.PIX, HOJE);

        assertThrows(IllegalArgumentException.class, () -> conta.pagarSelecionadas(
                List.of(venda102), valor("1"), FormaPagamento.PIX, HOJE));
        assertThrows(IllegalArgumentException.class, () -> conta.pagarSelecionadas(
                List.of(), valor("1"), FormaPagamento.PIX, HOJE));
    }
}
