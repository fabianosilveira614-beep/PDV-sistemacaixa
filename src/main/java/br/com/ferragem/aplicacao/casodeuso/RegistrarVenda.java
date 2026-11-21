package br.com.ferragem.aplicacao.casodeuso;

import br.com.ferragem.aplicacao.pagamento.ProcessadoresPagamento;
import br.com.ferragem.aplicacao.pagamento.ResultadoPagamento;
import br.com.ferragem.aplicacao.porta.CaixaRepository;
import br.com.ferragem.aplicacao.porta.DebitoRepository;
import br.com.ferragem.aplicacao.porta.GerenciadorTransacao;
import br.com.ferragem.aplicacao.porta.MovimentoEstoqueRepository;
import br.com.ferragem.aplicacao.porta.ProdutoRepository;
import br.com.ferragem.aplicacao.porta.RegistroVendas;
import br.com.ferragem.dominio.excecao.CaixaFechadoException;
import br.com.ferragem.dominio.excecao.EstoqueInsuficienteException;
import br.com.ferragem.dominio.excecao.ProdutoIndisponivelException;
import br.com.ferragem.dominio.excecao.RegraNegocioException;
import br.com.ferragem.dominio.modelo.caixa.Operador;
import br.com.ferragem.dominio.modelo.caixa.SessaoCaixa;
import br.com.ferragem.dominio.modelo.fiado.Debito;
import br.com.ferragem.dominio.modelo.produto.MovimentoEstoque;
import br.com.ferragem.dominio.modelo.produto.Produto;
import br.com.ferragem.dominio.modelo.venda.FormaPagamento;
import br.com.ferragem.dominio.modelo.venda.ItemVenda;
import br.com.ferragem.dominio.modelo.venda.Venda;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Conclui uma venda no caixa: confere caixa, estoque e pagamento; depois grava a venda, baixa o
 * estoque e registra o dinheiro no caixa ou a dívida do fiado, <b>tudo numa transação</b> (RN07).
 *
 * <p><b>Ordem importa:</b> primeiro tudo o que pode falhar (caixa fechado, estoque que acabou
 * enquanto o cliente estava no balcão, dinheiro insuficiente, cartão recusado); só depois o que
 * altera dados. Assim uma recusa nunca deixa estoque baixado sem venda.</p>
 *
 * <p><b>O carrinho recebido nunca é alterado:</b> quem é finalizada e gravada é uma cópia
 * ({@link Venda#copiar()}). Se o banco falhar no meio (queda de conexão), a transação desfaz o
 * que foi gravado e o carrinho da tela continua em aberto, com tudo o que foi bipado, para o
 * operador tentar de novo.</p>
 *
 * <p>Depende só de interfaces (portas), recebidas no construtor: não sabe se o banco é
 * PostgreSQL ou uma lista em memória (inversão de dependência).</p>
 */
public class RegistrarVenda {

    private final ProdutoRepository produtos;
    private final MovimentoEstoqueRepository movimentosEstoque;
    private final RegistroVendas vendas;
    private final DebitoRepository debitos;
    private final CaixaRepository caixas;
    private final ProcessadoresPagamento processadores;
    private final GerenciadorTransacao transacao;
    private final Clock relogio;

    /**
     * @param produtos          leitura do estoque atual e gravação da baixa
     * @param movimentosEstoque trilha de movimentações de estoque
     * @param vendas            gravação da venda
     * @param debitos           gravação da dívida, nas vendas fiado
     * @param caixas            caixa aberto
     * @param processadores     estratégias de pagamento
     * @param transacao         controle de transação
     * @param relogio           fonte do "agora" (fixa nos testes)
     */
    public RegistrarVenda(ProdutoRepository produtos, MovimentoEstoqueRepository movimentosEstoque,
                          RegistroVendas vendas, DebitoRepository debitos, CaixaRepository caixas,
                          ProcessadoresPagamento processadores, GerenciadorTransacao transacao, Clock relogio) {
        this.produtos = Objects.requireNonNull(produtos);
        this.movimentosEstoque = Objects.requireNonNull(movimentosEstoque);
        this.vendas = Objects.requireNonNull(vendas);
        this.debitos = Objects.requireNonNull(debitos);
        this.caixas = Objects.requireNonNull(caixas);
        this.processadores = Objects.requireNonNull(processadores);
        this.transacao = Objects.requireNonNull(transacao);
        this.relogio = Objects.requireNonNull(relogio);
    }

    /**
     * Pedido de conclusão de venda, montado pela tela do PDV.
     *
     * @param venda         carrinho montado pelo operador, ainda em aberto; não é alterado
     * @param forma         forma de pagamento escolhida
     * @param valorRecebido dinheiro entregue pelo cliente (só no pagamento em dinheiro; pode ser nulo)
     * @param operador      quem está vendendo
     */
    public record Comando(Venda venda, FormaPagamento forma, BigDecimal valorRecebido, Operador operador) {
        /** Exige os dados obrigatórios. */
        public Comando {
            Objects.requireNonNull(venda, "venda");
            Objects.requireNonNull(forma, "forma");
            Objects.requireNonNull(operador, "operador");
        }
    }

    /**
     * Dados para o comprovante e para a tela de "venda concluída".
     *
     * @param vendaId           número da venda
     * @param total             total da venda
     * @param forma             forma de pagamento
     * @param valorRecebido     quanto o cliente entregou
     * @param troco             quanto devolver
     * @param codigoAutorizacao código do cartão, vazio nas demais formas
     */
    public record Comprovante(long vendaId, BigDecimal total, FormaPagamento forma, BigDecimal valorRecebido,
                              BigDecimal troco, String codigoAutorizacao) {
    }

    /**
     * Conclui a venda.
     *
     * @param comando dados da venda
     * @return comprovante com número da venda e troco
     * @throws RegraNegocioException venda vazia, fiado sem cliente, caixa fechado, estoque insuficiente,
     *                               produto retirado de venda, dinheiro insuficiente ou cartão recusado;
     *                               nesses casos nada é gravado
     *                               e a venda continua em aberto
     */
    public Comprovante executar(Comando comando) throws RegraNegocioException {
        comando.venda().verificarPodeFinalizar(comando.forma());
        return transacao.executar(() -> registrar(comando));
    }

    private Comprovante registrar(Comando comando) throws RegraNegocioException {
        Venda venda = comando.venda().copiar();
        BigDecimal total = venda.calcularTotal();

        // 1. Tudo o que pode falhar, sem alterar nada
        SessaoCaixa caixa = caixas.buscarAberta().orElseThrow(CaixaFechadoException::new);
        List<Baixa> baixas = conferirEstoqueAtual(venda);
        ResultadoPagamento pagamento = processadores.para(comando.forma()).processar(total, comando.valorRecebido());

        // 2. Daqui em diante nada mais falha por regra de negócio
        LocalDateTime agora = LocalDateTime.now(relogio);
        venda.finalizar(comando.forma(), agora);
        vendas.salvar(venda, caixa, comando.operador(), pagamento);
        long vendaId = venda.getId().orElseThrow(() ->
                new IllegalStateException("O repositório não atribuiu número à venda."));

        baixarEstoque(baixas, vendaId, agora);
        registrarFinanceiro(venda, caixa, vendaId, total, agora);

        return new Comprovante(vendaId, total, comando.forma(), pagamento.valorRecebido(), pagamento.troco(),
                pagamento.codigoAutorizacao());
    }

    /** Produto com o estoque lido agora do banco e a quantidade a baixar. */
    private record Baixa(Produto produtoAtual, BigDecimal quantidade) {
    }

    /**
     * Relê cada produto do repositório, porque o estoque pode ter mudado desde que o item entrou no
     * carrinho (outra venda, ajuste de estoque, produto retirado de venda). Confere tudo antes de
     * baixar qualquer coisa.
     */
    private List<Baixa> conferirEstoqueAtual(Venda venda)
            throws EstoqueInsuficienteException, ProdutoIndisponivelException {
        List<Baixa> baixas = new ArrayList<>();
        for (ItemVenda item : venda.getItens()) {
            Produto atual = produtos.buscarPorCodigo(item.getProduto().getCodigo()).orElseThrow(() ->
                    new ProdutoIndisponivelException(item.getProduto().getNome()));
            if (!atual.temEstoqueSuficiente(item.getQuantidade())) {
                throw new EstoqueInsuficienteException(atual.getNome(), atual.getQuantidadeEstoque(),
                        item.getQuantidade());
            }
            baixas.add(new Baixa(atual, item.getQuantidade()));
        }
        return baixas;
    }

    private void baixarEstoque(List<Baixa> baixas, long vendaId, LocalDateTime agora) throws RegraNegocioException {
        for (Baixa baixa : baixas) {
            baixa.produtoAtual().baixarEstoque(baixa.quantidade());
            produtos.salvar(baixa.produtoAtual());
            movimentosEstoque.registrar(
                    MovimentoEstoque.saidaPorVenda(baixa.produtoAtual(), baixa.quantidade(), vendaId, agora));
        }
    }

    /** Venda fiado vira dívida; as demais entram no caixa. */
    private void registrarFinanceiro(Venda venda, SessaoCaixa caixa, long vendaId, BigDecimal total,
                                     LocalDateTime agora) throws CaixaFechadoException {
        FormaPagamento forma = venda.getFormaPagamento().orElseThrow();
        if (forma == FormaPagamento.FIADO) {
            debitos.salvar(Debito.daVendaFiado(venda));
        } else {
            caixa.registrarVenda(forma, total, "venda #" + vendaId, agora);
            caixas.salvar(caixa);
        }
    }
}
