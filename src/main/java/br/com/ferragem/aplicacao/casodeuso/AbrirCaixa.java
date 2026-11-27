package br.com.ferragem.aplicacao.casodeuso;

import br.com.ferragem.aplicacao.porta.CaixaRepository;
import br.com.ferragem.dominio.excecao.CaixaJaAbertoException;
import br.com.ferragem.dominio.excecao.ValorCaixaInvalidoException;
import br.com.ferragem.dominio.modelo.caixa.Operador;
import br.com.ferragem.dominio.modelo.caixa.SessaoCaixa;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Abre o caixa do dia. Só pode haver um caixa aberto
 * ess classe confere antes
 */
public class AbrirCaixa {

    private final CaixaRepository caixas;
    private final Clock relogio;

    /**
     * @param caixas  sessões de caixa - sessões abertas
     * @param relogio hora que foi aberto
     */
    public AbrirCaixa(CaixaRepository caixas, Clock relogio) {
        this.caixas = Objects.requireNonNull(caixas);
        this.relogio = Objects.requireNonNull(relogio);
    }

    /**
     * @param operador      quem está abrindo
     * @param valorAbertura dinheiro no caixa
     * @return a sessão aberta
     * @throws CaixaJaAbertoException      se já houver um caixa aberto
     * @throws ValorCaixaInvalidoException se o valor for negativo ou tiver mais de 2 casas
     */
    public SessaoCaixa executar(Operador operador, BigDecimal valorAbertura)
            throws CaixaJaAbertoException, ValorCaixaInvalidoException {
        if (caixas.buscarAberta().isPresent()) {
            throw new CaixaJaAbertoException();
        }
        SessaoCaixa sessao = SessaoCaixa.abrir(operador, valorAbertura, LocalDateTime.now(relogio));
        caixas.salvar(sessao);
        return sessao;
    }
}
