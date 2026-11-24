package br.com.ferragem.aplicacao.casodeuso;

import br.com.ferragem.aplicacao.casodeuso.CarregarOrcamento.Aviso;
import br.com.ferragem.aplicacao.casodeuso.CarregarOrcamento.TipoAviso;
import br.com.ferragem.aplicacao.memoria.OrcamentoRepositoryEmMemoria;
import br.com.ferragem.aplicacao.memoria.ProdutoRepositoryEmMemoria;
import br.com.ferragem.aplicacao.memoria.TransacaoEmMemoria;
import br.com.ferragem.dominio.excecao.OrcamentoVazioException;
import br.com.ferragem.dominio.modelo.caixa.Operador;
import br.com.ferragem.dominio.modelo.caixa.PerfilOperador;
import br.com.ferragem.dominio.modelo.cliente.Cliente;
import br.com.ferragem.dominio.modelo.orcamento.Orcamento;
import br.com.ferragem.dominio.modelo.produto.Produto;
import br.com.ferragem.dominio.modelo.produto.ProdutoUnidade;
import br.com.ferragem.dominio.modelo.venda.Venda;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Testes do orçamento: salvar, listar, excluir e carregar no caixa. */
class OrcamentoCasosDeUsoTest {

    private static final Clock RELOGIO = Clock.fixed(Instant.parse("2026-11-21T13:00:00Z"), ZoneId.of("America/Sao_Paulo"));

    private ProdutoRepositoryEmMemoria produtos;
    private OrcamentoRepositoryEmMemoria orcamentos;
    private SalvarOrcamento salvar;
    private CarregarOrcamento carregar;
    private Operador ana;
    private Produto martelo;
    private Produto serrote;

    private static BigDecimal valor(String texto) {
        return new BigDecimal(texto);
    }

    @BeforeEach
    void preparar() throws Exception {
        produtos = new ProdutoRepositoryEmMemoria();
        orcamentos = new OrcamentoRepositoryEmMemoria();
        salvar = new SalvarOrcamento(orcamentos, new TransacaoEmMemoria(), RELOGIO);
        carregar = new CarregarOrcamento(orcamentos, produtos);
        ana = new Operador("Ana", "ana", "hash", PerfilOperador.OPERADOR);
        martelo = produto("MAR-1", "Martelo", "35", "10");
        serrote = produto("SER-1", "Serrote", "50", "2");
    }

    private Produto produto(String codigo, String nome, String preco, String estoque) throws Exception {
        ProdutoUnidade produto = new ProdutoUnidade(codigo, null, nome, valor("10"), valor(preco));
        produto.reporEstoque(valor(estoque));
        produtos.salvar(produto);
        return produto;
    }

    private Venda carrinho(Cliente cliente) throws Exception {
        Venda venda = new Venda();
        venda.adicionarItem(martelo, valor("2"));
        venda.adicionarItem(serrote, valor("1"));
        if (cliente != null) {
            venda.identificarCliente(cliente);
        }
        return venda;
    }

    @Test
    @DisplayName("Salvar orçamento: guarda itens, preços, cliente e atendente; não mexe no carrinho nem no estoque")
    void salvaSemMexerEmNada() throws Exception {
        Cliente ze = new Cliente("Seu Zé", null, null);
        Venda carrinho = carrinho(ze);

        Orcamento orcamento = salvar.executar(carrinho, ana);

        assertEquals(1L, orcamento.getId().orElseThrow());
        assertEquals(valor("120.00"), orcamento.calcularTotal());
        assertEquals("Ana", orcamento.getAtendente());
        assertEquals("Seu Zé", orcamento.getCliente().orElseThrow().getNome());
        assertEquals(2, carrinho.getItens().size());             // carrinho intacto, ainda em aberto
        assertFalse(carrinho.getFormaPagamento().isPresent());
        assertEquals(0, valor("10").compareTo(martelo.getQuantidadeEstoque())); // estoque intacto
        assertThrows(OrcamentoVazioException.class, () -> salvar.executar(new Venda(), ana));
    }

    @Test
    @DisplayName("Orçamento pode pedir mais do que há em estoque (não confere estoque)")
    void naoConfereEstoque() throws Exception {
        Venda carrinho = new Venda();
        carrinho.adicionarItem(serrote, valor("2"));     // todo o estoque
        Orcamento orcamento = salvar.executar(carrinho, ana);
        serrote.baixarEstoque(valor("2"));               // vendido depois por outro caminho

        assertEquals(1, orcamento.getItens().size());
        assertEquals(1, new ListarOrcamentos(orcamentos).executar().size());
    }

    @Test
    @DisplayName("Carregar: preço de hoje com aviso; fora de linha e sem estoque ficam de fora; cliente excluído não volta")
    void carregaComPrecoDeHojeEAvisos() throws Exception {
        Cliente ze = new Cliente("Seu Zé", null, null);
        Produto prego = produto("PRE-1", "Prego", "5", "100");
        Venda carrinho = carrinho(ze);
        carrinho.adicionarItem(prego, valor("10"));
        long id = salvar.executar(carrinho, ana).getId().orElseThrow();

        martelo.alterarPrecos(valor("10"), valor("40"));  // reajuste
        serrote.inativar();                                // saiu de linha
        prego.baixarEstoque(valor("95"));                  // só sobraram 5
        ze.inativar();                                     // cliente excluído

        CarregarOrcamento.Resultado resultado = carregar.executar(id);

        Venda novo = resultado.carrinho();
        assertEquals(1, novo.getItens().size());
        assertEquals(valor("80.00"), novo.calcularTotal());             // 2 × R$ 40, preço de hoje
        assertTrue(novo.getCliente().isEmpty());
        assertEquals(List.of(TipoAviso.PRECO_MUDOU, TipoAviso.SAIU_DE_LINHA, TipoAviso.SEM_ESTOQUE,
                TipoAviso.CLIENTE_EXCLUIDO), resultado.avisos().stream().map(Aviso::tipo).toList());
        Aviso preco = resultado.avisos().get(0);
        assertEquals(0, valor("35").compareTo(preco.antes()));
        assertEquals(0, valor("40").compareTo(preco.agora()));
    }

    @Test
    @DisplayName("Carregar sem mudanças: mesmo carrinho, mesmo cliente, nenhum aviso; o orçamento continua guardado")
    void carregaSemMudancas() throws Exception {
        Cliente ze = new Cliente("Seu Zé", null, null);
        long id = salvar.executar(carrinho(ze), ana).getId().orElseThrow();

        CarregarOrcamento.Resultado resultado = carregar.executar(id);

        assertTrue(resultado.avisos().isEmpty());
        assertEquals(valor("120.00"), resultado.carrinho().calcularTotal());
        assertEquals("Seu Zé", resultado.carrinho().getCliente().orElseThrow().getNome());
        assertTrue(orcamentos.buscarPorId(id).isPresent());
    }

    @Test
    @DisplayName("Excluir apaga só aquele orçamento; lista vem do mais recente para o mais antigo")
    void excluirEListar() throws Exception {
        long primeiro = salvar.executar(carrinho(null), ana).getId().orElseThrow();
        long segundo = salvar.executar(carrinho(null), ana).getId().orElseThrow();

        assertEquals(List.of(segundo, primeiro), new ListarOrcamentos(orcamentos).executar().stream()
                .map(o -> o.getId().orElseThrow()).toList());
        new ExcluirOrcamento(orcamentos).executar(primeiro);
        assertEquals(List.of(segundo), new ListarOrcamentos(orcamentos).executar().stream()
                .map(o -> o.getId().orElseThrow()).toList());
    }
}
