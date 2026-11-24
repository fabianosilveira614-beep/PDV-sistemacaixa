package br.com.ferragem.aplicacao.casodeuso;

import br.com.ferragem.aplicacao.porta.DebitoRepository;
import br.com.ferragem.aplicacao.porta.HistoricoVendas;
import br.com.ferragem.dominio.excecao.PeriodoInvalidoException;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Relatório "Vendas do período": busca as vendas e os recebimentos de fiado do período e entrega
 * os números calculados por {@link ResumoVendas}.
 */
public class RelatorioVendas {

    private final HistoricoVendas historico;
    private final DebitoRepository debitos;

    /**
     * @param historico vendas já feitas
     * @param debitos   para os recebimentos de fiado
     */
    public RelatorioVendas(HistoricoVendas historico, DebitoRepository debitos) {
        this.historico = Objects.requireNonNull(historico);
        this.debitos = Objects.requireNonNull(debitos);
    }

    /**
     * @param inicio primeiro dia (inclusive)
     * @param fim    último dia (inclusive)
     * @return o resumo do período
     * @throws PeriodoInvalidoException se o início for depois do fim
     */
    public ResumoVendas executar(LocalDate inicio, LocalDate fim) throws PeriodoInvalidoException {
        Periodo.validar(inicio, fim);
        return ResumoVendas.calcular(inicio, fim, historico.buscarPorPeriodo(inicio, fim),
                debitos.buscarPagamentosNoPeriodo(inicio, fim));
    }
}
