package br.com.ferragem.aplicacao.memoria;

import br.com.ferragem.aplicacao.porta.ProcessadorCartao;
import br.com.ferragem.dominio.modelo.venda.FormaPagamento;

import java.math.BigDecimal;

/** Operadora de cartão controlada pelo teste: aprova ou recusa conforme configurado. */
public class OperadoraCartaoFalsa implements ProcessadorCartao {

    private boolean aprovar = true;
    private int cobrancas;

    @Override
    public Autorizacao autorizar(BigDecimal valor, FormaPagamento tipo) {
        cobrancas++;
        return aprovar
                ? new Autorizacao(true, "AUT-" + cobrancas, "")
                : new Autorizacao(false, "", "saldo insuficiente");
    }

    /** Faz as próximas cobranças serem recusadas. */
    public void passarARecusar() {
        aprovar = false;
    }

    /** @return quantas cobranças foram pedidas à operadora */
    public int getCobrancas() {
        return cobrancas;
    }
}
