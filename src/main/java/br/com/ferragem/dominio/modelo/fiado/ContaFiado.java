package br.com.ferragem.dominio.modelo.fiado;

import br.com.ferragem.dominio.comum.Decimais;
import br.com.ferragem.dominio.excecao.DebitoJaQuitadoException;
import br.com.ferragem.dominio.excecao.ValorPagamentoInvalidoException;
import br.com.ferragem.dominio.modelo.cliente.Cliente;
import br.com.ferragem.dominio.modelo.venda.FormaPagamento;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import static br.com.ferragem.dominio.comum.Decimais.CASAS_DINHEIRO;

/**
 * O "caderno de fiado" de um cliente: todas as suas compras fiado, o total devido e as formas de
 * pagar. Corresponde à tela de fiado do cliente.
 *
 * <p>Regras garantidas aqui:</p>
 * <ul>
 *   <li>RN11: o saldo devedor é a soma dos saldos das compras pendentes, sempre calculado;</li>
 *   <li>um recebimento pode quitar uma, várias ou todas as compras; o valor é aplicado
 *       <b>da compra mais antiga para a mais nova</b>, só entre as compras escolhidas;</li>
 *   <li>o recebimento não pode passar da soma dos saldos escolhidos (o excedente em dinheiro é
 *       troco, não crédito).</li>
 * </ul>
 */
public class ContaFiado {

    private static final Comparator<Debito> MAIS_ANTIGA_PRIMEIRO =
            Comparator.comparing(Debito::getCriadoEm).thenComparingLong(Debito::getVendaId);

    private final Cliente cliente;
    private final List<Debito> debitos;

    /**
     * @param cliente  dono da conta
     * @param debitos  todas as compras fiado do cliente (pendentes e quitadas)
     */
    public ContaFiado(Cliente cliente, Collection<Debito> debitos) {
        this.cliente = Objects.requireNonNull(cliente, "cliente");
        this.debitos = new ArrayList<>(Objects.requireNonNull(debitos, "debitos"));
        this.debitos.sort(MAIS_ANTIGA_PRIMEIRO);
    }

    /** @return total devido pelo cliente (RN11) */
    public BigDecimal calcularSaldoDevedor() {
        return debitos.stream()
                .map(Debito::calcularSaldo)
                .reduce(BigDecimal.ZERO.setScale(CASAS_DINHEIRO), BigDecimal::add);
    }

    /** @return compras com saldo a pagar, da mais antiga para a mais nova */
    public List<Debito> getPendentes() {
        return debitos.stream().filter(Debito::estaPendente).toList();
    }

    /** @return compras já quitadas (histórico), da mais antiga para a mais nova */
    public List<Debito> getQuitadas() {
        return debitos.stream().filter(debito -> !debito.estaPendente()).toList();
    }

    /**
     * Botão "Pagar selecionadas": aplica o valor nas compras escolhidas, da mais antiga para a
     * mais nova. Exemplo: compras de R$ 100 e R$ 80 selecionadas e R$ 150 pagos → a primeira é
     * quitada e a segunda fica com saldo de R$ 30.
     *
     * @param selecionadas compras escolhidas; todas desta conta e pendentes
     * @param valor        valor recebido; maior que zero e no máximo a soma dos saldos escolhidos
     * @param forma        forma de pagamento (dinheiro, Pix, crédito ou débito)
     * @param momento      data e hora do recebimento
     * @return quanto caiu em cada compra, na ordem em que foi aplicado
     * @throws ValorPagamentoInvalidoException se o valor for inválido ou maior que a soma dos saldos;
     *                                         nenhuma compra é alterada
     * @throws DebitoJaQuitadoException        nunca na prática: compras quitadas são recusadas antes
     * @throws IllegalArgumentException        se a seleção estiver vazia, tiver compra de outra conta
     *                                         ou compra já quitada
     */
    public List<PagamentoAplicado> pagarSelecionadas(Collection<Debito> selecionadas, BigDecimal valor,
                                                     FormaPagamento forma, LocalDateTime momento)
            throws ValorPagamentoInvalidoException, DebitoJaQuitadoException {
        List<Debito> escolhidas = verificarPagamento(selecionadas, valor);

        List<PagamentoAplicado> aplicados = new ArrayList<>();
        BigDecimal restante = valor;
        for (Debito debito : escolhidas) {
            if (restante.signum() == 0) {
                break;
            }
            BigDecimal parte = restante.min(debito.calcularSaldo());
            aplicados.add(new PagamentoAplicado(debito, debito.registrarPagamento(parte, forma, momento)));
            restante = restante.subtract(parte);
        }
        return aplicados;
    }

    /**
     * Botão "Pagar valor livre": o cliente entrega um valor sem escolher compra; ele é aplicado
     * em todas as pendentes, da mais antiga para a mais nova.
     *
     * @param valor   valor recebido
     * @param forma   forma de pagamento
     * @param momento data e hora do recebimento
     * @return quanto caiu em cada compra
     * @throws ValorPagamentoInvalidoException se o valor for inválido ou maior que o total devido
     * @throws DebitoJaQuitadoException        nunca na prática
     * @throws IllegalArgumentException        se não houver compras pendentes
     */
    public List<PagamentoAplicado> pagarValorLivre(BigDecimal valor, FormaPagamento forma, LocalDateTime momento)
            throws ValorPagamentoInvalidoException, DebitoJaQuitadoException {
        return pagarSelecionadas(getPendentes(), valor, forma, momento);
    }

    /**
     * Botão "Quitar tudo": paga o total devido.
     *
     * @param forma   forma de pagamento
     * @param momento data e hora do recebimento
     * @return quanto caiu em cada compra
     * @throws ValorPagamentoInvalidoException nunca na prática: o valor é exatamente o saldo devedor
     * @throws DebitoJaQuitadoException        nunca na prática
     * @throws IllegalArgumentException        se não houver compras pendentes
     */
    public List<PagamentoAplicado> quitarTudo(FormaPagamento forma, LocalDateTime momento)
            throws ValorPagamentoInvalidoException, DebitoJaQuitadoException {
        return pagarValorLivre(calcularSaldoDevedor(), forma, momento);
    }

    /**
     * Confere se um pagamento é possível, <b>sem alterar nada</b>. O caso de uso chama antes de
     * cobrar o cartão, para nunca cobrar um valor que depois seria recusado.
     *
     * @param selecionadas compras escolhidas
     * @param valor        valor a pagar
     * @return as compras escolhidas, da mais antiga para a mais nova (ordem em que o valor será aplicado)
     * @throws ValorPagamentoInvalidoException se o valor for inválido ou maior que a soma dos saldos
     * @throws IllegalArgumentException        se a seleção for vazia, de outra conta ou tiver compra quitada
     */
    public List<Debito> verificarPagamento(Collection<Debito> selecionadas, BigDecimal valor)
            throws ValorPagamentoInvalidoException {
        List<Debito> escolhidas = validarSelecao(selecionadas);
        validarValor(valor, somarSaldos(escolhidas));
        return escolhidas;
    }

    /**
     * Encontra as compras desta conta pelo número da venda (é o que a tela envia).
     *
     * @param vendaIds números das vendas selecionadas
     * @return as compras correspondentes
     * @throws IllegalArgumentException se algum número não for de uma compra desta conta
     */
    public List<Debito> selecionarPorVenda(Collection<Long> vendaIds) {
        List<Debito> encontradas = new ArrayList<>();
        for (Long vendaId : vendaIds) {
            encontradas.add(debitos.stream()
                    .filter(debito -> debito.getVendaId() == vendaId)
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "A venda #" + vendaId + " não é uma compra fiado deste cliente.")));
        }
        return encontradas;
    }

    /** @return dono da conta */
    public Cliente getCliente() {
        return cliente;
    }

    // ---------------------------------------------------------------- internos

    private List<Debito> validarSelecao(Collection<Debito> selecionadas) {
        if (selecionadas == null || selecionadas.isEmpty()) {
            throw new IllegalArgumentException("Selecione ao menos uma compra pendente.");
        }
        for (Debito debito : selecionadas) {
            if (debitos.stream().noneMatch(proprio -> proprio == debito)) {
                throw new IllegalArgumentException("A compra " + debito + " não pertence a esta conta.");
            }
            if (!debito.estaPendente()) {
                throw new IllegalArgumentException("A compra " + debito + " já está quitada.");
            }
        }
        return selecionadas.stream().distinct().sorted(MAIS_ANTIGA_PRIMEIRO).toList();
    }

    private static void validarValor(BigDecimal valor, BigDecimal saldoEscolhido)
            throws ValorPagamentoInvalidoException {
        if (valor == null || valor.signum() <= 0) {
            throw new ValorPagamentoInvalidoException("O valor do pagamento deve ser maior que zero.");
        }
        if (Decimais.casasDecimais(valor) > CASAS_DINHEIRO) {
            throw new ValorPagamentoInvalidoException("O valor aceita no máximo 2 casas decimais (centavos).");
        }
        if (valor.compareTo(saldoEscolhido) > 0) {
            throw new ValorPagamentoInvalidoException("O valor de R$ " + valor.toPlainString()
                    + " é maior que o total das compras escolhidas (R$ " + saldoEscolhido.toPlainString() + ").");
        }
    }

    private static BigDecimal somarSaldos(List<Debito> escolhidas) {
        return escolhidas.stream()
                .map(Debito::calcularSaldo)
                .reduce(BigDecimal.ZERO.setScale(CASAS_DINHEIRO), BigDecimal::add);
    }
}
