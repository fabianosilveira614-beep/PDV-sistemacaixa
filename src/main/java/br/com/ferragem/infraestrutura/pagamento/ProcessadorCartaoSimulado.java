package br.com.ferragem.infraestrutura.pagamento;

import br.com.ferragem.aplicacao.porta.ProcessadorCartao;
import br.com.ferragem.dominio.modelo.venda.FormaPagamento;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Operadora de cartão simulada: aprova toda cobrança e gera um código de autorização sequencial.
 *
 * <p>Substitui a integração real com maquininha/TEF, que está fora do escopo do trabalho. Para usar
 * uma operadora real, basta outra implementação de {@link ProcessadorCartao}; nenhum caso de uso
 * muda.</p>
 */
public class ProcessadorCartaoSimulado implements ProcessadorCartao {

    private final AtomicLong sequencia = new AtomicLong();

    /**
     * @param valor valor a cobrar
     * @param tipo  crédito ou débito
     * @return autorização aprovada com código {@code SIM-000001}, {@code SIM-000002}...
     */
    @Override
    public Autorizacao autorizar(BigDecimal valor, FormaPagamento tipo) {
        return new Autorizacao(true, String.format("SIM-%06d", sequencia.incrementAndGet()), "");
    }
}
