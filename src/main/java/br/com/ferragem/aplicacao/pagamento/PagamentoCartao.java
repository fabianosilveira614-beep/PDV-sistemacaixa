package br.com.ferragem.aplicacao.pagamento;

import br.com.ferragem.aplicacao.porta.ProcessadorCartao;
import br.com.ferragem.dominio.excecao.PagamentoRecusadoException;
import br.com.ferragem.dominio.modelo.venda.FormaPagamento;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Pagamento com cartão de crédito ou débito. Pede autorização à operadora pela porta
 * {@link ProcessadorCartao}, sem saber se a operadora é real ou simulada.
 */
public class PagamentoCartao implements ProcessadorPagamento {

    private final FormaPagamento tipo;
    private final ProcessadorCartao operadora;

    /**
     * @param tipo      {@link FormaPagamento#CREDITO} ou {@link FormaPagamento#DEBITO}
     * @param operadora comunicação com a operadora de cartão
     * @throws IllegalArgumentException se o tipo não for crédito nem débito
     */
    public PagamentoCartao(FormaPagamento tipo, ProcessadorCartao operadora) {
        if (tipo != FormaPagamento.CREDITO && tipo != FormaPagamento.DEBITO) {
            throw new IllegalArgumentException("Cartão é só crédito ou débito: " + tipo);
        }
        this.tipo = tipo;
        this.operadora = Objects.requireNonNull(operadora, "operadora");
    }

    @Override
    public FormaPagamento forma() {
        return tipo;
    }

    /**
     * @param total         valor a cobrar
     * @param valorRecebido ignorado: o cartão cobra o valor exato
     * @return o total, sem troco, com o código de autorização
     * @throws PagamentoRecusadoException se a operadora recusar
     */
    @Override
    public ResultadoPagamento processar(BigDecimal total, BigDecimal valorRecebido)
            throws PagamentoRecusadoException {
        ProcessadorCartao.Autorizacao autorizacao = operadora.autorizar(total, tipo);
        if (!autorizacao.aprovada()) {
            throw new PagamentoRecusadoException(autorizacao.motivo());
        }
        return new ResultadoPagamento(total, BigDecimal.ZERO.setScale(2), autorizacao.codigo());
    }
}
