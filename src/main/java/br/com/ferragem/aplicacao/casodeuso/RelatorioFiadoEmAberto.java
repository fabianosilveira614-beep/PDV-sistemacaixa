package br.com.ferragem.aplicacao.casodeuso;

import br.com.ferragem.aplicacao.porta.DebitoRepository;
import br.com.ferragem.dominio.excecao.PeriodoInvalidoException;
import br.com.ferragem.dominio.modelo.fiado.Debito;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Relatório "Fiado em aberto": das compras fiado <b>feitas no período</b>, as que ainda não foram
 * quitadas, da mais antiga para a mais nova (a mais antiga é a mais urgente de cobrar).
 *
 * <p>O que é "em aberto" é decidido pelo domínio ({@link Debito#estaPendente()}): o mesmo critério
 * da conta do cliente e do pagamento.</p>
 */
public class RelatorioFiadoEmAberto {

    /**
     * Uma compra em aberto.
     *
     * @param debito       a compra fiado, com cliente e pagamentos
     * @param diasEmAberto dias desde a compra até hoje
     */
    public record Linha(Debito debito, long diasEmAberto) {
    }

    /**
     * @param linhas      compras em aberto, da mais antiga para a mais nova
     * @param totalDevido soma dos saldos
     */
    public record Resultado(List<Linha> linhas, BigDecimal totalDevido) {
    }

    private final DebitoRepository debitos;
    private final Clock relogio;

    /**
     * @param debitos compras fiado
     * @param relogio fonte do "hoje", para contar os dias em aberto
     */
    public RelatorioFiadoEmAberto(DebitoRepository debitos, Clock relogio) {
        this.debitos = Objects.requireNonNull(debitos);
        this.relogio = Objects.requireNonNull(relogio);
    }

    /**
     * @param inicio primeiro dia da compra (inclusive)
     * @param fim    último dia da compra (inclusive)
     * @return compras em aberto e o total devido
     * @throws PeriodoInvalidoException se o início for depois do fim
     */
    public Resultado executar(LocalDate inicio, LocalDate fim) throws PeriodoInvalidoException {
        Periodo.validar(inicio, fim);
        LocalDate hoje = LocalDate.now(relogio);
        List<Linha> linhas = debitos.buscarPorPeriodoDeCompra(inicio, fim).stream()
                .filter(Debito::estaPendente)
                .sorted(Comparator.comparing(Debito::getCriadoEm))
                .map(d -> new Linha(d, ChronoUnit.DAYS.between(d.getCriadoEm().toLocalDate(), hoje)))
                .toList();
        BigDecimal total = linhas.stream().map(l -> l.debito().calcularSaldo())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new Resultado(linhas, total);
    }
}
