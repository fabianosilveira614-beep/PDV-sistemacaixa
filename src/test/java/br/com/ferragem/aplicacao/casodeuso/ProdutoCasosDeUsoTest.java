package br.com.ferragem.aplicacao.casodeuso;

import br.com.ferragem.aplicacao.memoria.MovimentoEstoqueEmMemoria;
import br.com.ferragem.aplicacao.memoria.ProdutoRepositoryEmMemoria;
import br.com.ferragem.aplicacao.memoria.RegistroVendasEmMemoria;
import br.com.ferragem.aplicacao.memoria.TransacaoEmMemoria;
import br.com.ferragem.dominio.excecao.EstoqueInsuficienteException;
import br.com.ferragem.dominio.excecao.PrecoInvalidoException;
import br.com.ferragem.dominio.excecao.QuantidadeInvalidaException;
import br.com.ferragem.dominio.excecao.RegistroDuplicadoException;
import br.com.ferragem.dominio.modelo.produto.MovimentoEstoque;
import br.com.ferragem.dominio.modelo.produto.Produto;
import br.com.ferragem.dominio.modelo.produto.ProdutoKg;
import br.com.ferragem.dominio.modelo.produto.TipoMovimentoEstoque;
import br.com.ferragem.dominio.modelo.produto.UnidadeMedida;
import br.com.ferragem.dominio.modelo.venda.FormaPagamento;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Testes de cadastro, edição, inativação, busca e ajuste de estoque de produtos. */
class ProdutoCasosDeUsoTest {

    private static final Clock RELOGIO =
            Clock.fixed(Instant.parse("2026-11-20T12:00:00Z"), ZoneId.of("America/Sao_Paulo"));

    private ProdutoRepositoryEmMemoria produtos;
    private MovimentoEstoqueEmMemoria movimentos;
    private CadastrarProduto cadastrar;
    private AjustarEstoque ajustar;

    private static BigDecimal valor(String texto) {
        return new BigDecimal(texto);
    }

    private static CadastrarProduto.Comando prego(String codigo, String barras, String estoque) {
        return new CadastrarProduto.Comando(UnidadeMedida.KG, codigo, barras, "Prego 17x27",
                valor("12.00"), valor("19.90"), estoque == null ? null : valor(estoque));
    }

    @BeforeEach
    void preparar() {
        produtos = new ProdutoRepositoryEmMemoria();
        movimentos = new MovimentoEstoqueEmMemoria();
        TransacaoEmMemoria transacao = new TransacaoEmMemoria();
        cadastrar = new CadastrarProduto(produtos, movimentos, transacao, RELOGIO);
        ajustar = new AjustarEstoque(produtos, movimentos, transacao, RELOGIO);
    }

    // ---------------------------------------------------------------- cadastro

    @Test
    @DisplayName("Cadastro cria o tipo certo pela unidade e registra o estoque inicial como entrada")
    void cadastroComEstoqueInicial() throws Exception {
        Produto produto = cadastrar.executar(prego("PRE-01", "7891234567895", "25.500"));

        assertInstanceOf(ProdutoKg.class, produto);
        assertTrue(produto.getId().isPresent());
        assertEquals(0, valor("25.5").compareTo(produto.getQuantidadeEstoque()));
        MovimentoEstoque entrada = movimentos.getMovimentos().get(0);
        assertEquals(TipoMovimentoEstoque.ENTRADA_COMPRA, entrada.tipo());
        assertEquals("estoque inicial", entrada.motivo());
    }

    @Test
    @DisplayName("Cadastro sem estoque inicial não registra movimento")
    void cadastroSemEstoque() throws Exception {
        cadastrar.executar(prego("PRE-01", null, null));

        assertTrue(movimentos.getMovimentos().isEmpty());
    }

    @Test
    @DisplayName("CT-20 código repetido é recusado; código de barras repetido também")
    void ct20_codigosDuplicados() throws Exception {
        cadastrar.executar(prego("PRE-01", "7891234567895", null));

        assertThrows(RegistroDuplicadoException.class, () -> cadastrar.executar(prego("PRE-01", null, null)));
        assertThrows(RegistroDuplicadoException.class, () -> cadastrar.executar(prego("PRE-02", "7891234567895", null)));
        assertEquals(1, produtos.getGravacoes());
    }

    @Test
    @DisplayName("Estoque inicial fracionado demais para a unidade é recusado e nada é gravado")
    void estoqueInicialInvalido() {
        assertThrows(QuantidadeInvalidaException.class, () -> cadastrar.executar(new CadastrarProduto.Comando(
                UnidadeMedida.UNIDADE, "CAD-01", null, "Cadeado", valor("20"), valor("35"), valor("2.5"))));
        assertEquals(0, produtos.getGravacoes());
    }

    // ---------------------------------------------------------------- edição e inativação

    @Test
    @DisplayName("Editar mantendo o próprio código é permitido; usar o código de outro produto não")
    void edicaoDeCodigo() throws Exception {
        Produto prego = cadastrar.executar(prego("PRE-01", null, null));
        cadastrar.executar(prego("PRE-02", null, null));
        AtualizarProduto atualizar = new AtualizarProduto(produtos);
        long id = prego.getId().orElseThrow();

        atualizar.executar(new AtualizarProduto.Comando(id, "PRE-01", null, "Prego 17x27 cabeça dupla",
                valor("12.00"), valor("21.00"), null));
        assertEquals("Prego 17x27 cabeça dupla", prego.getNome());

        assertThrows(RegistroDuplicadoException.class, () -> atualizar.executar(
                new AtualizarProduto.Comando(id, "PRE-02", null, "Prego", valor("12"), valor("21"), null)));
        assertEquals("PRE-01", prego.getCodigo());
    }

    @Test
    @DisplayName("Edição com preço inválido não altera NENHUM campo (nem o nome)")
    void edicaoInvalidaNaoAlteraNada() throws Exception {
        Produto prego = cadastrar.executar(prego("PRE-01", null, null));

        assertThrows(PrecoInvalidoException.class, () -> new AtualizarProduto(produtos).executar(
                new AtualizarProduto.Comando(prego.getId().orElseThrow(), "PRE-01", null, "Nome novo",
                        valor("12"), valor("0"), null)));

        assertEquals("Prego 17x27", prego.getNome());
        assertEquals(valor("19.90"), prego.getPrecoVenda());
    }

    @Test
    @DisplayName("Produto inativado some da busca do PDV")
    void inativadoSomeDaBusca() throws Exception {
        Produto prego = cadastrar.executar(prego("PRE-01", "7891234567895", null));
        ConsultarProdutos consultar = new ConsultarProdutos(produtos);
        assertEquals(1, consultar.executar("prego").size());

        new InativarProduto(produtos).executar(prego.getId().orElseThrow());

        assertTrue(consultar.executar("prego").isEmpty());
        assertTrue(consultar.executar("7891234567895").isEmpty());
    }

    @Test
    @DisplayName("Busca única: código de barras, código ou parte do nome")
    void buscaUnica() throws Exception {
        cadastrar.executar(prego("PRE-01", "7891234567895", null));
        cadastrar.executar(new CadastrarProduto.Comando(UnidadeMedida.KG, "PRE-02", null, "Prego galvanizado",
                valor("15"), valor("25"), null));
        ConsultarProdutos consultar = new ConsultarProdutos(produtos);

        assertEquals("PRE-01", consultar.executar("7891234567895").get(0).getCodigo());
        assertEquals("PRE-02", consultar.executar("PRE-02").get(0).getCodigo());
        List<Produto> porNome = consultar.executar("PREGO");
        assertEquals(2, porNome.size());
        assertTrue(consultar.executar("   ").isEmpty());
    }

    // ---------------------------------------------------------------- ajuste de estoque

    @Test
    @DisplayName("Entrada de mercadoria, perda e correção registram movimentos com sinal")
    void ajustesDeEstoque() throws Exception {
        long id = cadastrar.executar(prego("PRE-01", null, "10")).getId().orElseThrow();

        ajustar.executar(new AjustarEstoque.Comando(id, TipoMovimentoEstoque.ENTRADA_COMPRA, valor("5"), "NF 4521"));
        ajustar.executar(new AjustarEstoque.Comando(id, TipoMovimentoEstoque.PERDA, valor("0.750"), "saco rasgou"));
        BigDecimal estoque = ajustar.executar(
                new AjustarEstoque.Comando(id, TipoMovimentoEstoque.AJUSTE, valor("-0.250"), "contagem"));

        assertEquals(0, valor("14").compareTo(estoque));
        assertEquals(4, movimentos.getMovimentos().size());
        assertEquals(0, valor("-0.750").compareTo(movimentos.getMovimentos().get(2).quantidade()));
    }

    @Test
    @DisplayName("Perda maior que o estoque, perda negativa e saída por venda são recusados")
    void ajustesInvalidos() throws Exception {
        long id = cadastrar.executar(prego("PRE-01", null, "1")).getId().orElseThrow();

        assertThrows(EstoqueInsuficienteException.class, () -> ajustar.executar(
                new AjustarEstoque.Comando(id, TipoMovimentoEstoque.PERDA, valor("2"), "quebra")));
        assertThrows(QuantidadeInvalidaException.class, () -> ajustar.executar(
                new AjustarEstoque.Comando(id, TipoMovimentoEstoque.PERDA, valor("-2"), "quebra")));
        assertThrows(IllegalArgumentException.class, () ->
                new AjustarEstoque.Comando(id, TipoMovimentoEstoque.SAIDA_VENDA, valor("1"), "venda"));

        assertEquals(1, movimentos.getMovimentos().size());
    }

    // ---------------------------------------------------------------- estoque mínimo, listagem, reativação

    @Test
    @DisplayName("Estoque mínimo: gravado no cadastro, alterado na edição; inválido na edição não altera nada")
    void estoqueMinimo() throws Exception {
        Produto prego = cadastrar.executar(new CadastrarProduto.Comando(UnidadeMedida.KG, "PRE-01", null,
                "Prego 17x27", valor("12.00"), valor("19.90"), valor("8"), valor("10")));
        assertTrue(prego.precisaRepor());

        AtualizarProduto atualizar = new AtualizarProduto(produtos);
        long id = prego.getId().orElseThrow();
        atualizar.executar(new AtualizarProduto.Comando(id, "PRE-01", null, "Prego 17x27",
                valor("12.00"), valor("19.90"), valor("5")));
        assertFalse(prego.precisaRepor());

        assertThrows(QuantidadeInvalidaException.class, () -> atualizar.executar(new AtualizarProduto.Comando(
                id, "PRE-01", null, "Nome novo", valor("12.00"), valor("19.90"), valor("0.0005"))));
        assertEquals("Prego 17x27", prego.getNome());
        assertEquals(0, valor("5").compareTo(prego.getEstoqueMinimo().orElseThrow()));
    }

    @Test
    @DisplayName("Listagem filtra por nome ou código e só mostra inativos quando pedido")
    void listagem() throws Exception {
        cadastrar.executar(prego("PRE-01", null, null));
        Produto cadeado = cadastrar.executar(new CadastrarProduto.Comando(UnidadeMedida.UNIDADE, "CAD-30", null,
                "Cadeado 30mm", valor("18.00"), valor("32.50"), null));
        new InativarProduto(produtos).executar(cadeado.getId().orElseThrow());
        ListarProdutos listar = new ListarProdutos(produtos);

        assertEquals(1, listar.executar("", false).size());
        assertEquals(2, listar.executar("", true).size());
        assertEquals(1, listar.executar("cad-", true).size());
        assertEquals(1, listar.executar("PREGO", false).size());
    }

    @Test
    @DisplayName("Produto reativado volta a ser encontrado pelo PDV")
    void reativacao() throws Exception {
        Produto prego = cadastrar.executar(prego("PRE-01", null, null));
        long id = prego.getId().orElseThrow();
        new InativarProduto(produtos).executar(id);
        assertTrue(new ConsultarProdutos(produtos).executar("PRE-01").isEmpty());

        new ReativarProduto(produtos).executar(id);

        assertEquals(1, new ConsultarProdutos(produtos).executar("PRE-01").size());
    }

    @Test
    @DisplayName("Motivo do ajuste é opcional: entrada sem motivo é aceita e registrada")
    void ajusteSemMotivo() throws Exception {
        long id = cadastrar.executar(prego("PRE-01", null, "1")).getId().orElseThrow();

        BigDecimal novo = ajustar.executar(new AjustarEstoque.Comando(id, TipoMovimentoEstoque.ENTRADA_COMPRA,
                valor("5"), "  "));

        assertEquals(0, valor("6").compareTo(novo));
        assertEquals("", movimentos.getMovimentos().get(1).motivo());
    }

    // ---------------------------------------------------------------- exclusão

    @Test
    @DisplayName("Produto nunca vendido (cadastro errado) é apagado; já vendido é inativado, não apagado")
    void exclusaoConformeVendas() throws Exception {
        RegistroVendasEmMemoria vendas = new RegistroVendasEmMemoria();
        ExcluirProduto excluir = new ExcluirProduto(produtos, vendas, new TransacaoEmMemoria());
        Produto errado = cadastrar.executar(prego("PRE-ERRADO", null, "5"));
        Produto vendido = cadastrar.executar(prego("PRE-01", null, "5"));
        Venda venda = new Venda();
        venda.adicionarItem(vendido, valor("1"));
        venda.finalizar(FormaPagamento.PIX, LocalDateTime.now(RELOGIO));
        vendas.salvar(venda, null, null, null);

        assertEquals(ExcluirProduto.Resultado.EXCLUIDO, excluir.executar(errado.getId().orElseThrow()));
        assertTrue(produtos.buscarPorId(errado.getId().orElseThrow()).isEmpty());

        assertEquals(ExcluirProduto.Resultado.INATIVADO, excluir.executar(vendido.getId().orElseThrow()));
        assertFalse(produtos.buscarPorId(vendido.getId().orElseThrow()).orElseThrow().estaAtivo());
    }
}
