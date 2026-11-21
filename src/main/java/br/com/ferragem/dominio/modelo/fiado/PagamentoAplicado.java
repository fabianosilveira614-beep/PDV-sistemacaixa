package br.com.ferragem.dominio.modelo.fiado;

/**
 * Quanto de um recebimento caiu em cada compra fiado. Quando o cliente paga várias compras de
 * uma vez, a {@link ContaFiado} devolve um destes para cada compra afetada, para o caso de uso
 * gravar e o comprovante mostrar.
 *
 * @param debito    compra que recebeu parte do valor
 * @param pagamento parte do valor registrada nessa compra
 */
public record PagamentoAplicado(Debito debito, PagamentoDebito pagamento) {
}
