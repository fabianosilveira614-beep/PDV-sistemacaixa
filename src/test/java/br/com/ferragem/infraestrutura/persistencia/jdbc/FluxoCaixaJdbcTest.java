package br.com.ferragem.infraestrutura.persistencia.jdbc;

import br.com.ferragem.aplicacao.casodeuso.CadastrarCliente;
import br.com.ferragem.aplicacao.casodeuso.CadastrarProduto;
import br.com.ferragem.aplicacao.casodeuso.ConsultarHistoricoCliente;
import br.com.ferragem.aplicacao.casodeuso.ConsultarVenda;
import br.com.ferragem.aplicacao.casodeuso.ExcluirProduto;
import br.com.ferragem.aplicacao.casodeuso.ListarClientes;
import br.com.ferragem.aplicacao.casodeuso.PagarDebito;
import br.com.ferragem.aplicacao.casodeuso.RegistrarVenda;
import br.com.ferragem.aplicacao.porta.CaixaRepository;
import br.com.ferragem.dominio.excecao.CaixaJaAbertoException;
import br.com.ferragem.dominio.excecao.EstoqueInsuficienteException;
import br.com.ferragem.dominio.modelo.caixa.Operador;
import br.com.ferragem.dominio.modelo.caixa.PerfilOperador;
import br.com.ferragem.dominio.modelo.caixa.SessaoCaixa;
import br.com.ferragem.dominio.modelo.cliente.Cliente;
import br.com.ferragem.dominio.modelo.fiado.ContaFiado;
import br.com.ferragem.dominio.modelo.fiado.Debito;
import br.com.ferragem.dominio.modelo.fiado.StatusDebito;
import br.com.ferragem.dominio.modelo.produto.Produto;
import br.com.ferragem.dominio.modelo.produto.UnidadeMedida;
import br.com.ferragem.dominio.modelo.venda.FormaPagamento;
import br.com.ferragem.dominio.modelo.venda.Venda;
import br.com.ferragem.infraestrutura.persistencia.FabricaConexao;
import br.com.ferragem.infraestrutura.persistencia.PersistenciaException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Fluxos completos do caixa contra o PostgreSQL real: casos de uso de verdade, repositórios
 * JDBC de verdade, transação de verdade. Cobre o CT-26 (os dados sobrevivem ao fechamento do
 * programa) e o rollback (uma falha no meio da venda não deixa nada pela metade).
 */
class FluxoCaixaJdbcTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 11, 20);

    private FabricaConexao fabrica;
    private SistemaJdbc sistema;
    private Operador ana;
    private Produto parafuso;
    private Cliente seuZe;

    private static BigDecimal valor(String texto) {
        return new BigDecimal(texto);
    }

    @BeforeEach
    void preparar() throws Exception {
        fabrica = BancoDeTeste.limparEPreparar();
        sistema = new SistemaJdbc(fabrica);
        ana = new Operador("Ana", "ana", "hash", PerfilOperador.OPERADOR);
        sistema.operadores.salvar(ana);
        parafuso = sistema.cadastrarProduto().executar(new CadastrarProduto.Comando(UnidadeMedida.UNIDADE,
                "PAR-01", null, "Parafuso 6mm", valor("0.10"), valor("0.25"), valor("100")));
        seuZe = sistema.cadastrarCliente().executar(new CadastrarCliente.Comando("Seu Zé", null, null));
        sistema.abrirCaixa().executar(ana, valor("50.00"));
    }

    /** Simula fechar e abrir o programa: objetos novos, só o banco em comum. */
    private SistemaJdbc reabrir() {
        return new SistemaJdbc(fabrica);
    }

    private Venda carrinho(SistemaJdbc s, String quantidade) throws Exception {
        Venda venda = new Venda();
        venda.adicionarItem(s.produtos.buscarPorCodigo("PAR-01").orElseThrow(), valor(quantidade));
        return venda;
    }

    private long contar(String tabela) throws Exception {
        try (Connection conexao = fabrica.abrirConexao(); Statement comando = conexao.createStatement();
             ResultSet resultado = comando.executeQuery("SELECT count(*) FROM " + tabela)) {
            resultado.next();
            return resultado.getLong(1);
        }
    }

    @Test
    @DisplayName("CT-26: venda em dinheiro continua lá depois de reabrir o programa")
    void ct26_vendaPersisteAposReiniciar() throws Exception {
        RegistrarVenda.Comprovante comprovante = sistema.registrarVenda().executar(new RegistrarVenda.Comando(
                carrinho(sistema, "40"), FormaPagamento.DINHEIRO, valor("20.00"), ana));
        assertEquals(0, valor("10.00").compareTo(comprovante.troco()));

        SistemaJdbc depois = reabrir();
        Produto lido = depois.produtos.buscarPorCodigo("PAR-01").orElseThrow();
        assertEquals(0, valor("60").compareTo(lido.getQuantidadeEstoque()));
        SessaoCaixa caixa = depois.caixas.buscarAberta().orElseThrow();
        assertEquals(0, valor("60.00").compareTo(caixa.calcularSaldoEsperado()));
        assertEquals(1, caixa.getMovimentos().size());
        assertEquals(1L, contar("venda"));
        assertEquals(1L, contar("item_venda"));
        assertEquals(2L, contar("movimento_estoque"));
    }

    @Test
    @DisplayName("Rollback: se o caixa falhar depois do estoque baixado, nada da venda fica gravado")
    void rollbackQuandoFalhaNoMeioDaVenda() throws Exception {
        CaixaRepository caixaQueQuebra = new CaixaRepository() {
            @Override
            public Optional<SessaoCaixa> buscarAberta() {
                return sistema.caixas.buscarAberta();
            }

            @Override
            public void salvar(SessaoCaixa sessao) {
                throw new PersistenciaException("queda simulada da conexão", null);
            }
        };
        RegistrarVenda venda = sistema.registrarVendaCom(caixaQueQuebra);

        assertThrows(PersistenciaException.class, () -> venda.executar(new RegistrarVenda.Comando(
                carrinho(sistema, "40"), FormaPagamento.DINHEIRO, valor("20.00"), ana)));

        SistemaJdbc depois = reabrir();
        assertEquals(0, valor("100").compareTo(
                depois.produtos.buscarPorCodigo("PAR-01").orElseThrow().getQuantidadeEstoque()));
        assertEquals(0L, contar("venda"));
        assertEquals(0L, contar("item_venda"));
        assertEquals(1L, contar("movimento_estoque"));
        assertEquals(0, depois.caixas.buscarAberta().orElseThrow().getMovimentos().size());
    }

    @Test
    @DisplayName("Estoque relido dentro da transação: carrinho antigo não vende o que já saiu")
    void estoqueRelidoNaTransacao() throws Exception {
        Venda carrinhoAntigo = carrinho(sistema, "80");
        sistema.registrarVenda().executar(new RegistrarVenda.Comando(
                carrinho(sistema, "50"), FormaPagamento.PIX, null, ana));

        assertThrows(EstoqueInsuficienteException.class, () -> sistema.registrarVenda().executar(
                new RegistrarVenda.Comando(carrinhoAntigo, FormaPagamento.PIX, null, ana)));
        assertEquals(1L, contar("venda"));
    }

    @Test
    @DisplayName("Fiado: dívida gravada, pagamento parcial após reabrir, saldo correto e ligado ao caixa")
    void fiadoCompletoNoBanco() throws Exception {
        Venda venda = carrinho(sistema, "40");
        venda.identificarCliente(seuZe);
        long vendaId = sistema.registrarVenda().executar(
                new RegistrarVenda.Comando(venda, FormaPagamento.FIADO, null, ana)).vendaId();

        SistemaJdbc depois = reabrir();
        Cliente ze = depois.clientes.buscarPorId(seuZe.getId().orElseThrow()).orElseThrow();
        depois.pagarDebito().executar(new PagarDebito.Comando(ze, Set.of(vendaId), valor("4.00"),
                FormaPagamento.DINHEIRO, valor("5.00")));

        SistemaJdbc maisTarde = reabrir();
        List<Debito> debitos = maisTarde.debitos.buscarPorCliente(ze);
        assertEquals(1, debitos.size());
        assertEquals(StatusDebito.PARCIAL, debitos.get(0).getStatus());
        assertEquals(0, valor("6.00").compareTo(new ContaFiado(ze, debitos).calcularSaldoDevedor()));
        assertEquals(0, valor("54.00").compareTo(
                maisTarde.caixas.buscarAberta().orElseThrow().calcularSaldoEsperado()));
        assertEquals(1L, contar("pagamento_debito"));
    }

    @Test
    @DisplayName("Suprimento, sangria e fechamento sobrevivem; depois do fechamento abre-se outro caixa")
    void expedienteNoBanco() throws Exception {
        sistema.suprimento().executar(valor("30.00"), "troco extra");
        reabrir().sangria().executar(valor("70.00"), "depósito");
        assertEquals(0, valor("10.00").compareTo(
                reabrir().caixas.buscarAberta().orElseThrow().calcularSaldoEsperado()));

        reabrir().fecharCaixa().executar(ana, valor("10.00"));
        assertTrue(reabrir().caixas.buscarAberta().isEmpty());
        reabrir().abrirCaixa().executar(ana, valor("0.00"));
        assertEquals(2L, contar("sessao_caixa"));
    }

    @Test
    @DisplayName("CT-24 no banco: segundo caixa aberto é barrado pela regra e, se ela falhar, pelo índice único")
    void umCaixaAbertoDefesaEmProfundidade() throws Exception {
        assertThrows(CaixaJaAbertoException.class, () -> reabrir().abrirCaixa().executar(ana, valor("1.00")));

        SessaoCaixa intrusa = SessaoCaixa.abrir(ana, valor("1.00"), java.time.LocalDateTime.now());
        assertThrows(PersistenciaException.class, () -> sistema.caixas.salvar(intrusa));
    }

    @Test
    @DisplayName("Histórico mostra o preço cobrado na época, mesmo depois de o produto ser reajustado")
    void historicoComPrecoDaEpoca() throws Exception {
        Venda venda = carrinho(sistema, "4");
        venda.identificarCliente(seuZe);
        sistema.registrarVenda().executar(new RegistrarVenda.Comando(venda, FormaPagamento.PIX, null, ana));
        Produto reajustado = sistema.produtos.buscarPorCodigo("PAR-01").orElseThrow();
        reajustado.alterarPrecos(valor("0.20"), valor("0.50"));
        sistema.produtos.salvar(reajustado);

        SistemaJdbc depois = reabrir();
        Cliente ze = depois.clientes.buscarPorId(seuZe.getId().orElseThrow()).orElseThrow();
        List<Venda> historico = new ConsultarHistoricoCliente(depois.vendas).executar(ze, HOJE, HOJE);
        assertEquals(1, historico.size());
        assertEquals(0, valor("1.00").compareTo(historico.get(0).calcularTotal()));
        assertEquals(FormaPagamento.PIX, historico.get(0).getFormaPagamento().orElseThrow());
        assertTrue(depois.vendas.clienteTemVendas(ze));
    }

    @Test
    @DisplayName("Exclusão no banco: cadastro errado some com seus movimentos; produto vendido é só inativado")
    void exclusaoDeProdutoNoBanco() throws Exception {
        Produto errado = sistema.cadastrarProduto().executar(new CadastrarProduto.Comando(UnidadeMedida.UNIDADE,
                "ERRADO-01", null, "Cadastro errado", valor("1.00"), valor("2.00"), valor("10")));
        sistema.registrarVenda().executar(new RegistrarVenda.Comando(carrinho(sistema, "1"), FormaPagamento.PIX,
                null, ana));
        ExcluirProduto excluir = new ExcluirProduto(sistema.produtos, sistema.vendas, sistema.contexto);

        assertEquals(ExcluirProduto.Resultado.EXCLUIDO, excluir.executar(errado.getId().orElseThrow()));
        assertTrue(reabrir().produtos.buscarPorId(errado.getId().orElseThrow()).isEmpty());
        assertEquals(2L, contar("movimento_estoque"), "sobram só os movimentos do parafuso");

        assertEquals(ExcluirProduto.Resultado.INATIVADO, excluir.executar(parafuso.getId().orElseThrow()));
        assertTrue(reabrir().produtos.buscarPorId(parafuso.getId().orElseThrow()).isPresent());
        assertEquals(1L, contar("item_venda"), "a venda antiga continua íntegra");
    }

    @Test
    @DisplayName("Lista de clientes com total devido e consulta da venda fiado pelo número, no banco")
    void clientesComTotalEVendaPorNumero() throws Exception {
        Venda venda = carrinho(sistema, "40");
        venda.identificarCliente(seuZe);
        long vendaId = sistema.registrarVenda().executar(
                new RegistrarVenda.Comando(venda, FormaPagamento.FIADO, null, ana)).vendaId();

        SistemaJdbc depois = reabrir();
        List<ListarClientes.ResumoCliente> lista = new ListarClientes(depois.clientes, depois.debitos).executar("");
        assertEquals(1, lista.size());
        assertEquals(0, valor("10.00").compareTo(lista.get(0).totalDevido()));
        assertEquals(1, lista.get(0).comprasEmAberto());

        Venda lida = new ConsultarVenda(depois.vendas).executar(vendaId).orElseThrow();
        assertEquals("Seu Zé", lida.getCliente().orElseThrow().getNome());
        assertEquals(0, valor("40").compareTo(lida.getItens().get(0).getQuantidade()));
        assertEquals(FormaPagamento.FIADO, lida.getFormaPagamento().orElseThrow());
        assertTrue(new ConsultarVenda(depois.vendas).executar(vendaId + 1000).isEmpty());
    }
}
