package br.com.ferragem.aplicacao.casodeuso;

import br.com.ferragem.aplicacao.porta.CaixaRepository;
import br.com.ferragem.dominio.excecao.CaixaFechadoException;
import br.com.ferragem.dominio.excecao.OperacaoNaoAutorizadaException;
import br.com.ferragem.dominio.excecao.ValorCaixaInvalidoException;
import br.com.ferragem.dominio.modelo.caixa.Operador;
import br.com.ferragem.dominio.modelo.caixa.ResultadoFechamento;
import br.com.ferragem.dominio.modelo.caixa.SessaoCaixa;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Fecha o caixa aberto e devolve a conferência (RN14).
 */
public class FecharCaixa {

    private final CaixaRepository caixas;
    private final Clock relogio;

    /**
     * @param caixas  sessões de caixa
     * @param relogio fonte do "agora"
     */
    public FecharCaixa(CaixaRepository caixas, Clock relogio) {
        this.caixas = Objects.requireNonNull(caixas);
        this.relogio = Objects.requireNonNull(relogio);
    }

    /**
     * @param quem    quem está fechando (quem abriu ou um administrador)
     * @param contado dinheiro contado na gaveta
     * @return esperado, contado, diferença, situação e totais por forma
     * @throws CaixaFechadoException          se não houver caixa aberto
     * @throws OperacaoNaoAutorizadaException se quem fecha não abriu o caixa e não é administrador
     * @throws ValorCaixaInvalidoException    se o valor contado for inválido
     */
    public ResultadoFechamento executar(Operador quem, BigDecimal contado)
            throws CaixaFechadoException, OperacaoNaoAutorizadaException, ValorCaixaInvalidoException {
        SessaoCaixa caixa = caixas.buscarAberta().orElseThrow(CaixaFechadoException::new);
        ResultadoFechamento resultado = caixa.fechar(quem, contado, LocalDateTime.now(relogio));
        caixas.salvar(caixa);
        return resultado;
    }
}
