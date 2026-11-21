package br.com.ferragem.aplicacao.pagamento;

import br.com.ferragem.dominio.modelo.venda.FormaPagamento;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;

/**
 * Catálogo das estratégias de pagamento disponíveis, montado uma vez na inicialização.
 * Os casos de uso pedem a estratégia pela forma escolhida.
 */
public class ProcessadoresPagamento {

    private final Map<FormaPagamento, ProcessadorPagamento> porForma = new EnumMap<>(FormaPagamento.class);

    /**
     * @param processadores uma estratégia para cada forma aceita pela loja
     * @throws IllegalArgumentException se duas estratégias atenderem a mesma forma
     */
    public ProcessadoresPagamento(Collection<? extends ProcessadorPagamento> processadores) {
        for (ProcessadorPagamento processador : processadores) {
            if (porForma.putIfAbsent(processador.forma(), processador) != null) {
                throw new IllegalArgumentException("Duas estratégias para " + processador.forma());
            }
        }
    }

    /**
     * @param forma forma escolhida
     * @return a estratégia dessa forma
     * @throws IllegalArgumentException se a loja não aceitar essa forma
     */
    public ProcessadorPagamento para(FormaPagamento forma) {
        ProcessadorPagamento processador = porForma.get(forma);
        if (processador == null) {
            throw new IllegalArgumentException("Forma de pagamento não configurada: " + forma);
        }
        return processador;
    }
}
