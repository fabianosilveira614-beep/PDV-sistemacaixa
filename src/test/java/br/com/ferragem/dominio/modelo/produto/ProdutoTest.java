package br.com.ferragem.dominio.modelo.produto;

import br.com.ferragem.dominio.excecao.DadoObrigatorioException;
import br.com.ferragem.dominio.excecao.EstoqueInsuficienteException;
import br.com.ferragem.dominio.excecao.PrecoInvalidoException;
import br.com.ferragem.dominio.excecao.QuantidadeInvalidaException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes das regras de {@link Produto} e subclasses (RN02, RN04, RN06, RN15).
 * Os nomes CT-xx correspondem ao plano de testes do documento de projeto (seção 13).
 */
class ProdutoTest {

    // ---------------------------------------------------------------- fábricas de exemplo

    private static BigDecimal valor(String texto) {
        return new BigDecimal(texto);
    }

    private static ProdutoUnidade parafuso(String estoque) throws Exception {
        ProdutoUnidade produto = new ProdutoUnidade("PAR-01", "7891234567895", "Parafuso 6mm",
                valor("0.10"), valor("0.25"));
        produto.reporEstoque(valor(estoque));
        return produto;
    }

    private static ProdutoKg pregoAGranel(String estoque) throws Exception {
        ProdutoKg produto = new ProdutoKg("PRE-01", null, "Prego 17x27", valor("12.00"), valor("19.90"));
        produto.reporEstoque(valor(estoque));
        return produto;
    }

    private static ProdutoMetro corrente(String estoque) throws Exception {
        ProdutoMetro produto = new ProdutoMetro("COR-01", null, "Corrente galvanizada 5mm",
                valor("8.00"), valor("14.50"));
        produto.reporEstoque(valor(estoque));
        return produto;
    }

    /** BigDecimal: compare com compareTo, não equals (para equals, 3 e 3.000 são diferentes). */
    private static void assertMesmoValor(String esperado, BigDecimal atual) {
        assertEquals(0, valor(esperado).compareTo(atual),
                () -> "esperado " + esperado + " mas foi " + atual.toPlainString());
    }

    // ---------------------------------------------------------------- RN02 margem

    @Test
    @DisplayName("CT-01 margem = (venda - custo) / venda: custo 6,00 e venda 10,00 dá 40,00%")
    void ct01_calculaMargemSobreOPrecoDeVenda() throws Exception {
        ProdutoUnidade martelo = new ProdutoUnidade("MAR-01", null, "Martelo",
                valor("6.00"), valor("10.00"));

        assertEquals(valor("40.00"), martelo.calcularMargem());
    }

    @Test
    @DisplayName("Margem acompanha a mudança de preço (não fica desatualizada)")
    void margemEhRecalculadaQuandoOPrecoMuda() throws Exception {
        ProdutoUnidade martelo = new ProdutoUnidade("MAR-01", null, "Martelo",
                valor("6.00"), valor("10.00"));

        martelo.alterarPrecos(valor("6.00"), valor("12.00"));

        assertEquals(valor("50.00"), martelo.calcularMargem());
    }

    @Test
    @DisplayName("Margem fica negativa quando o produto é vendido abaixo do custo")
    void margemNegativaAbaixoDoCusto() throws Exception {
        ProdutoUnidade queima = new ProdutoUnidade("QUE-01", null, "Ponta de estoque",
                valor("10.00"), valor("8.00"));

        assertEquals(valor("-25.00"), queima.calcularMargem());
    }

    // ---------------------------------------------------------------- RN15 preços

    @Test
    @DisplayName("CT-02 preço de venda zero é recusado")
    void ct02_precoDeVendaZeroEhRecusado() {
        assertThrows(PrecoInvalidoException.class, () -> new ProdutoUnidade("X-01", null, "Item",
                valor("1.00"), valor("0.00")));
    }

    @Test
    @DisplayName("Preço de custo negativo é recusado")
    void precoDeCustoNegativoEhRecusado() {
        assertThrows(PrecoInvalidoException.class, () -> new ProdutoUnidade("X-01", null, "Item",
                valor("-1.00"), valor("2.00")));
    }

    @Test
    @DisplayName("Preço com mais de 2 casas é recusado e o preço antigo é mantido")
    void precoComTresCasasEhRecusadoSemAlterarOProduto() throws Exception {
        ProdutoUnidade item = parafuso("10");

        assertThrows(PrecoInvalidoException.class,
                () -> item.alterarPrecos(valor("0.10"), valor("0.255")));
        assertMesmoValor("0.25", item.getPrecoVenda());
    }

    @Test
    @DisplayName("Custo zero é permitido (brinde, doação)")
    void custoZeroEhPermitido() {
        assertDoesNotThrow(() -> new ProdutoUnidade("BRI-01", null, "Chaveiro brinde",
                valor("0"), valor("1.00")));
    }

    // ---------------------------------------------------------------- RN06 quantidade por tipo

    @Test
    @DisplayName("CT-03 produto por unidade recusa quantidade fracionada (1,5)")
    void ct03_unidadeRecusaQuantidadeFracionada() throws Exception {
        ProdutoUnidade item = parafuso("10");

        assertThrows(QuantidadeInvalidaException.class, () -> item.validarQuantidade(valor("1.5")));
    }

    @Test
    @DisplayName("Produto por unidade aceita 2.0 (é inteiro, só está escrito com decimal)")
    void unidadeAceitaInteiroEscritoComDecimal() throws Exception {
        ProdutoUnidade item = parafuso("10");

        assertDoesNotThrow(() -> item.validarQuantidade(valor("2.0")));
    }

    @Test
    @DisplayName("CT-04 produto por quilo aceita 0,250 e recusa 0,2505")
    void ct04_quiloAceitaAteTresCasas() throws Exception {
        ProdutoKg prego = pregoAGranel("50");

        assertDoesNotThrow(() -> prego.validarQuantidade(valor("0.250")));
        assertThrows(QuantidadeInvalidaException.class, () -> prego.validarQuantidade(valor("0.2505")));
    }

    @Test
    @DisplayName("Produto por metro aceita 1,35 e recusa 1,355")
    void metroAceitaAteDuasCasas() throws Exception {
        ProdutoMetro item = corrente("100");

        assertDoesNotThrow(() -> item.validarQuantidade(valor("1.35")));
        assertThrows(QuantidadeInvalidaException.class, () -> item.validarQuantidade(valor("1.355")));
    }

    @Test
    @DisplayName("Reposição fracionada é recusada para produto por unidade, sem alterar o estoque")
    void reposicaoFracionadaRecusadaParaUnidade() throws Exception {
        ProdutoUnidade item = parafuso("10");

        assertThrows(QuantidadeInvalidaException.class, () -> item.reporEstoque(valor("2.5")));
        assertMesmoValor("10", item.getQuantidadeEstoque());
    }

    @Test
    @DisplayName("Produto novo nasce com estoque zero (cadastrado antes de a mercadoria chegar)")
    void produtoNovoNasceComEstoqueZero() throws Exception {
        ProdutoKg prego = new ProdutoKg("PRE-09", null, "Prego 12x12", valor("10.00"), valor("15.00"));

        assertMesmoValor("0", prego.getQuantidadeEstoque());
    }

    // ---------------------------------------------------------------- RN04 estoque

    @Test
    @DisplayName("CT-05 baixar 5 com estoque 3 é recusado e o estoque continua 3")
    void ct05_naoBaixaEstoqueAlemDoDisponivel() throws Exception {
        ProdutoUnidade item = parafuso("3");

        EstoqueInsuficienteException erro = assertThrows(EstoqueInsuficienteException.class,
                () -> item.baixarEstoque(valor("5")));

        assertMesmoValor("3", item.getQuantidadeEstoque());
        assertMesmoValor("3", erro.getDisponivel());
        assertMesmoValor("5", erro.getSolicitado());
    }

    @Test
    @DisplayName("Baixa dentro do estoque reduz a quantidade")
    void baixaReduzOEstoque() throws Exception {
        ProdutoKg prego = pregoAGranel("10");

        prego.baixarEstoque(valor("2.750"));

        assertMesmoValor("7.25", prego.getQuantidadeEstoque());
    }

    @Test
    @DisplayName("Pode vender exatamente todo o estoque")
    void podeZerarOEstoque() throws Exception {
        ProdutoUnidade item = parafuso("3");

        item.baixarEstoque(valor("3"));

        assertMesmoValor("0", item.getQuantidadeEstoque());
        assertFalse(item.temEstoqueSuficiente(valor("1")));
    }

    @Test
    @DisplayName("Reposição aumenta o estoque")
    void reposicaoAumentaOEstoque() throws Exception {
        ProdutoMetro item = corrente("10.50");

        item.reporEstoque(valor("25"));

        assertMesmoValor("35.50", item.getQuantidadeEstoque());
    }

    // ---------------------------------------------------------------- dados obrigatórios e situação

    @Test
    @DisplayName("Nome em branco é recusado")
    void nomeEmBrancoEhRecusado() {
        DadoObrigatorioException erro = assertThrows(DadoObrigatorioException.class,
                () -> new ProdutoUnidade("X-01", null, "   ", valor("1"), valor("2")));

        assertEquals("nome do produto", erro.getCampo());
    }

    @Test
    @DisplayName("Código de barras vazio vira ausente")
    void codigoDeBarrasVazioViraAusente() throws Exception {
        ProdutoKg prego = new ProdutoKg("PRE-02", "  ", "Prego", valor("1"), valor("2"));

        assertTrue(prego.getCodigoBarras().isEmpty());
    }

    @Test
    @DisplayName("Produto nasce ativo e pode ser inativado")
    void produtoPodeSerInativado() throws Exception {
        ProdutoUnidade item = parafuso("1");
        assertTrue(item.estaAtivo());

        item.inativar();

        assertFalse(item.estaAtivo());
    }

    @Test
    @DisplayName("Id só pode ser atribuído uma vez")
    void idSoPodeSerAtribuidoUmaVez() throws Exception {
        ProdutoUnidade item = parafuso("1");
        assertTrue(item.getId().isEmpty());

        item.atribuirId(42);

        assertEquals(42L, item.getId().orElseThrow());
        assertThrows(IllegalStateException.class, () -> item.atribuirId(43));
    }

    // ---------------------------------------------------------------- estoque mínimo e reativação

    @Test
    @DisplayName("Precisa repor quando o estoque chega no mínimo; sem mínimo, nunca")
    void precisaReporNoMinimo() throws Exception {
        ProdutoUnidade semControle = parafuso("2");
        assertFalse(semControle.precisaRepor());

        ProdutoUnidade parafuso = parafuso("11");
        parafuso.definirEstoqueMinimo(valor("10"));
        assertFalse(parafuso.precisaRepor());
        parafuso.baixarEstoque(valor("1"));
        assertTrue(parafuso.precisaRepor(), "no mínimo exato já é hora de repor");

        parafuso.definirEstoqueMinimo(null);
        assertFalse(parafuso.precisaRepor());
    }

    @Test
    @DisplayName("Estoque mínimo segue o fracionamento da unidade e não aceita zero")
    void minimoSegueAUnidade() throws Exception {
        assertThrows(QuantidadeInvalidaException.class, () -> parafuso("1").definirEstoqueMinimo(valor("2.5")));
        assertThrows(QuantidadeInvalidaException.class, () -> parafuso("1").definirEstoqueMinimo(valor("0")));
        assertDoesNotThrow(() -> pregoAGranel("1").definirEstoqueMinimo(valor("2.5")));
    }

    @Test
    @DisplayName("Produto inativado pode ser reativado")
    void reativar() throws Exception {
        ProdutoUnidade parafuso = parafuso("1");
        parafuso.inativar();
        assertFalse(parafuso.estaAtivo());
        parafuso.reativar();
        assertTrue(parafuso.estaAtivo());
    }
}
