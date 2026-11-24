package br.com.ferragem.aplicacao.casodeuso;

import br.com.ferragem.aplicacao.memoria.DebitoRepositoryEmMemoria;
import br.com.ferragem.aplicacao.memoria.ProdutoRepositoryEmMemoria;
import br.com.ferragem.aplicacao.memoria.RegistroVendasEmMemoria;
import br.com.ferragem.aplicacao.pagamento.ResultadoPagamento;
import br.com.ferragem.aplicacao.porta.HistoricoVendas.VendaDoPeriodo;
import br.com.ferragem.dominio.excecao.PeriodoInvalidoException;
import br.com.ferragem.dominio.modelo.caixa.Operador;
import br.com.ferragem.dominio.modelo.caixa.PerfilOperador;
import br.com.ferragem.dominio.modelo.caixa.SessaoCaixa;
import br.com.ferragem.dominio.modelo.cliente.Cliente;
import br.com.ferragem.dominio.modelo.fiado.Debito;
import br.com.ferragem.dominio.modelo.produto.Produto;
import br.com.ferragem.dominio.modelo.produto.ProdutoUnidade;
import br.com.ferragem.dominio.modelo.venda.FormaPagamento;
import br.com.ferragem.dominio.modelo.venda.Venda;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Testes dos três relatórios: vendas do período, fiado em aberto e estoque baixo. */
class RelatorioCasosDeUsoTest {

    private static final LocalDate INICIO = LocalDate.of(2026, 11, 5);
    private static final LocalDate FIM = LocalDate.of(2026, 11, 15);

    private RegistroVendasEmMemoria vendas;
    private DebitoRepositoryEmMemoria debitos;
    private Operador ana;
    private Cliente ze;

    private static BigDecimal valor(String texto) {
        return new BigDecimal(texto);
    }

    private static LocalDateTime dia(int dia) {
        return LocalDateTime.of(2026, 11, dia, 10, 0);
    }

    /** Martelo: custo R$ 20,00, venda R$ 35,00 (lucro de R$ 15,00 por unidade). */
    private static Produto martelo() throws Exception {
        ProdutoUnidade martelo = new ProdutoUnidade("MAR-1", null, "Martelo", valor("20"), valor("35"));
        martelo.reporEstoque(valor("100"));
        return martelo;
    }

    @BeforeEach
    void preparar() throws Exception {
        vendas = new RegistroVendasEmMemoria();
        debitos = new DebitoRepositoryEmMemoria();
        ana = new Operador("Ana", "ana", "hash", PerfilOperador.OPERADOR);
        ze = new Cliente("Seu Zé", null, null);
    }

    private Venda vender(Produto produto, String quantidade, FormaPagamento forma, LocalDateTime quando)
            throws Exception {
        Venda venda = new Venda();
        venda.adicionarItem(produto, valor(quantidade));
        if (forma == FormaPagamento.FIADO) {
            venda.identificarCliente(ze);
        }
        venda.finalizar(forma, quando);
        vendas.salvar(venda, SessaoCaixa.abrir(ana, valor("0"), quando), ana,
                new ResultadoPagamento(venda.calcularTotal(), valor("0"), ""));
        return venda;
    }

    // ---------------------------------------------------------------- vendas do período

    @Test
    @DisplayName("Vendas do período: total, formas, fiado recebido, o que entrou no caixa, lucro e ranking")
    void resumoDoPeriodo() throws Exception {
        Produto martelo = martelo();
        vender(martelo, "2", FormaPagamento.PIX, dia(10));              // R$ 70, lucro 30
        Venda fiado = vender(martelo, "1", FormaPagamento.FIADO, dia(12)); // R$ 35, lucro 15
        vender(martelo, "5", FormaPagamento.DINHEIRO, dia(20));         // fora do período
        Debito antigo = new Debito(ze, 99, valor("50"), dia(1));        // compra de antes do período...
        antigo.registrarPagamento(valor("20"), FormaPagamento.DINHEIRO, dia(14)); // ...paga dentro dele
        debitos.salvar(antigo);
        debitos.salvar(Debito.daVendaFiado(fiado));

        ResumoVendas resumo = new RelatorioVendas(vendas, debitos).executar(INICIO, FIM);

        assertEquals(2, resumo.getQuantidadeVendas());
        assertEquals(valor("105.00"), resumo.getTotalVendido());
        assertEquals(valor("52.50"), resumo.getTicketMedio());
        assertEquals(valor("70.00"), resumo.getTotalPorForma(FormaPagamento.PIX));
        assertEquals(valor("35.00"), resumo.getTotalPorForma(FormaPagamento.FIADO));
        assertEquals(0, resumo.getTotalPorForma(FormaPagamento.DINHEIRO).signum());
        assertEquals(valor("20.00"), resumo.getRecebidoDeFiado());
        // entrou de fato: 105 vendidos − 35 no fiado (ainda não pagos) + 20 de fiado recebido
        assertEquals(valor("90.00"), resumo.getEntrouNoCaixa());
        assertEquals(valor("45.00"), resumo.getLucroBruto());
        assertEquals(valor("42.86"), resumo.getMargemBruta().orElseThrow()); // 45 ÷ 105
        assertEquals(1, resumo.getMaisVendidos().size());
        assertEquals(valor("3"), resumo.getMaisVendidos().get(0).quantidade());
        assertEquals("Ana", resumo.getVendas().get(0).operador());
    }

    @Test
    @DisplayName("Lucro usa o custo do dia da venda; venda antiga sem custo fica fora do lucro, mas no total")
    void lucroComCustoDaEpocaESemCusto() throws Exception {
        Produto martelo = martelo();
        Venda nova = new Venda();
        nova.adicionarItem(martelo, valor("1"));  // custo gravado: R$ 20,00
        nova.finalizar(FormaPagamento.PIX, dia(10));
        martelo.alterarPrecos(valor("30"), valor("35")); // custo sobe DEPOIS da venda
        Venda antiga = Venda.restaurar(7, null, FormaPagamento.DINHEIRO, dia(6),
                List.of(new Venda.ItemRegistrado(martelo, valor("2"), valor("25"))));  // sem custo

        ResumoVendas resumo = ResumoVendas.calcular(INICIO, FIM,
                List.of(new VendaDoPeriodo(nova, "Ana"), new VendaDoPeriodo(antiga, "Ana")), List.of());

        assertEquals(valor("85.00"), resumo.getTotalVendido());
        assertEquals(valor("15.00"), resumo.getLucroBruto());          // 35 − 20, não 35 − 30
        assertEquals(valor("35.00"), resumo.getVendidoComCusto());
        assertEquals(valor("50.00"), resumo.getVendidoSemCusto());
    }

    @Test
    @DisplayName("Período sem vendas: tudo zero, sem divisão por zero; período invertido é recusado")
    void periodoVazioEInvertido() throws Exception {
        ResumoVendas vazio = new RelatorioVendas(vendas, debitos).executar(INICIO, FIM);

        assertEquals(0, vazio.getQuantidadeVendas());
        assertEquals(0, vazio.getTicketMedio().signum());
        assertTrue(vazio.getMargemBruta().isEmpty());
        assertTrue(vazio.getMaisVendidos().isEmpty());
        assertThrows(PeriodoInvalidoException.class, () -> new RelatorioVendas(vendas, debitos).executar(FIM, INICIO));
        assertThrows(PeriodoInvalidoException.class, () -> new RelatorioFiadoEmAberto(debitos,
                Clock.systemDefaultZone()).executar(FIM, INICIO));
    }

    @Test
    @DisplayName("Ranking: até 10 produtos, do maior valor vendido para o menor")
    void rankingLimitadoEOrdenado() throws Exception {
        for (int i = 1; i <= 12; i++) {
            ProdutoUnidade produto = new ProdutoUnidade("P-" + i, null, "Produto " + i, valor("1"), valor("2"));
            produto.reporEstoque(valor("100"));
            vender(produto, String.valueOf(i), FormaPagamento.PIX, dia(10));
        }

        List<ResumoVendas.ProdutoVendido> ranking = new RelatorioVendas(vendas, debitos)
                .executar(INICIO, FIM).getMaisVendidos();

        assertEquals(ResumoVendas.TAMANHO_RANKING, ranking.size());
        assertEquals("Produto 12", ranking.get(0).produto().getNome());
        assertEquals("Produto 3", ranking.get(9).produto().getNome());
    }

    // ---------------------------------------------------------------- fiado em aberto

    @Test
    @DisplayName("Fiado em aberto: só compras do período ainda não quitadas, a mais antiga primeiro, com os dias")
    void fiadoEmAberto() throws Exception {
        debitos.salvar(new Debito(ze, 1, valor("100"), dia(1)));        // fora do período
        Debito parcial = new Debito(ze, 3, valor("100"), dia(8));
        parcial.registrarPagamento(valor("40"), FormaPagamento.PIX, dia(9));
        debitos.salvar(parcial);
        debitos.salvar(new Debito(ze, 2, valor("50"), dia(5)));
        Debito quitado = new Debito(ze, 4, valor("30"), dia(10));
        quitado.registrarPagamento(valor("30"), FormaPagamento.PIX, dia(11));
        debitos.salvar(quitado);
        Clock hoje = Clock.fixed(dia(21).atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());

        RelatorioFiadoEmAberto.Resultado resultado = new RelatorioFiadoEmAberto(debitos, hoje).executar(INICIO, FIM);

        assertEquals(List.of(2L, 3L), resultado.linhas().stream().map(l -> l.debito().getVendaId()).toList());
        assertEquals(16L, resultado.linhas().get(0).diasEmAberto());
        assertEquals(0, valor("110").compareTo(resultado.totalDevido())); // 50 + (100 − 40)
    }

    // ---------------------------------------------------------------- estoque baixo

    @Test
    @DisplayName("Estoque baixo: ativos no mínimo ou abaixo, com quanto falta; inativo e sem mínimo ficam fora")
    void estoqueBaixo() throws Exception {
        ProdutoRepositoryEmMemoria produtos = new ProdutoRepositoryEmMemoria();
        produtos.salvar(comEstoque("A-1", "Abaixo", "3", "5"));
        produtos.salvar(comEstoque("N-1", "No mínimo", "5", "5"));
        produtos.salvar(comEstoque("F-1", "Folgado", "10", "5"));
        produtos.salvar(comEstoque("S-1", "Sem mínimo", "0", null));
        Produto inativo = comEstoque("I-1", "Inativo", "0", "5");
        inativo.inativar();
        produtos.salvar(inativo);

        List<RelatorioEstoqueBaixo.Linha> linhas = new RelatorioEstoqueBaixo(produtos).executar();

        assertEquals(List.of("Abaixo", "No mínimo"), linhas.stream().map(l -> l.produto().getNome()).toList());
        assertEquals(valor("2"), linhas.get(0).falta());
        assertEquals(0, linhas.get(1).falta().signum());
    }

    private static Produto comEstoque(String codigo, String nome, String estoque, String minimo) throws Exception {
        ProdutoUnidade produto = new ProdutoUnidade(codigo, null, nome, valor("1"), valor("2"));
        if (valor(estoque).signum() > 0) {
            produto.reporEstoque(valor(estoque));
        }
        produto.definirEstoqueMinimo(minimo == null ? null : valor(minimo));
        return produto;
    }
}
