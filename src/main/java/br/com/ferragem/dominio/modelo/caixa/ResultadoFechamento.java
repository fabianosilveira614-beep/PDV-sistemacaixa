package br.com.ferragem.dominio.modelo.caixa;

import br.com.ferragem.dominio.modelo.venda.FormaPagamento;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Relatório de fechamento do caixa (RN14).
 *
 * @param esperado               dinheiro que deveria estar na gaveta
 * @param contado                dinheiro que o operador contou
 * @param diferenca              contado − esperado (negativo = falta)
 * @param situacao               CONFERE, QUEBRA ou SOBRA
 * @param recebidoPorForma       quanto entrou de clientes em cada forma de pagamento, para conferir
 *                               com a maquininha e o extrato do Pix
 */
public record ResultadoFechamento(BigDecimal esperado, BigDecimal contado, BigDecimal diferenca,
                                  SituacaoFechamento situacao, Map<FormaPagamento, BigDecimal> recebidoPorForma) {

    /** Garante que o mapa de totais não possa ser alterado depois. */
    public ResultadoFechamento {
        recebidoPorForma = Map.copyOf(recebidoPorForma);
    }

    /**
     * Calcula a diferença e a situação a partir do esperado e do contado.
     *
     * @param esperado         dinheiro esperado na gaveta
     * @param contado          dinheiro contado
     * @param recebidoPorForma totais recebidos por forma
     * @return resultado completo
     */
    public static ResultadoFechamento calcular(BigDecimal esperado, BigDecimal contado,
                                               Map<FormaPagamento, BigDecimal> recebidoPorForma) {
        BigDecimal diferenca = contado.subtract(esperado);
        SituacaoFechamento situacao = switch (diferenca.signum()) {
            case -1 -> SituacaoFechamento.QUEBRA;
            case 1 -> SituacaoFechamento.SOBRA;
            default -> SituacaoFechamento.CONFERE;
        };
        return new ResultadoFechamento(esperado, contado, diferenca, situacao, recebidoPorForma);
    }
}
