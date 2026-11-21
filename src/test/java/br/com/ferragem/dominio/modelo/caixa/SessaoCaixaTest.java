package br.com.ferragem.dominio.modelo.caixa;

import br.com.ferragem.dominio.excecao.CaixaFechadoException;
import br.com.ferragem.dominio.excecao.DadoObrigatorioException;
import br.com.ferragem.dominio.excecao.OperacaoNaoAutorizadaException;
import br.com.ferragem.dominio.excecao.SaldoCaixaInsuficienteException;
import br.com.ferragem.dominio.excecao.ValorCaixaInvalidoException;
import br.com.ferragem.dominio.modelo.venda.FormaPagamento;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Testes da sessão de caixa (RN08, RN14, sangria limitada, quem pode fechar). */
class SessaoCaixaTest {

    private static final LocalDateTime ABERTURA = LocalDateTime.of(2026, 11, 20, 8, 0);
    private static final LocalDateTime DURANTE = LocalDateTime.of(2026, 11, 20, 12, 0);
    private static final LocalDateTime FECHAMENTO = LocalDateTime.of(2026, 11, 20, 18, 0);

    private Operador ana;
    private Operador bruno;
    private Operador dona;
    private SessaoCaixa caixa;

    private static BigDecimal valor(String texto) {
        return new BigDecimal(texto);
    }

    @BeforeEach
    void preparar() throws Exception {
        ana = new Operador("Ana", "ana", "hash-ana", PerfilOperador.OPERADOR);
        bruno = new Operador("Bruno", "bruno", "hash-bruno", PerfilOperador.OPERADOR);
        dona = new Operador("Dona Marta", "marta", "hash-marta", PerfilOperador.ADMINISTRADOR);
        caixa = SessaoCaixa.abrir(ana, valor("100.00"), ABERTURA);
    }

    @Test
    @DisplayName("CT-25 abertura 100 + vendas dinheiro 250 + fiado dinheiro 50 + suprimento 20 - sangria 70 = 350; contado 340 = QUEBRA de 10")
    void ct25_fechamentoComQuebra() throws Exception {
        caixa.registrarVenda(FormaPagamento.DINHEIRO, valor("150.00"), "venda #1", DURANTE);
        caixa.registrarVenda(FormaPagamento.DINHEIRO, valor("100.00"), "venda #2", DURANTE);
        caixa.registrarRecebimentoFiado(FormaPagamento.DINHEIRO, valor("50.00"), "fiado Seu Zé", DURANTE);
        caixa.registrarSuprimento(valor("20.00"), "troco", DURANTE);
        caixa.registrarSangria(valor("70.00"), "cofre", DURANTE);
        caixa.registrarVenda(FormaPagamento.PIX, valor("80.00"), "venda #3", DURANTE);

        ResultadoFechamento resultado = caixa.fechar(ana, valor("340.00"), FECHAMENTO);

        assertEquals(valor("350.00"), resultado.esperado());
        assertEquals(valor("-10.00"), resultado.diferenca());
        assertEquals(SituacaoFechamento.QUEBRA, resultado.situacao());
        assertEquals(valor("300.00"), resultado.recebidoPorForma().get(FormaPagamento.DINHEIRO));
        assertEquals(valor("80.00"), resultado.recebidoPorForma().get(FormaPagamento.PIX));
        assertFalse(caixa.estaAberta());
    }

    @Test
    @DisplayName("Fechamento com sobra e com conferência exata")
    void sobraEConfere() throws Exception {
        SessaoCaixa outro = SessaoCaixa.abrir(ana, valor("50"), ABERTURA);

        assertEquals(SituacaoFechamento.SOBRA, caixa.fechar(ana, valor("105"), FECHAMENTO).situacao());
        assertEquals(SituacaoFechamento.CONFERE, outro.fechar(ana, valor("50.00"), FECHAMENTO).situacao());
    }

    @Test
    @DisplayName("Pix e cartão não entram no dinheiro esperado da gaveta")
    void pixECartaoNaoEntramNaGaveta() throws Exception {
        caixa.registrarVenda(FormaPagamento.PIX, valor("40"), "venda", DURANTE);
        caixa.registrarVenda(FormaPagamento.CREDITO, valor("60"), "venda", DURANTE);

        assertEquals(valor("100.00"), caixa.calcularSaldoEsperado());
    }

    @Test
    @DisplayName("Sangria maior que o dinheiro da gaveta é recusada (Pix não conta)")
    void sangriaMaiorQueAGavetaEhRecusada() throws Exception {
        caixa.registrarVenda(FormaPagamento.PIX, valor("500"), "venda", DURANTE);

        assertThrows(SaldoCaixaInsuficienteException.class,
                () -> caixa.registrarSangria(valor("100.01"), "cofre", DURANTE));
        assertEquals(valor("100.00"), caixa.calcularSaldoEsperado());

        caixa.registrarSangria(valor("100.00"), "cofre", DURANTE);
        assertEquals(0, caixa.calcularSaldoEsperado().signum());
    }

    @Test
    @DisplayName("Valores inválidos de abertura, suprimento e sangria são recusados")
    void valoresInvalidos() {
        assertThrows(ValorCaixaInvalidoException.class, () -> SessaoCaixa.abrir(ana, valor("-1"), ABERTURA));
        assertThrows(ValorCaixaInvalidoException.class, () -> caixa.registrarSuprimento(valor("0"), "x", DURANTE));
        assertThrows(ValorCaixaInvalidoException.class, () -> caixa.registrarSangria(valor("10.001"), "x", DURANTE));
    }

    @Test
    @DisplayName("Caixa pode abrir com zero na gaveta")
    void aberturaComZero() throws Exception {
        assertEquals(valor("0.00"), SessaoCaixa.abrir(ana, valor("0"), ABERTURA).calcularSaldoEsperado());
    }

    @Test
    @DisplayName("Caixa fechado não aceita movimentos nem novo fechamento (RN08)")
    void caixaFechadoNaoAceitaNada() throws Exception {
        caixa.fechar(ana, valor("100"), FECHAMENTO);

        assertThrows(CaixaFechadoException.class,
                () -> caixa.registrarVenda(FormaPagamento.DINHEIRO, valor("10"), "venda", FECHAMENTO));
        assertThrows(CaixaFechadoException.class,
                () -> caixa.registrarSuprimento(valor("10"), "troco", FECHAMENTO));
        assertThrows(CaixaFechadoException.class, () -> caixa.fechar(ana, valor("100"), FECHAMENTO));
    }

    @Test
    @DisplayName("Outro operador não pode fechar o caixa; quem abriu e o administrador podem")
    void quemPodeFechar() throws Exception {
        assertThrows(OperacaoNaoAutorizadaException.class, () -> caixa.fechar(bruno, valor("100"), FECHAMENTO));
        assertTrue(caixa.estaAberta());

        caixa.fechar(dona, valor("100"), FECHAMENTO);
        assertFalse(caixa.estaAberta());

        SessaoCaixa daAna = SessaoCaixa.abrir(ana, valor("10"), ABERTURA);
        Operador anaDeNovo = new Operador("Ana", "ANA", "hash-ana", PerfilOperador.OPERADOR);
        daAna.fechar(anaDeNovo, valor("10"), FECHAMENTO);
        assertFalse(daAna.estaAberta());
    }

    @Test
    @DisplayName("Venda fiado não entra no caixa")
    void vendaFiadoNaoEntraNoCaixa() {
        assertThrows(IllegalArgumentException.class,
                () -> caixa.registrarVenda(FormaPagamento.FIADO, valor("50"), "venda", DURANTE));
    }

    @Test
    @DisplayName("Operador sem login é recusado; toString não mostra o hash da senha")
    void operador() throws Exception {
        assertThrows(DadoObrigatorioException.class,
                () -> new Operador("Carlos", " ", "hash", PerfilOperador.OPERADOR));
        assertFalse(ana.toString().contains("hash-ana"));
    }
}
