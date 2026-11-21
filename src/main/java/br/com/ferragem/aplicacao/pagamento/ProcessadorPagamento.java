package br.com.ferragem.aplicacao.pagamento;

import br.com.ferragem.dominio.excecao.RegraNegocioException;
import br.com.ferragem.dominio.modelo.venda.FormaPagamento;

import java.math.BigDecimal;

/**
 * Como receber o dinheiro em uma forma de pagamento (padrão <i>Strategy</i>).
 *
 * <p>Cada forma tem a sua implementação. Os casos de uso pedem o processador da forma escolhida
 * e chamam {@link #processar}, sem nenhum {@code if (forma == ...)}. Uma forma nova (ex.:
 * vale-presente) entra criando uma classe, sem alterar quem usa: princípio aberto/fechado.</p>
 *
 * <p><b>Contrato (substituição de Liskov):</b> toda implementação devolve um
 * {@link ResultadoPagamento} válido ou lança uma {@link RegraNegocioException} com mensagem para
 * o usuário. Nenhuma lança {@code UnsupportedOperationException} nem devolve nulo.</p>
 */
public interface ProcessadorPagamento {

    /** @return forma de pagamento que esta estratégia atende */
    FormaPagamento forma();

    /**
     * Recebe o valor.
     *
     * @param total         valor a pagar
     * @param valorRecebido valor entregue pelo cliente (só faz diferença no dinheiro; pode ser nulo nas demais)
     * @return valor recebido, troco e autorização
     * @throws RegraNegocioException se o pagamento não puder ser concluído (dinheiro insuficiente, cartão recusado)
     */
    ResultadoPagamento processar(BigDecimal total, BigDecimal valorRecebido) throws RegraNegocioException;
}
