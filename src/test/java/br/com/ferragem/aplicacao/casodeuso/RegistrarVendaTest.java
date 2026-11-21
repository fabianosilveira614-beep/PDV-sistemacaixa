package br.com.ferragem.aplicacao.casodeuso;

import br.com.ferragem.aplicacao.memoria.CaixaRepositoryEmMemoria;
import br.com.ferragem.aplicacao.memoria.DebitoRepositoryEmMemoria;
import br.com.ferragem.aplicacao.memoria.MovimentoEstoqueEmMemoria;
import br.com.ferragem.aplicacao.memoria.OperadoraCartaoFalsa;
import br.com.ferragem.aplicacao.memoria.ProdutoRepositoryEmMemoria;
import br.com.ferragem.aplicacao.memoria.RegistroVendasEmMemoria;
import br.com.ferragem.aplicacao.memoria.TransacaoEmMemoria;
import br.com.ferragem.aplicacao.pagamento.PagamentoCartao;
import br.com.ferragem.aplicacao.pagamento.PagamentoDinheiro;
import br.com.ferragem.aplicacao.pagamento.PagamentoFiado;
import br.com.ferragem.aplicacao.pagamento.PagamentoPix;
import br.com.ferragem.aplicacao.pagamento.ProcessadoresPagamento;
import br.com.ferragem.aplicacao.porta.RegistroVendas;
import br.com.ferragem.dominio.excecao.CaixaFechadoException;
import br.com.ferragem.dominio.excecao.EstoqueInsuficienteException;
import br.com.ferragem.dominio.excecao.FiadoNaoPermitidoException;
import br.com.ferragem.dominio.excecao.PagamentoInsuficienteException;
import br.com.ferragem.dominio.excecao.PagamentoRecusadoException;
import br.com.ferragem.dominio.excecao.ProdutoIndisponivelException;
import br.com.ferragem.dominio.modelo.caixa.Operador;
import br.com.ferragem.dominio.modelo.caixa.PerfilOperador;
import br.com.ferragem.dominio.modelo.caixa.SessaoCaixa;
import br.com.ferragem.dominio.modelo.cliente.Cliente;
import br.com.ferragem.dominio.modelo.fiado.Debito;
import br.com.ferragem.dominio.modelo.fiado.StatusDebito;
import br.com.ferragem.dominio.modelo.produto.MovimentoEstoque;
import br.com.ferragem.dominio.modelo.produto.ProdutoKg;
import br.com.ferragem.dominio.modelo.produto.ProdutoUnidade;
import br.com.ferragem.dominio.modelo.produto.TipoMovimentoEstoque;
import br.com.ferragem.dominio.modelo.venda.FormaPagamento;
import br.com.ferragem.dominio.modelo.venda.StatusVenda;
import br.com.ferragem.dominio.modelo.venda.Venda;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes do caso de uso de venda com repositórios em memória: nenhum banco, nenhuma tela.
 * Carrinho padrão: 4 parafusos (R$ 0,25) + 0,500 kg de prego (R$ 9,99/kg) = R$ 6,00.
 */
class RegistrarVendaTest {

    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    private static final Clock RELOGIO = Clock.fixed(Instant.parse("2026-11-20T18:00:00Z"), FUSO);

    private ProdutoRepositoryEmMemoria produtos;
    private MovimentoEstoqueEmMemoria movimentos;
    private RegistroVendasEmMemoria vendas;
    private DebitoRepositoryEmMemoria debitos;
    private CaixaRepositoryEmMemoria caixas;
    private TransacaoEmMemoria transacao;
    private OperadoraCartaoFalsa operadora;
    private RegistrarVenda registrarVenda;

    private Operador ana;
    private Cliente seuZe;
    private ProdutoUnidade parafuso;
    private ProdutoKg prego;
    private Venda venda;

    private static BigDecimal valor(String texto) {
        return new BigDecimal(texto);
    }

    @BeforeEach
    void preparar() throws Exception {
        produtos = new ProdutoRepositoryEmMemoria();
        movimentos = new MovimentoEstoqueEmMemoria();
        vendas = new RegistroVendasEmMemoria();
        debitos = new DebitoRepositoryEmMemoria();
        caixas = new CaixaRepositoryEmMemoria();
        transacao = new TransacaoEmMemoria();
        operadora = new OperadoraCartaoFalsa();
        ProcessadoresPagamento processadores = new ProcessadoresPagamento(List.of(
                new PagamentoDinheiro(), new PagamentoPix(), new PagamentoFiado(),
                new PagamentoCartao(FormaPagamento.CREDITO, operadora),
                new PagamentoCartao(FormaPagamento.DEBITO, operadora)));
        registrarVenda = new RegistrarVenda(produtos, movimentos, vendas, debitos, caixas, processadores,
                transacao, RELOGIO);

        ana = new Operador("Ana", "ana", "hash", PerfilOperador.OPERADOR);
        seuZe = new Cliente("Seu Zé", null, null);

        parafuso = new ProdutoUnidade("PAR-01", null, "Parafuso 6mm", valor("0.10"), valor("0.25"));
        parafuso.reporEstoque(valor("10"));
        prego = new ProdutoKg("PRE-01", null, "Prego 17x27", valor("6.00"), valor("9.99"));
        prego.reporEstoque(valor("5"));
        produtos.salvar(parafuso);
        produtos.salvar(prego);

        venda = new Venda();
        venda.adicionarItem(parafuso, valor("4"));
        venda.adicionarItem(prego, valor("0.500"));
    }

    private void abrirCaixaCom(String valorAbertura) throws Exception {
        new AbrirCaixa(caixas, RELOGIO).executar(ana, valor(valorAbertura));
    }

    private SessaoCaixa caixaAberto() {
        return caixas.buscarAberta().orElseThrow();
    }

    /** Confere que uma venda recusada não deixou rastro em lugar nenhum. */
    private void assertNadaFoiAlterado() {
        assertEquals(StatusVenda.EM_ABERTO, venda.getStatus());
        assertEquals(0, valor("10").compareTo(parafuso.getQuantidadeEstoque()));
        assertTrue(vendas.getRegistros().isEmpty());
        assertTrue(movimentos.getMovimentos().isEmpty());
        assertTrue(debitos.getTodos().isEmpty());
    }

    // ---------------------------------------------------------------- caminho feliz

    @Test
    @DisplayName("CT-11 e CT-14 venda em dinheiro: grava, baixa estoque, registra no caixa e calcula o troco")
    void ct11_ct14_vendaEmDinheiro() throws Exception {
        abrirCaixaCom("100.00");

        RegistrarVenda.Comprovante comprovante = registrarVenda.executar(
                new RegistrarVenda.Comando(venda, FormaPagamento.DINHEIRO, valor("10.00"), ana));

        assertEquals(1L, comprovante.vendaId());
        assertEquals(valor("6.00"), comprovante.total());
        assertEquals(valor("4.00"), comprovante.troco());
        assertEquals(StatusVenda.FINALIZADA, vendas.getRegistros().get(0).venda().getStatus());
        assertEquals(StatusVenda.EM_ABERTO, venda.getStatus(), "o carrinho da tela não é alterado");

        assertEquals(0, valor("6").compareTo(parafuso.getQuantidadeEstoque()));
        assertEquals(0, valor("4.5").compareTo(prego.getQuantidadeEstoque()));

        List<MovimentoEstoque> saidas = movimentos.getMovimentos();
        assertEquals(2, saidas.size());
        assertEquals(TipoMovimentoEstoque.SAIDA_VENDA, saidas.get(0).tipo());
        assertEquals(0, valor("-4").compareTo(saidas.get(0).quantidade()));
        assertEquals(1L, saidas.get(0).getVendaId().orElseThrow());

        assertEquals(valor("106.00"), caixaAberto().calcularSaldoEsperado());
        assertTrue(debitos.getTodos().isEmpty());
        assertEquals(1, transacao.getExecucoes());
    }

    @Test
    @DisplayName("CT-10 venda fiado: cria a dívida do cliente e não entra dinheiro no caixa")
    void ct10_vendaFiadoCriaDivida() throws Exception {
        abrirCaixaCom("100.00");
        venda.identificarCliente(seuZe);

        RegistrarVenda.Comprovante comprovante = registrarVenda.executar(
                new RegistrarVenda.Comando(venda, FormaPagamento.FIADO, null, ana));

        Debito divida = debitos.getTodos().get(0);
        assertEquals(StatusDebito.ABERTO, divida.getStatus());
        assertEquals(valor("6.00"), divida.getValorOriginal());
        assertEquals(comprovante.vendaId(), divida.getVendaId());
        assertEquals(valor("0.00"), comprovante.valorRecebido());
        assertEquals(valor("100.00"), caixaAberto().calcularSaldoEsperado());
        assertTrue(caixaAberto().getMovimentos().isEmpty());
    }

    @Test
    @DisplayName("Venda no cartão aprovada guarda o código da operadora e não mexe na gaveta")
    void vendaNoCartaoAprovada() throws Exception {
        abrirCaixaCom("100.00");

        RegistrarVenda.Comprovante comprovante = registrarVenda.executar(
                new RegistrarVenda.Comando(venda, FormaPagamento.CREDITO, null, ana));

        assertEquals("AUT-1", comprovante.codigoAutorizacao());
        assertEquals(valor("100.00"), caixaAberto().calcularSaldoEsperado());
        assertEquals(valor("6.00"), caixaAberto().calcularRecebidoPorForma().get(FormaPagamento.CREDITO));
    }

    @Test
    @DisplayName("Venda no Pix: sem troco, registrada no caixa como Pix")
    void vendaNoPix() throws Exception {
        abrirCaixaCom("0");

        RegistrarVenda.Comprovante comprovante = registrarVenda.executar(
                new RegistrarVenda.Comando(venda, FormaPagamento.PIX, null, ana));

        assertEquals(valor("0.00"), comprovante.troco());
        assertEquals(valor("6.00"), caixaAberto().calcularRecebidoPorForma().get(FormaPagamento.PIX));
    }

    // ---------------------------------------------------------------- recusas: nada pode mudar

    @Test
    @DisplayName("CT-13 vender sem caixa aberto é recusado e nada é alterado")
    void ct13_semCaixaAberto() {
        assertThrows(CaixaFechadoException.class, () -> registrarVenda.executar(
                new RegistrarVenda.Comando(venda, FormaPagamento.DINHEIRO, valor("10"), ana)));

        assertNadaFoiAlterado();
    }

    @Test
    @DisplayName("CT-12 estoque acabou enquanto o cliente estava no balcão: nada é gravado, nem o item que tinha estoque")
    void ct12_estoqueMudouAntesDeConfirmar() throws Exception {
        abrirCaixaCom("100.00");
        prego.baixarEstoque(valor("4.800"));   // outra venda levou o prego: sobraram 0,200 kg

        assertThrows(EstoqueInsuficienteException.class, () -> registrarVenda.executar(
                new RegistrarVenda.Comando(venda, FormaPagamento.DINHEIRO, valor("10"), ana)));

        assertNadaFoiAlterado();
        assertEquals(valor("100.00"), caixaAberto().calcularSaldoEsperado());
    }

    @Test
    @DisplayName("CT-15 dinheiro entregue menor que o total é recusado e nada é alterado")
    void ct15_dinheiroInsuficiente() throws Exception {
        abrirCaixaCom("100.00");

        assertThrows(PagamentoInsuficienteException.class, () -> registrarVenda.executar(
                new RegistrarVenda.Comando(venda, FormaPagamento.DINHEIRO, valor("5.99"), ana)));

        assertNadaFoiAlterado();
    }

    @Test
    @DisplayName("Cartão recusado: nada é alterado e a venda pode ser paga de outro jeito")
    void cartaoRecusado() throws Exception {
        abrirCaixaCom("100.00");
        operadora.passarARecusar();

        assertThrows(PagamentoRecusadoException.class, () -> registrarVenda.executar(
                new RegistrarVenda.Comando(venda, FormaPagamento.DEBITO, null, ana)));
        assertNadaFoiAlterado();

        registrarVenda.executar(new RegistrarVenda.Comando(venda, FormaPagamento.PIX, null, ana));
        assertEquals(1, vendas.getRegistros().size());
        assertEquals(FormaPagamento.PIX, vendas.getRegistros().get(0).venda().getFormaPagamento().orElseThrow());
    }

    @Test
    @DisplayName("Queda do banco no meio da gravação: o carrinho continua em aberto e a venda é refeita")
    void falhaTecnicaNaoTravaOCarrinho() throws Exception {
        abrirCaixaCom("100.00");
        RegistroVendas bancoQueCai = (vendaGravada, sessao, operador, pagamento) -> {
            throw new IllegalStateException("conexão perdida");
        };
        RegistrarVenda comFalha = new RegistrarVenda(produtos, movimentos, bancoQueCai, debitos, caixas,
                new ProcessadoresPagamento(List.of(new PagamentoPix())), transacao, RELOGIO);

        assertThrows(IllegalStateException.class, () -> comFalha.executar(
                new RegistrarVenda.Comando(venda, FormaPagamento.PIX, null, ana)));
        assertEquals(StatusVenda.EM_ABERTO, venda.getStatus());
        assertEquals(2, venda.getItens().size());

        RegistrarVenda.Comprovante comprovante = registrarVenda.executar(
                new RegistrarVenda.Comando(venda, FormaPagamento.PIX, null, ana));
        assertEquals(valor("6.00"), comprovante.total());
    }

    @Test
    @DisplayName("Estoque esgotado: a operadora do cartão nem é chamada (não se cobra o que não se pode vender)")
    void naoCobraCartaoSemEstoque() throws Exception {
        abrirCaixaCom("100.00");
        parafuso.baixarEstoque(valor("10"));

        assertThrows(EstoqueInsuficienteException.class, () -> registrarVenda.executar(
                new RegistrarVenda.Comando(venda, FormaPagamento.CREDITO, null, ana)));

        assertEquals(0, operadora.getCobrancas());
    }

    @Test
    @DisplayName("Fiado sem cliente é recusado antes de abrir transação")
    void fiadoSemClienteNemAbreTransacao() throws Exception {
        abrirCaixaCom("100.00");

        assertThrows(FiadoNaoPermitidoException.class, () -> registrarVenda.executar(
                new RegistrarVenda.Comando(venda, FormaPagamento.FIADO, null, ana)));

        assertEquals(0, transacao.getExecucoes());
        assertNadaFoiAlterado();
    }

    @Test
    @DisplayName("Produto retirado de venda depois de entrar no carrinho: mensagem de negócio e nada gravado")
    void produtoInativadoNoMeioDaVenda() throws Exception {
        abrirCaixaCom("100.00");
        prego.inativar();

        ProdutoIndisponivelException erro = assertThrows(ProdutoIndisponivelException.class,
                () -> registrarVenda.executar(new RegistrarVenda.Comando(venda, FormaPagamento.PIX, null, ana)));
        assertTrue(erro.getMessage().contains("Prego 17x27"));
        assertNadaFoiAlterado();
    }

    @Test
    @DisplayName("O momento da venda vem do relógio injetado")
    void momentoVemDoRelogio() throws Exception {
        abrirCaixaCom("100.00");

        registrarVenda.executar(new RegistrarVenda.Comando(venda, FormaPagamento.PIX, null, ana));

        assertEquals(LocalDateTime.of(2026, 11, 20, 15, 0),
                vendas.getRegistros().get(0).venda().getDataHora().orElseThrow());
    }
}
