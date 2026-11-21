package br.com.ferragem.aplicacao.casodeuso;

import br.com.ferragem.aplicacao.porta.CaixaRepository;
import br.com.ferragem.dominio.excecao.CaixaFechadoException;
import br.com.ferragem.dominio.excecao.SaldoCaixaInsuficienteException;
import br.com.ferragem.dominio.excecao.ValorCaixaInvalidoException;
import br.com.ferragem.dominio.modelo.caixa.SessaoCaixa;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Registra uma retirada de dinheiro da gaveta do caixa aberto, limitada ao dinheiro que há nela.
 */
public class RegistrarSangria {

    private final CaixaRepository caixas;
    private final Clock relogio;

    /**
     * @param caixas  sessões de caixa
     * @param relogio fonte do "agora"
     */
    public RegistrarSangria(CaixaRepository caixas, Clock relogio) {
        this.caixas = Objects.requireNonNull(caixas);
        this.relogio = Objects.requireNonNull(relogio);
    }

    /**
     * @param valor  valor retirado
     * @param motivo motivo (ex.: "levado ao cofre")
     * @throws CaixaFechadoException           se não houver caixa aberto
     * @throws ValorCaixaInvalidoException     se o valor for inválido
     * @throws SaldoCaixaInsuficienteException se o valor for maior que o dinheiro na gaveta
     */
    public void executar(BigDecimal valor, String motivo)
            throws CaixaFechadoException, ValorCaixaInvalidoException, SaldoCaixaInsuficienteException {
        SessaoCaixa caixa = caixas.buscarAberta().orElseThrow(CaixaFechadoException::new);
        caixa.registrarSangria(valor, motivo, LocalDateTime.now(relogio));
        caixas.salvar(caixa);
    }
}
