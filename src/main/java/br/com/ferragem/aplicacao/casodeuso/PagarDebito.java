package br.com.ferragem.aplicacao.casodeuso;

import br.com.ferragem.aplicacao.pagamento.ProcessadoresPagamento;
import br.com.ferragem.aplicacao.pagamento.ResultadoPagamento;
import br.com.ferragem.aplicacao.porta.CaixaRepository;
import br.com.ferragem.aplicacao.porta.DebitoRepository;
import br.com.ferragem.aplicacao.porta.GerenciadorTransacao;
import br.com.ferragem.dominio.excecao.CaixaFechadoException;
import br.com.ferragem.dominio.excecao.RegraNegocioException;
import br.com.ferragem.dominio.modelo.caixa.SessaoCaixa;
import br.com.ferragem.dominio.modelo.cliente.Cliente;
import br.com.ferragem.dominio.modelo.fiado.ContaFiado;
import br.com.ferragem.dominio.modelo.fiado.Debito;
import br.com.ferragem.dominio.modelo.fiado.PagamentoAplicado;
import br.com.ferragem.dominio.modelo.venda.FormaPagamento;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Recebe pagamento de compras fiado: das compras selecionadas na tela ou, sem seleção, de todas
 * as pendentes, da mais antiga para a mais nova (regra da {@link ContaFiado}). O dinheiro entra no
 * caixa aberto (RN08).
 *
 * <p>Como na venda, primeiro valida tudo (caixa aberto, valor dentro do devido, cartão aprovado)
 * e só depois aplica o pagamento e grava.</p>
 */
public class PagarDebito {

    private final DebitoRepository debitos;
    private final CaixaRepository caixas;
    private final ProcessadoresPagamento processadores;
    private final GerenciadorTransacao transacao;
    private final Clock relogio;

    /**
     * @param debitos       compras fiado
     * @param caixas        caixa aberto
     * @param processadores estratégias de pagamento
     * @param transacao     controle de transação
     * @param relogio       fonte do "agora"
     */
    public PagarDebito(DebitoRepository debitos, CaixaRepository caixas, ProcessadoresPagamento processadores,
                       GerenciadorTransacao transacao, Clock relogio) {
        this.debitos = Objects.requireNonNull(debitos);
        this.caixas = Objects.requireNonNull(caixas);
        this.processadores = Objects.requireNonNull(processadores);
        this.transacao = Objects.requireNonNull(transacao);
        this.relogio = Objects.requireNonNull(relogio);
    }

    /**
     * Pedido de pagamento, montado pela tela de fiado.
     *
     * @param cliente             cliente que está pagando
     * @param vendasSelecionadas  números das vendas marcadas; vazio = "pagar valor livre" (todas as pendentes)
     * @param valor               valor a abater
     * @param forma               dinheiro, Pix, crédito ou débito (nunca fiado)
     * @param valorRecebido       dinheiro entregue (só no dinheiro; pode ser nulo)
     */
    public record Comando(Cliente cliente, Set<Long> vendasSelecionadas, BigDecimal valor, FormaPagamento forma,
                          BigDecimal valorRecebido) {
        /** Exige os dados obrigatórios e proíbe pagar fiado com fiado. */
        public Comando {
            Objects.requireNonNull(cliente, "cliente");
            Objects.requireNonNull(forma, "forma");
            vendasSelecionadas = vendasSelecionadas == null ? Set.of() : Set.copyOf(vendasSelecionadas);
            if (forma == FormaPagamento.FIADO) {
                throw new IllegalArgumentException("Não se paga fiado com fiado.");
            }
        }
    }

    /**
     * Dados do comprovante de recebimento.
     *
     * @param aplicados         quanto caiu em cada compra
     * @param valorRecebido     quanto o cliente entregou
     * @param troco             quanto devolver
     * @param saldoDevedorFinal quanto o cliente ainda deve depois deste pagamento
     */
    public record Comprovante(List<PagamentoAplicado> aplicados, BigDecimal valorRecebido, BigDecimal troco,
                              BigDecimal saldoDevedorFinal) {
    }

    /**
     * Recebe o pagamento.
     *
     * @param comando dados do pagamento
     * @return comprovante com a distribuição e o saldo restante
     * @throws RegraNegocioException caixa fechado, valor inválido ou maior que o devido, dinheiro insuficiente
     *                               ou cartão recusado; nesses casos nenhuma compra é alterada
     */
    public Comprovante executar(Comando comando) throws RegraNegocioException {
        return transacao.executar(() -> receber(comando));
    }

    private Comprovante receber(Comando comando) throws RegraNegocioException {
        SessaoCaixa caixa = caixas.buscarAberta().orElseThrow(CaixaFechadoException::new);
        ContaFiado conta = new ContaFiado(comando.cliente(), debitos.buscarPorCliente(comando.cliente()));
        List<Debito> escolhidas = comando.vendasSelecionadas().isEmpty()
                ? conta.getPendentes()
                : conta.selecionarPorVenda(comando.vendasSelecionadas());

        conta.verificarPagamento(escolhidas, comando.valor());
        ResultadoPagamento pagamento = processadores.para(comando.forma())
                .processar(comando.valor(), comando.valorRecebido());

        LocalDateTime agora = LocalDateTime.now(relogio);
        List<PagamentoAplicado> aplicados = conta.pagarSelecionadas(escolhidas, comando.valor(), comando.forma(), agora);
        aplicados.forEach(aplicado -> debitos.salvar(aplicado.debito()));

        caixa.registrarRecebimentoFiado(comando.forma(), comando.valor(), "fiado " + comando.cliente().getNome(), agora);
        caixas.salvar(caixa);

        return new Comprovante(aplicados, pagamento.valorRecebido(), pagamento.troco(), conta.calcularSaldoDevedor());
    }
}
