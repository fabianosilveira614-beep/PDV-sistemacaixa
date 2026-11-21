package br.com.ferragem.aplicacao.casodeuso;

import br.com.ferragem.aplicacao.porta.CaixaRepository;
import br.com.ferragem.dominio.excecao.CaixaFechadoException;
import br.com.ferragem.dominio.excecao.ValorCaixaInvalidoException;
import br.com.ferragem.dominio.modelo.caixa.SessaoCaixa;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Registra um reforço de dinheiro na gaveta do caixa aberto.
 */
public class RegistrarSuprimento {

    private final CaixaRepository caixas;
    private final Clock relogio;

    /**
     * @param caixas  sessões de caixa
     * @param relogio fonte do "agora"
     */
    public RegistrarSuprimento(CaixaRepository caixas, Clock relogio) {
        this.caixas = Objects.requireNonNull(caixas);
        this.relogio = Objects.requireNonNull(relogio);
    }

    /**
     * @param valor  valor colocado na gaveta
     * @param motivo motivo (ex.: "troco")
     * @throws CaixaFechadoException       se não houver caixa aberto
     * @throws ValorCaixaInvalidoException se o valor for inválido
     */
    public void executar(BigDecimal valor, String motivo) throws CaixaFechadoException, ValorCaixaInvalidoException {
        SessaoCaixa caixa = caixas.buscarAberta().orElseThrow(CaixaFechadoException::new);
        caixa.registrarSuprimento(valor, motivo, LocalDateTime.now(relogio));
        caixas.salvar(caixa);
    }
}
