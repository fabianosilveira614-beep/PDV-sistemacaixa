package br.com.ferragem.aplicacao.porta;

import br.com.ferragem.dominio.modelo.venda.FormaPagamento;

import java.math.BigDecimal;

/**
 * Comunicação com a operadora de cartão (maquininha/TEF).
 *
 * <p>Neste projeto a implementação é simulada; uma integração real substituiria só a classe de
 * infraestrutura, sem mudar nenhum caso de uso (inversão de dependência).</p>
 */
public interface ProcessadorCartao {

    /**
     * Pede autorização para cobrar um valor no cartão.
     *
     * @param valor valor a cobrar
     * @param tipo  {@link FormaPagamento#CREDITO} ou {@link FormaPagamento#DEBITO}
     * @return resposta da operadora
     */
    Autorizacao autorizar(BigDecimal valor, FormaPagamento tipo);

    /**
     * Resposta da operadora.
     *
     * @param aprovada {@code true} se a cobrança foi aceita
     * @param codigo   código da autorização (NSU), vazio se recusada
     * @param motivo   motivo da recusa, vazio se aprovada
     */
    record Autorizacao(boolean aprovada, String codigo, String motivo) {
    }
}
