package br.com.ferragem.aplicacao.pagamento;

import br.com.ferragem.dominio.modelo.venda.FormaPagamento;

import java.math.BigDecimal;

/**
 * Venda fiado: nenhum dinheiro é recebido agora. A dívida é criada pelo caso de uso
 * {@code RegistrarVenda}, e as regras de quem pode comprar fiado (RN01) ficam na {@code Venda}.
 */
public class PagamentoFiado implements ProcessadorPagamento {

    @Override
    public FormaPagamento forma() {
        return FormaPagamento.FIADO;
    }

    /**
     * @param total         valor da compra (vira dívida)
     * @param valorRecebido ignorado
     * @return recebido zero, troco zero
     */
    @Override
    public ResultadoPagamento processar(BigDecimal total, BigDecimal valorRecebido) {
        return ResultadoPagamento.exato(BigDecimal.ZERO.setScale(2));
    }
}
