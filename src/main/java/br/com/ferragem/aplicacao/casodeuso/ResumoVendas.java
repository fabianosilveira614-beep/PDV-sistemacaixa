package br.com.ferragem.aplicacao.casodeuso;

import br.com.ferragem.aplicacao.porta.HistoricoVendas.VendaDoPeriodo;
import br.com.ferragem.dominio.modelo.fiado.PagamentoDebito;
import br.com.ferragem.dominio.modelo.produto.Produto;
import br.com.ferragem.dominio.modelo.venda.FormaPagamento;
import br.com.ferragem.dominio.modelo.venda.ItemVenda;
import br.com.ferragem.dominio.modelo.venda.Venda;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static br.com.ferragem.dominio.comum.Decimais.CASAS_DINHEIRO;

/**
 * Números do relatório "Vendas do período". Todas as contas são feitas aqui, em Java, a partir das
 * vendas que o banco devolveu: assim cada número pode ser testado sem banco, com vendas montadas
 * no teste.
 *
 * <p><b>Vendido × entrou no caixa:</b> venda no fiado conta como vendida no dia da compra, mas o
 * dinheiro só entra quando o cliente paga. Por isso o resumo separa o {@link #getTotalVendido()
 * total vendido} do {@link #getEntrouNoCaixa() que entrou de fato} (vendas à vista + fiado
 * recebido no período, de compras de qualquer data).</p>
 *
 * <p><b>Lucro bruto:</b> preço cobrado menos o custo <i>do dia da venda</i>, gravado em cada item.
 * Vendas anteriores à gravação do custo não entram no lucro; o valor delas aparece em
 * {@link #getVendidoSemCusto()} para o leitor saber que o lucro não cobre tudo.</p>
 */
public final class ResumoVendas {

    /** Quantos produtos aparecem na lista de mais vendidos. */
    public static final int TAMANHO_RANKING = 10;

    /**
     * Um produto no ranking de mais vendidos.
     *
     * @param produto    produto
     * @param quantidade quanto saiu no período, na unidade do produto
     * @param valor      quanto foi vendido em dinheiro
     */
    public record ProdutoVendido(Produto produto, BigDecimal quantidade, BigDecimal valor) {
    }

    private final LocalDate inicio;
    private final LocalDate fim;
    private final List<VendaDoPeriodo> vendas;
    private final BigDecimal totalVendido;
    private final Map<FormaPagamento, BigDecimal> totalPorForma;
    private final BigDecimal recebidoDeFiado;
    private final BigDecimal lucroBruto;
    private final BigDecimal vendidoComCusto;
    private final List<ProdutoVendido> maisVendidos;

    private ResumoVendas(LocalDate inicio, LocalDate fim, List<VendaDoPeriodo> vendas, BigDecimal totalVendido,
                         Map<FormaPagamento, BigDecimal> totalPorForma, BigDecimal recebidoDeFiado,
                         BigDecimal lucroBruto, BigDecimal vendidoComCusto, List<ProdutoVendido> maisVendidos) {
        this.inicio = inicio;
        this.fim = fim;
        this.vendas = vendas;
        this.totalVendido = totalVendido;
        this.totalPorForma = totalPorForma;
        this.recebidoDeFiado = recebidoDeFiado;
        this.lucroBruto = lucroBruto;
        this.vendidoComCusto = vendidoComCusto;
        this.maisVendidos = maisVendidos;
    }

    /**
     * @param inicio           primeiro dia do período
     * @param fim              último dia do período
     * @param vendas           vendas finalizadas no período
     * @param recebidosDeFiado pagamentos de fiado recebidos no período
     * @return o resumo calculado
     */
    public static ResumoVendas calcular(LocalDate inicio, LocalDate fim, List<VendaDoPeriodo> vendas,
                                        List<PagamentoDebito> recebidosDeFiado) {
        Map<FormaPagamento, BigDecimal> porForma = new EnumMap<>(FormaPagamento.class);
        for (FormaPagamento forma : FormaPagamento.values()) {
            porForma.put(forma, BigDecimal.ZERO);
        }
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal lucro = BigDecimal.ZERO;
        BigDecimal comCusto = BigDecimal.ZERO;
        Map<String, ProdutoVendido> porProduto = new LinkedHashMap<>();

        for (VendaDoPeriodo registro : vendas) {
            Venda venda = registro.venda();
            BigDecimal totalDaVenda = venda.calcularTotal();
            total = total.add(totalDaVenda);
            porForma.merge(venda.getFormaPagamento().orElseThrow(), totalDaVenda, BigDecimal::add);
            for (ItemVenda item : venda.getItens()) {
                Optional<BigDecimal> lucroDoItem = item.calcularLucroBruto();
                if (lucroDoItem.isPresent()) {
                    lucro = lucro.add(lucroDoItem.get());
                    comCusto = comCusto.add(item.calcularSubtotal());
                }
                porProduto.merge(item.getProduto().getCodigo(),
                        new ProdutoVendido(item.getProduto(), item.getQuantidade(), item.calcularSubtotal()),
                        (a, b) -> new ProdutoVendido(a.produto(), a.quantidade().add(b.quantidade()),
                                a.valor().add(b.valor())));
            }
        }
        BigDecimal recebido = recebidosDeFiado.stream().map(PagamentoDebito::valor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        List<ProdutoVendido> ranking = porProduto.values().stream()
                .sorted(Comparator.comparing(ProdutoVendido::valor).reversed()
                        .thenComparing(p -> p.produto().getNome()))
                .limit(TAMANHO_RANKING)
                .toList();
        return new ResumoVendas(inicio, fim, List.copyOf(vendas), total, Collections.unmodifiableMap(porForma),
                recebido, lucro, comCusto, ranking);
    }

    /** @return quantas vendas houve */
    public int getQuantidadeVendas() {
        return vendas.size();
    }

    /** @return soma de todas as vendas, inclusive as no fiado */
    public BigDecimal getTotalVendido() {
        return totalVendido;
    }

    /** @return valor médio por venda (zero se não houve venda) */
    public BigDecimal getTicketMedio() {
        return vendas.isEmpty() ? BigDecimal.ZERO.setScale(CASAS_DINHEIRO)
                : totalVendido.divide(BigDecimal.valueOf(vendas.size()), CASAS_DINHEIRO, RoundingMode.HALF_UP);
    }

    /**
     * @param forma forma de pagamento
     * @return total vendido nessa forma (zero se nenhuma)
     */
    public BigDecimal getTotalPorForma(FormaPagamento forma) {
        return totalPorForma.get(forma);
    }

    /** @return pagamentos de fiado recebidos no período (de compras de qualquer data) */
    public BigDecimal getRecebidoDeFiado() {
        return recebidoDeFiado;
    }

    /** @return vendas à vista (tudo menos fiado) mais o fiado recebido: o dinheiro que entrou de fato */
    public BigDecimal getEntrouNoCaixa() {
        return totalVendido.subtract(totalPorForma.get(FormaPagamento.FIADO)).add(recebidoDeFiado);
    }

    /** @return lucro bruto dos itens que têm custo registrado */
    public BigDecimal getLucroBruto() {
        return lucroBruto;
    }

    /** @return quanto do total vendido tem custo registrado (base do lucro) */
    public BigDecimal getVendidoComCusto() {
        return vendidoComCusto;
    }

    /** @return quanto do total vendido não tem custo registrado (vendas antigas) */
    public BigDecimal getVendidoSemCusto() {
        return totalVendido.subtract(vendidoComCusto);
    }

    /** @return lucro ÷ vendido com custo × 100, ou vazio se nada tem custo */
    public Optional<BigDecimal> getMargemBruta() {
        return vendidoComCusto.signum() == 0 ? Optional.empty()
                : Optional.of(lucroBruto.multiply(BigDecimal.valueOf(100))
                .divide(vendidoComCusto, CASAS_DINHEIRO, RoundingMode.HALF_UP));
    }

    /** @return até {@value #TAMANHO_RANKING} produtos que mais venderam em dinheiro */
    public List<ProdutoVendido> getMaisVendidos() {
        return maisVendidos;
    }

    /** @return as vendas do período, da mais recente para a mais antiga */
    public List<VendaDoPeriodo> getVendas() {
        return vendas;
    }

    /** @return primeiro dia do período */
    public LocalDate getInicio() {
        return inicio;
    }

    /** @return último dia do período */
    public LocalDate getFim() {
        return fim;
    }
}
