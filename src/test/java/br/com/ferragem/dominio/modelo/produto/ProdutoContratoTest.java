package br.com.ferragem.dominio.modelo.produto;

import br.com.ferragem.dominio.excecao.EstoqueInsuficienteException;
import br.com.ferragem.dominio.excecao.QuantidadeInvalidaException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Contrato que <b>toda</b> subclasse de {@link Produto} precisa cumprir (princípio de substituição
 * de Liskov, o "L" do SOLID).
 *
 * <p>Cada teste roda uma vez para cada tipo de produto. Se alguém criar um novo tipo (ex.:
 * {@code ProdutoLitro}) e o incluir em {@link #todosOsTipos()}, estes testes garantem que ele pode
 * substituir um {@code Produto} em qualquer lugar do sistema sem surpresas.</p>
 */
class ProdutoContratoTest {

    static Stream<Produto> todosOsTipos() throws Exception {
        return Stream.of(
                comEstoqueDez(new ProdutoUnidade("U-01", null, "Cadeado", new BigDecimal("20.00"), new BigDecimal("35.00"))),
                comEstoqueDez(new ProdutoKg("K-01", null, "Arame", new BigDecimal("15.00"), new BigDecimal("22.00"))),
                comEstoqueDez(new ProdutoMetro("M-01", null, "Mangueira", new BigDecimal("3.00"), new BigDecimal("5.50"))));
    }

    private static Produto comEstoqueDez(Produto produto) throws Exception {
        produto.reporEstoque(BigDecimal.TEN);
        return produto;
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("todosOsTipos")
    void todoTipoRecusaQuantidadeZero(Produto produto) {
        assertThrows(QuantidadeInvalidaException.class, () -> produto.validarQuantidade(BigDecimal.ZERO));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("todosOsTipos")
    void todoTipoRecusaQuantidadeNegativa(Produto produto) {
        assertThrows(QuantidadeInvalidaException.class, () -> produto.validarQuantidade(new BigDecimal("-1")));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("todosOsTipos")
    void todoTipoAceitaQuantidadeInteira(Produto produto) throws Exception {
        produto.validarQuantidade(new BigDecimal("2"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("todosOsTipos")
    void todoTipoRecusaBaixaAlemDoEstoqueSemAlterarNada(Produto produto) {
        assertThrows(EstoqueInsuficienteException.class, () -> produto.baixarEstoque(new BigDecimal("11")));
        assertEquals(0, new BigDecimal("10").compareTo(produto.getQuantidadeEstoque()));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("todosOsTipos")
    void todoTipoInformaSuaUnidade(Produto produto) {
        assertNotNull(produto.getUnidade());
    }
}
