package br.com.ferragem.dominio.modelo.venda;

import br.com.ferragem.dominio.excecao.EstoqueInsuficienteException;
import br.com.ferragem.dominio.excecao.FiadoNaoPermitidoException;
import br.com.ferragem.dominio.excecao.OperacaoNaoAutorizadaException;
import br.com.ferragem.dominio.excecao.PrecoInvalidoException;
import br.com.ferragem.dominio.excecao.QuantidadeInvalidaException;
import br.com.ferragem.dominio.excecao.VendaVaziaException;
import br.com.ferragem.dominio.modelo.caixa.PerfilOperador;
import br.com.ferragem.dominio.modelo.cliente.Cliente;
import br.com.ferragem.dominio.modelo.produto.ProdutoKg;
import br.com.ferragem.dominio.modelo.produto.ProdutoMetro;
import br.com.ferragem.dominio.modelo.produto.ProdutoUnidade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes do carrinho e da finalização da {@link Venda} (RN01, RN04, RN05, RN06, RN13).
 */
class VendaTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 11, 20, 15, 30);

    private ProdutoUnidade parafuso;   // estoque 10, venda 0,25, custo 0,10
    private ProdutoKg prego;           // estoque 5 kg, venda 9,99/kg
    private ProdutoMetro corrente;     // estoque 20 m, venda 14,50/m
    private Venda venda;

    private static BigDecimal valor(String texto) {
        return new BigDecimal(texto);
    }

    @BeforeEach
    void prepararCarrinho() throws Exception {
        parafuso = new ProdutoUnidade("PAR-01", null, "Parafuso 6mm", valor("0.10"), valor("0.25"));
        parafuso.reporEstoque(valor("10"));
        prego = new ProdutoKg("PRE-01", null, "Prego 17x27", valor("6.00"), valor("9.99"));
        prego.reporEstoque(valor("5"));
        corrente = new ProdutoMetro("COR-01", null, "Corrente 5mm", valor("8.00"), valor("14.50"));
        corrente.reporEstoque(valor("20"));
        venda = new Venda();
    }

    // ---------------------------------------------------------------- carrinho

    @Test
    @DisplayName("Carrinho aceita produtos de tipos diferentes (polimorfismo) e soma o total")
    void carrinhoComTiposDiferentes() throws Exception {
        venda.adicionarItem(parafuso, valor("4"));      // 4 x 0,25  = 1,00
        venda.adicionarItem(prego, valor("0.500"));     // 0,5 x 9,99 = 4,995 -> 5,00
        venda.adicionarItem(corrente, valor("1.20"));   // 1,2 x 14,50 = 17,40

        assertEquals(3, venda.getItens().size());
        assertEquals(valor("23.40"), venda.calcularTotal());
    }

    @Test
    @DisplayName("CT-06 finalizar venda sem itens é recusado")
    void ct06_vendaVaziaNaoPodeSerFinalizada() {
        assertThrows(VendaVaziaException.class, () -> venda.finalizar(FormaPagamento.DINHEIRO, AGORA));
        assertEquals(StatusVenda.EM_ABERTO, venda.getStatus());
    }

    @Test
    @DisplayName("CT-07 mesmo produto adicionado duas vezes (2 + 3) vira uma linha com 5")
    void ct07_mesmoProdutoSomaNaMesmaLinha() throws Exception {
        venda.adicionarItem(parafuso, valor("2"));
        venda.adicionarItem(parafuso, valor("3"));

        assertEquals(1, venda.getItens().size());
        assertEquals(0, valor("5").compareTo(venda.getItens().get(0).getQuantidade()));
    }

    @Test
    @DisplayName("CT-08 subtotal 0,333 kg x R$ 9,99 = R$ 3,33 (arredondamento HALF_UP)")
    void ct08_subtotalArredondaParaCentavos() throws Exception {
        venda.adicionarItem(prego, valor("0.333"));

        assertEquals(valor("3.33"), venda.getItens().get(0).calcularSubtotal());
    }

    @Test
    @DisplayName("Quantidade inválida para o tipo do produto não entra no carrinho (RN06)")
    void quantidadeInvalidaNaoEntraNoCarrinho() {
        assertThrows(QuantidadeInvalidaException.class, () -> venda.adicionarItem(parafuso, valor("1.5")));
        assertTrue(venda.estaVazia());
    }

    @Test
    @DisplayName("Estoque é conferido pela soma do carrinho: 6 + 6 com estoque 10 é recusado e fica 6")
    void estoqueConsideraASomaDoCarrinho() throws Exception {
        venda.adicionarItem(parafuso, valor("6"));

        assertThrows(EstoqueInsuficienteException.class, () -> venda.adicionarItem(parafuso, valor("6")));
        assertEquals(0, valor("6").compareTo(venda.getItens().get(0).getQuantidade()));
    }

    @Test
    @DisplayName("Alterar quantidade respeita estoque e tipo do produto")
    void alterarQuantidadeRespeitaAsRegras() throws Exception {
        venda.adicionarItem(corrente, valor("1.00"));

        venda.alterarQuantidade("COR-01", valor("2.50"));
        assertEquals(valor("36.25"), venda.calcularTotal());

        assertThrows(EstoqueInsuficienteException.class, () -> venda.alterarQuantidade("COR-01", valor("25")));
        assertThrows(QuantidadeInvalidaException.class, () -> venda.alterarQuantidade("COR-01", valor("1.555")));
    }

    @Test
    @DisplayName("Remover item tira o produto do carrinho")
    void removerItem() throws Exception {
        venda.adicionarItem(parafuso, valor("1"));
        venda.adicionarItem(prego, valor("1"));

        venda.removerItem("PAR-01");

        assertEquals(1, venda.getItens().size());
        assertEquals("PRE-01", venda.getItens().get(0).getProduto().getCodigo());
    }

    @Test
    @DisplayName("A lista de itens devolvida não permite alterar o carrinho por fora")
    void listaDeItensEhSomenteLeitura() throws Exception {
        venda.adicionarItem(parafuso, valor("1"));
        List<ItemVenda> itens = venda.getItens();

        assertThrows(UnsupportedOperationException.class, itens::clear);
        assertEquals(1, venda.getItens().size());
    }

    @Test
    @DisplayName("Preço do item é copiado do produto: mudar o produto depois não muda a venda")
    void precoEhCopiadoNoMomentoDaVenda() throws Exception {
        venda.adicionarItem(parafuso, valor("4"));

        parafuso.alterarPrecos(valor("0.10"), valor("0.50"));

        assertEquals(valor("1.00"), venda.calcularTotal());
    }

    // ---------------------------------------------------------------- RN13 preço alterado

    @Test
    @DisplayName("Operador pode dar desconto desde que o preço não fique abaixo do custo")
    void operadorPodeDarDescontoAteOCusto() throws Exception {
        venda.adicionarItem(corrente, valor("2"));

        venda.alterarPrecoUnitario("COR-01", valor("12.00"), PerfilOperador.OPERADOR);

        assertEquals(valor("24.00"), venda.calcularTotal());
    }

    @Test
    @DisplayName("CT-23 preço abaixo do custo: operador é barrado, administrador consegue")
    void ct23_precoAbaixoDoCustoExigeAdministrador() throws Exception {
        venda.adicionarItem(corrente, valor("1"));

        assertThrows(OperacaoNaoAutorizadaException.class,
                () -> venda.alterarPrecoUnitario("COR-01", valor("7.00"), PerfilOperador.OPERADOR));
        assertEquals(valor("14.50"), venda.calcularTotal());

        venda.alterarPrecoUnitario("COR-01", valor("7.00"), PerfilOperador.ADMINISTRADOR);
        assertEquals(valor("7.00"), venda.calcularTotal());
    }

    @Test
    @DisplayName("Preço unitário zero é recusado mesmo para administrador")
    void precoZeroEhRecusado() throws Exception {
        venda.adicionarItem(parafuso, valor("1"));

        assertThrows(PrecoInvalidoException.class,
                () -> venda.alterarPrecoUnitario("PAR-01", valor("0"), PerfilOperador.ADMINISTRADOR));
    }

    // ---------------------------------------------------------------- RN01 fiado e finalização

    @Test
    @DisplayName("CT-09 venda FIADO sem cliente é recusada")
    void ct09_fiadoSemClienteEhRecusado() throws Exception {
        venda.adicionarItem(parafuso, valor("1"));

        assertThrows(FiadoNaoPermitidoException.class, () -> venda.finalizar(FormaPagamento.FIADO, AGORA));
        assertEquals(StatusVenda.EM_ABERTO, venda.getStatus());
    }

    @Test
    @DisplayName("Venda FIADO para cliente inativo é recusada")
    void fiadoParaClienteInativoEhRecusado() throws Exception {
        Cliente cliente = new Cliente("Seu Zé", null, null);
        cliente.inativar();
        venda.adicionarItem(parafuso, valor("1"));
        venda.identificarCliente(cliente);

        assertThrows(FiadoNaoPermitidoException.class, () -> venda.finalizar(FormaPagamento.FIADO, AGORA));
    }

    @Test
    @DisplayName("Venda FIADO com cliente ativo é finalizada")
    void fiadoComClienteAtivoEhFinalizado() throws Exception {
        venda.adicionarItem(prego, valor("2"));
        venda.identificarCliente(new Cliente("Seu Zé", null, null));

        venda.finalizar(FormaPagamento.FIADO, AGORA);

        assertEquals(StatusVenda.FINALIZADA, venda.getStatus());
        assertEquals(FormaPagamento.FIADO, venda.getFormaPagamento().orElseThrow());
        assertEquals(AGORA, venda.getDataHora().orElseThrow());
    }

    @Test
    @DisplayName("Venda sem cliente pode ser paga em dinheiro (qualquer pessoa compra)")
    void vendaSemClienteEmDinheiro() throws Exception {
        venda.adicionarItem(parafuso, valor("2"));

        venda.finalizar(FormaPagamento.DINHEIRO, AGORA);

        assertTrue(venda.getCliente().isEmpty());
        assertEquals(StatusVenda.FINALIZADA, venda.getStatus());
    }

    @Test
    @DisplayName("Venda finalizada não pode mais ser alterada")
    void vendaFinalizadaNaoPodeSerAlterada() throws Exception {
        venda.adicionarItem(parafuso, valor("1"));
        venda.finalizar(FormaPagamento.PIX, AGORA);

        assertThrows(IllegalStateException.class, () -> venda.adicionarItem(prego, valor("1")));
        assertThrows(IllegalStateException.class, () -> venda.removerItem("PAR-01"));
        assertThrows(IllegalStateException.class, () -> venda.finalizar(FormaPagamento.PIX, AGORA));
    }

    @Test
    @DisplayName("Finalizar a venda não baixa o estoque (isso é tarefa do caso de uso, na transação)")
    void finalizarNaoBaixaEstoque() throws Exception {
        venda.adicionarItem(parafuso, valor("3"));

        venda.finalizar(FormaPagamento.DINHEIRO, AGORA);

        assertEquals(0, valor("10").compareTo(parafuso.getQuantidadeEstoque()));
    }
}
