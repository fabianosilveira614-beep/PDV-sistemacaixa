package br.com.ferragem.dominio.modelo.fiado;

import br.com.ferragem.dominio.comum.Decimais;
import br.com.ferragem.dominio.excecao.DebitoJaQuitadoException;
import br.com.ferragem.dominio.excecao.ValorPagamentoInvalidoException;
import br.com.ferragem.dominio.modelo.cliente.Cliente;
import br.com.ferragem.dominio.modelo.venda.FormaPagamento;
import br.com.ferragem.dominio.modelo.venda.StatusVenda;
import br.com.ferragem.dominio.modelo.venda.Venda;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static br.com.ferragem.dominio.comum.Decimais.CASAS_DINHEIRO;

/**
 * Uma compra fiado: o quanto o cliente ficou devendo por uma venda e os pagamentos já feitos.
 *
 * <p>Nasce de uma venda finalizada com {@link FormaPagamento#FIADO} e continua existindo depois
 * de quitada, como histórico. O <b>valor pago</b>, o <b>saldo</b> e o <b>status</b> não são
 * guardados: são calculados a partir da lista de pagamentos, então nunca ficam inconsistentes
 * entre si.</p>
 *
 * <p>Regra garantida aqui: RN10 (pagamento maior que zero e no máximo igual ao saldo; compra
 * quitada não aceita pagamento).</p>
 */
public class Debito {

    private final Cliente cliente;
    private final long vendaId;
    private final BigDecimal valorOriginal;
    private final LocalDateTime criadoEm;
    private final List<PagamentoDebito> pagamentos = new ArrayList<>();
    private Long id;

    /**
     * Cria uma compra fiado sem pagamentos.
     *
     * @param cliente       cliente devedor
     * @param vendaId       id da venda que originou a dívida
     * @param valorOriginal valor da compra, maior que zero
     * @param criadoEm      momento da venda
     * @throws IllegalArgumentException se o valor não for positivo ou o id da venda não for válido
     */
    public Debito(Cliente cliente, long vendaId, BigDecimal valorOriginal, LocalDateTime criadoEm) {
        this.cliente = Objects.requireNonNull(cliente, "cliente");
        this.valorOriginal = Objects.requireNonNull(valorOriginal, "valorOriginal").setScale(CASAS_DINHEIRO);
        this.criadoEm = Objects.requireNonNull(criadoEm, "criadoEm");
        if (vendaId <= 0) {
            throw new IllegalArgumentException("Id de venda inválido: " + vendaId);
        }
        if (valorOriginal.signum() <= 0) {
            throw new IllegalArgumentException("O valor da compra fiado deve ser positivo: " + valorOriginal);
        }
        this.vendaId = vendaId;
    }

    /**
     * Gera a dívida de uma venda fiado já finalizada e salva.
     *
     * @param venda venda finalizada com forma FIADO, com cliente e id
     * @return dívida com o total da venda
     * @throws IllegalArgumentException se a venda não for uma venda fiado finalizada e salva
     */
    public static Debito daVendaFiado(Venda venda) {
        Objects.requireNonNull(venda, "venda");
        boolean fiadoFinalizado = venda.getStatus() == StatusVenda.FINALIZADA
                && venda.getFormaPagamento().filter(forma -> forma == FormaPagamento.FIADO).isPresent();
        if (!fiadoFinalizado) {
            throw new IllegalArgumentException("Só uma venda fiado finalizada gera dívida.");
        }
        long vendaId = venda.getId().orElseThrow(() ->
                new IllegalArgumentException("A venda precisa estar salva (com id) para gerar a dívida."));
        Cliente cliente = venda.getCliente().orElseThrow(() ->
                new IllegalArgumentException("Venda fiado sem cliente."));
        return new Debito(cliente, vendaId, venda.calcularTotal(), venda.getDataHora().orElseThrow());
    }

    /**
     * Registra um pagamento, total ou parcial (RN10).
     *
     * @param valor    valor pago; maior que zero, até 2 casas, no máximo igual ao saldo
     * @param forma    forma de pagamento (dinheiro, Pix, crédito ou débito)
     * @param momento  data e hora do pagamento
     * @return o pagamento registrado
     * @throws DebitoJaQuitadoException        se a compra já estiver quitada
     * @throws ValorPagamentoInvalidoException se o valor for inválido ou maior que o saldo; nada é alterado
     * @throws IllegalArgumentException        se a forma for FIADO
     */
    public PagamentoDebito registrarPagamento(BigDecimal valor, FormaPagamento forma, LocalDateTime momento)
            throws DebitoJaQuitadoException, ValorPagamentoInvalidoException {
        if (getStatus() == StatusDebito.QUITADO) {
            throw new DebitoJaQuitadoException(descricao());
        }
        if (valor == null || valor.signum() <= 0) {
            throw new ValorPagamentoInvalidoException("O valor do pagamento deve ser maior que zero.");
        }
        if (Decimais.casasDecimais(valor) > CASAS_DINHEIRO) {
            throw new ValorPagamentoInvalidoException("O valor aceita no máximo 2 casas decimais (centavos).");
        }
        BigDecimal saldo = calcularSaldo();
        if (valor.compareTo(saldo) > 0) {
            throw new ValorPagamentoInvalidoException("O valor de R$ " + valor.toPlainString()
                    + " é maior que o saldo de " + descricao() + " (R$ " + saldo.toPlainString() + ").");
        }
        PagamentoDebito pagamento = new PagamentoDebito(valor.setScale(CASAS_DINHEIRO), forma, momento);
        pagamentos.add(pagamento);
        return pagamento;
    }

    /** @return soma de todos os pagamentos feitos */
    public BigDecimal getValorPago() {
        return pagamentos.stream()
                .map(PagamentoDebito::valor)
                .reduce(BigDecimal.ZERO.setScale(CASAS_DINHEIRO), BigDecimal::add);
    }

    /** @return quanto ainda falta pagar: valor original menos o valor pago */
    public BigDecimal calcularSaldo() {
        return valorOriginal.subtract(getValorPago());
    }

    /** @return situação calculada a partir dos pagamentos */
    public StatusDebito getStatus() {
        if (calcularSaldo().signum() == 0) {
            return StatusDebito.QUITADO;
        }
        return pagamentos.isEmpty() ? StatusDebito.ABERTO : StatusDebito.PARCIAL;
    }

    /** @return {@code true} se ainda há saldo a pagar */
    public boolean estaPendente() {
        return getStatus() != StatusDebito.QUITADO;
    }

    /**
     * Registra o identificador gerado pelo banco. Chamado uma única vez pelo repositório.
     *
     * @param idGerado identificador positivo
     * @throws IllegalStateException    se o débito já tiver um id
     * @throws IllegalArgumentException se o id não for positivo
     */
    public void atribuirId(long idGerado) {
        if (this.id != null) {
            throw new IllegalStateException("O débito já possui id " + this.id + ".");
        }
        if (idGerado <= 0) {
            throw new IllegalArgumentException("O id deve ser positivo: " + idGerado);
        }
        this.id = idGerado;
    }

    /** @return id gerado pelo banco, ou vazio se ainda não foi salvo */
    public Optional<Long> getId() {
        return Optional.ofNullable(id);
    }

    /** @return cliente devedor */
    public Cliente getCliente() {
        return cliente;
    }

    /** @return id da venda que originou a dívida */
    public long getVendaId() {
        return vendaId;
    }

    /** @return valor da compra */
    public BigDecimal getValorOriginal() {
        return valorOriginal;
    }

    /** @return momento da compra; define a ordem de abatimento (mais antiga primeiro) */
    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    /** @return cópia somente leitura dos pagamentos, do mais antigo ao mais recente */
    public List<PagamentoDebito> getPagamentos() {
        return List.copyOf(pagamentos);
    }

    @Override
    public String toString() {
        return "Debito{venda #" + vendaId + ", saldo=" + calcularSaldo().toPlainString() + ", " + getStatus() + "}";
    }

    private String descricao() {
        return "venda #" + vendaId;
    }
}
