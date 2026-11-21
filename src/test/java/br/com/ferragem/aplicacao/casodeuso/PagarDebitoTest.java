package br.com.ferragem.aplicacao.casodeuso;

import br.com.ferragem.aplicacao.memoria.CaixaRepositoryEmMemoria;
import br.com.ferragem.aplicacao.memoria.DebitoRepositoryEmMemoria;
import br.com.ferragem.aplicacao.memoria.OperadoraCartaoFalsa;
import br.com.ferragem.aplicacao.memoria.TransacaoEmMemoria;
import br.com.ferragem.aplicacao.pagamento.PagamentoCartao;
import br.com.ferragem.aplicacao.pagamento.PagamentoDinheiro;
import br.com.ferragem.aplicacao.pagamento.PagamentoFiado;
import br.com.ferragem.aplicacao.pagamento.PagamentoPix;
import br.com.ferragem.aplicacao.pagamento.ProcessadoresPagamento;
import br.com.ferragem.dominio.excecao.CaixaFechadoException;
import br.com.ferragem.dominio.excecao.PagamentoInsuficienteException;
import br.com.ferragem.dominio.excecao.ValorPagamentoInvalidoException;
import br.com.ferragem.dominio.modelo.caixa.Operador;
import br.com.ferragem.dominio.modelo.caixa.PerfilOperador;
import br.com.ferragem.dominio.modelo.cliente.Cliente;
import br.com.ferragem.dominio.modelo.fiado.Debito;
import br.com.ferragem.dominio.modelo.fiado.StatusDebito;
import br.com.ferragem.dominio.modelo.venda.FormaPagamento;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Testes do recebimento de fiado. Cenário: Seu Zé deve as vendas #102 (R$ 100), #131 (R$ 80)
 * e #158 (R$ 45), total R$ 225; caixa aberto com R$ 100.
 */
class PagarDebitoTest {

    private static final Clock RELOGIO =
            Clock.fixed(Instant.parse("2026-11-20T18:00:00Z"), ZoneId.of("America/Sao_Paulo"));

    private DebitoRepositoryEmMemoria debitos;
    private CaixaRepositoryEmMemoria caixas;
    private OperadoraCartaoFalsa operadora;
    private PagarDebito pagarDebito;
    private Cliente seuZe;
    private Debito venda102;
    private Debito venda131;
    private Debito venda158;

    private static BigDecimal valor(String texto) {
        return new BigDecimal(texto);
    }

    @BeforeEach
    void preparar() throws Exception {
        debitos = new DebitoRepositoryEmMemoria();
        caixas = new CaixaRepositoryEmMemoria();
        operadora = new OperadoraCartaoFalsa();
        ProcessadoresPagamento processadores = new ProcessadoresPagamento(List.of(
                new PagamentoDinheiro(), new PagamentoPix(), new PagamentoFiado(),
                new PagamentoCartao(FormaPagamento.CREDITO, operadora),
                new PagamentoCartao(FormaPagamento.DEBITO, operadora)));
        pagarDebito = new PagarDebito(debitos, caixas, processadores, new TransacaoEmMemoria(), RELOGIO);

        seuZe = new Cliente("Seu Zé", null, null);
        venda102 = new Debito(seuZe, 102, valor("100.00"), LocalDateTime.of(2026, 11, 5, 9, 0));
        venda131 = new Debito(seuZe, 131, valor("80.00"), LocalDateTime.of(2026, 11, 12, 9, 0));
        venda158 = new Debito(seuZe, 158, valor("45.00"), LocalDateTime.of(2026, 11, 18, 9, 0));
        debitos.salvar(venda102);
        debitos.salvar(venda131);
        debitos.salvar(venda158);

        Operador ana = new Operador("Ana", "ana", "hash", PerfilOperador.OPERADOR);
        new AbrirCaixa(caixas, RELOGIO).executar(ana, valor("100.00"));
    }

    private BigDecimal gaveta() {
        return caixas.buscarAberta().orElseThrow().calcularSaldoEsperado();
    }

    @Test
    @DisplayName("Selecionadas #102 e #131, R$ 150 em dinheiro com nota de 200: troco 50, #102 quitada, #131 saldo 30")
    void pagarSelecionadasEmDinheiro() throws Exception {
        PagarDebito.Comprovante comprovante = pagarDebito.executar(new PagarDebito.Comando(
                seuZe, Set.of(102L, 131L), valor("150.00"), FormaPagamento.DINHEIRO, valor("200.00")));

        assertEquals(StatusDebito.QUITADO, venda102.getStatus());
        assertEquals(valor("30.00"), venda131.calcularSaldo());
        assertEquals(StatusDebito.ABERTO, venda158.getStatus());
        assertEquals(valor("50.00"), comprovante.troco());
        assertEquals(valor("75.00"), comprovante.saldoDevedorFinal());
        assertEquals(2, comprovante.aplicados().size());
        assertEquals(valor("250.00"), gaveta());
    }

    @Test
    @DisplayName("Valor livre (sem seleção) no Pix: abate da mais antiga; a gaveta não muda")
    void valorLivreNoPix() throws Exception {
        pagarDebito.executar(new PagarDebito.Comando(
                seuZe, Set.of(), valor("150"), FormaPagamento.PIX, null));

        assertEquals(StatusDebito.QUITADO, venda102.getStatus());
        assertEquals(valor("30.00"), venda131.calcularSaldo());
        assertEquals(valor("100.00"), gaveta());
        assertEquals(valor("150.00"), caixas.buscarAberta().orElseThrow()
                .calcularRecebidoPorForma().get(FormaPagamento.PIX));
    }

    @Test
    @DisplayName("Valor maior que o selecionado: recusado e o cartão NEM é cobrado")
    void valorMaiorQueOSelecionadoNaoCobraCartao() {
        assertThrows(ValorPagamentoInvalidoException.class, () -> pagarDebito.executar(new PagarDebito.Comando(
                seuZe, Set.of(131L), valor("80.01"), FormaPagamento.CREDITO, null)));

        assertEquals(0, operadora.getCobrancas());
        assertEquals(valor("80.00"), venda131.calcularSaldo());
    }

    @Test
    @DisplayName("Dinheiro entregue menor que o valor: recusado e nenhuma compra muda")
    void dinheiroInsuficiente() {
        assertThrows(PagamentoInsuficienteException.class, () -> pagarDebito.executar(new PagarDebito.Comando(
                seuZe, Set.of(102L), valor("100"), FormaPagamento.DINHEIRO, valor("50"))));

        assertEquals(StatusDebito.ABERTO, venda102.getStatus());
        assertEquals(valor("100.00"), gaveta());
    }

    @Test
    @DisplayName("Sem caixa aberto não se recebe fiado (RN08)")
    void semCaixaAberto() throws Exception {
        new FecharCaixa(caixas, RELOGIO).executar(
                new Operador("Ana", "ana", "hash", PerfilOperador.OPERADOR), valor("100"));

        assertThrows(CaixaFechadoException.class, () -> pagarDebito.executar(new PagarDebito.Comando(
                seuZe, Set.of(102L), valor("10"), FormaPagamento.PIX, null)));
        assertEquals(StatusDebito.ABERTO, venda102.getStatus());
    }

    @Test
    @DisplayName("Venda que não é fiado deste cliente e pagamento com fiado são erros de uso")
    void errosDeUso() {
        assertThrows(IllegalArgumentException.class, () -> pagarDebito.executar(new PagarDebito.Comando(
                seuZe, Set.of(999L), valor("10"), FormaPagamento.PIX, null)));
        assertThrows(IllegalArgumentException.class, () -> new PagarDebito.Comando(
                seuZe, Set.of(), valor("10"), FormaPagamento.FIADO, null));
    }

    @Test
    @DisplayName("Consulta da conta mostra o total devido atualizado")
    void consultaConta() throws Exception {
        pagarDebito.executar(new PagarDebito.Comando(seuZe, Set.of(158L), valor("45"), FormaPagamento.PIX, null));

        assertEquals(valor("180.00"), new ConsultarContaFiado(debitos).executar(seuZe).calcularSaldoDevedor());
    }
}
