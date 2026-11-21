package br.com.ferragem.aplicacao.pagamento;

import br.com.ferragem.dominio.modelo.venda.FormaPagamento;

import java.math.BigDecimal;

/**
 * Pagamento por Pix (simulado). A tela mostra a chave ou o QR Code e o operador confirma o
 * recebimento; quando este processador é chamado, o valor já foi confirmado.
 */
public class PagamentoPix implements ProcessadorPagamento {

    @Override
    public FormaPagamento forma() {
        return FormaPagamento.PIX;
    }

    /**
     * @param total         valor a pagar
     * @param valorRecebido ignorado: Pix é sempre o valor exato
     * @return o total, sem troco
     */
    @Override
    public ResultadoPagamento processar(BigDecimal total, BigDecimal valorRecebido) {
        return ResultadoPagamento.exato(total);
    }
}
