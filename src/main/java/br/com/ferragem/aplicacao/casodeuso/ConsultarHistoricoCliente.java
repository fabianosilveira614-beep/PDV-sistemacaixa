package br.com.ferragem.aplicacao.casodeuso;

import br.com.ferragem.aplicacao.porta.HistoricoVendas;
import br.com.ferragem.dominio.excecao.PeriodoInvalidoException;
import br.com.ferragem.dominio.modelo.cliente.Cliente;
import br.com.ferragem.dominio.modelo.venda.Venda;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Histórico de compras de um cliente num período escolhido (data inicial e final).
 */
public class ConsultarHistoricoCliente {

    private final HistoricoVendas historico;

    /**
     * @param historico histórico de vendas
     */
    public ConsultarHistoricoCliente(HistoricoVendas historico) {
        this.historico = Objects.requireNonNull(historico);
    }

    /**
     * @param cliente cliente consultado
     * @param inicio  primeiro dia (inclusive)
     * @param fim     último dia (inclusive)
     * @return vendas do período, da mais recente para a mais antiga
     * @throws PeriodoInvalidoException se o início for depois do fim
     */
    public List<Venda> executar(Cliente cliente, LocalDate inicio, LocalDate fim) throws PeriodoInvalidoException {
        Objects.requireNonNull(cliente, "cliente");
        Periodo.validar(inicio, fim);
        return historico.buscarPorCliente(cliente, inicio, fim);
    }
}
