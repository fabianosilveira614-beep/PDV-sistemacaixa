package br.com.ferragem.aplicacao.casodeuso;

import br.com.ferragem.aplicacao.memoria.CaixaRepositoryEmMemoria;
import br.com.ferragem.dominio.excecao.CaixaFechadoException;
import br.com.ferragem.dominio.excecao.CaixaJaAbertoException;
import br.com.ferragem.dominio.excecao.SaldoCaixaInsuficienteException;
import br.com.ferragem.dominio.modelo.caixa.Operador;
import br.com.ferragem.dominio.modelo.caixa.PerfilOperador;
import br.com.ferragem.dominio.modelo.caixa.ResultadoFechamento;
import br.com.ferragem.dominio.modelo.caixa.SituacaoFechamento;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Testes de abrir, movimentar e fechar o caixa pelos casos de uso. */
class CaixaCasosDeUsoTest {

    private static final Clock RELOGIO =
            Clock.fixed(Instant.parse("2026-11-20T11:00:00Z"), ZoneId.of("America/Sao_Paulo"));

    private CaixaRepositoryEmMemoria caixas;
    private AbrirCaixa abrirCaixa;
    private FecharCaixa fecharCaixa;
    private Operador ana;

    private static BigDecimal valor(String texto) {
        return new BigDecimal(texto);
    }

    @BeforeEach
    void preparar() throws Exception {
        caixas = new CaixaRepositoryEmMemoria();
        abrirCaixa = new AbrirCaixa(caixas, RELOGIO);
        fecharCaixa = new FecharCaixa(caixas, RELOGIO);
        ana = new Operador("Ana", "ana", "hash", PerfilOperador.OPERADOR);
    }

    @Test
    @DisplayName("CT-24 abrir caixa com outro já aberto é recusado")
    void ct24_soUmCaixaAberto() throws Exception {
        abrirCaixa.executar(ana, valor("100"));

        assertThrows(CaixaJaAbertoException.class, () -> abrirCaixa.executar(ana, valor("50")));
        assertEquals(1, caixas.getTodas().size());
    }

    @Test
    @DisplayName("Expediente completo: abre 100, suprimento 50, sangria 120, contado 30 = CONFERE")
    void expedienteCompleto() throws Exception {
        abrirCaixa.executar(ana, valor("100"));
        new RegistrarSuprimento(caixas, RELOGIO).executar(valor("50"), "troco");
        new RegistrarSangria(caixas, RELOGIO).executar(valor("120"), "cofre");

        ResultadoFechamento resultado = fecharCaixa.executar(ana, valor("30"));

        assertEquals(SituacaoFechamento.CONFERE, resultado.situacao());
        assertTrue(caixas.buscarAberta().isEmpty());
    }

    @Test
    @DisplayName("Sangria maior que a gaveta é recusada pelo caso de uso")
    void sangriaMaiorQueAGaveta() throws Exception {
        abrirCaixa.executar(ana, valor("100"));

        assertThrows(SaldoCaixaInsuficienteException.class,
                () -> new RegistrarSangria(caixas, RELOGIO).executar(valor("150"), "cofre"));
    }

    @Test
    @DisplayName("Sem caixa aberto não há sangria, suprimento nem fechamento")
    void semCaixaAberto() {
        assertThrows(CaixaFechadoException.class,
                () -> new RegistrarSuprimento(caixas, RELOGIO).executar(valor("10"), "troco"));
        assertThrows(CaixaFechadoException.class,
                () -> new RegistrarSangria(caixas, RELOGIO).executar(valor("10"), "cofre"));
        assertThrows(CaixaFechadoException.class, () -> fecharCaixa.executar(ana, valor("0")));
    }

    @Test
    @DisplayName("Depois de fechar, um novo caixa pode ser aberto")
    void abreNovoDepoisDeFechar() throws Exception {
        abrirCaixa.executar(ana, valor("100"));
        fecharCaixa.executar(ana, valor("100"));

        abrirCaixa.executar(ana, valor("80"));

        assertEquals(2, caixas.getTodas().size());
        assertEquals(valor("80.00"), caixas.buscarAberta().orElseThrow().getValorAbertura());
    }
}
