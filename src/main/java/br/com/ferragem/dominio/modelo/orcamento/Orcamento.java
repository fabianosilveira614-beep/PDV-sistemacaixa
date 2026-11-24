package br.com.ferragem.dominio.modelo.orcamento;

import br.com.ferragem.dominio.excecao.OrcamentoVazioException;
import br.com.ferragem.dominio.modelo.cliente.Cliente;
import br.com.ferragem.dominio.modelo.produto.Produto;
import br.com.ferragem.dominio.modelo.venda.ItemVenda;
import br.com.ferragem.dominio.modelo.venda.Venda;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static br.com.ferragem.dominio.comum.Decimais.CASAS_DINHEIRO;

/**
 * Orçamento: retrato do carrinho num momento, guardado para imprimir de novo ou carregar no caixa
 * depois. <b>Não é venda:</b> não baixa estoque, não entra no caixa, não gera fiado e não confere
 * estoque (o cliente pode pedir orçamento do que ainda vai chegar).
 *
 * <p>Os preços ficam como estavam no dia. Quando o orçamento volta para o carrinho, valem os preços
 * de hoje (regra do caso de uso {@code CarregarOrcamento}): o orçamento não tem validade, e um
 * preço antigo poderia ficar abaixo do custo atual.</p>
 */
public final class Orcamento {

    /**
     * Uma linha do orçamento.
     *
     * @param produto       produto orçado
     * @param quantidade    quantidade, na unidade do produto
     * @param precoUnitario preço no dia do orçamento
     */
    public record Item(Produto produto, BigDecimal quantidade, BigDecimal precoUnitario) {
        /** Exige os três dados. */
        public Item {
            Objects.requireNonNull(produto, "produto");
            Objects.requireNonNull(quantidade, "quantidade");
            Objects.requireNonNull(precoUnitario, "precoUnitario");
        }

        /** @return quantidade × preço, arredondado para centavos (mesma regra do item de venda) */
        public BigDecimal calcularSubtotal() {
            return quantidade.multiply(precoUnitario).setScale(CASAS_DINHEIRO, RoundingMode.HALF_UP);
        }
    }

    private Long id;
    private final LocalDateTime criadoEm;
    private final Cliente cliente;
    private final String atendente;
    private final List<Item> itens;

    private Orcamento(LocalDateTime criadoEm, Cliente cliente, String atendente, List<Item> itens) {
        this.criadoEm = Objects.requireNonNull(criadoEm, "criadoEm");
        this.cliente = cliente;
        this.atendente = Objects.requireNonNull(atendente, "atendente");
        this.itens = List.copyOf(itens);
    }

    /**
     * Tira o retrato do carrinho. O carrinho não é alterado.
     *
     * @param carrinho  venda em aberto no caixa
     * @param atendente nome de quem fez o orçamento
     * @param momento   data e hora do orçamento
     * @return o orçamento, ainda sem número
     * @throws OrcamentoVazioException se o carrinho estiver vazio
     */
    public static Orcamento doCarrinho(Venda carrinho, String atendente, LocalDateTime momento)
            throws OrcamentoVazioException {
        if (carrinho.estaVazia()) {
            throw new OrcamentoVazioException();
        }
        List<Item> itens = carrinho.getItens().stream()
                .map((ItemVenda i) -> new Item(i.getProduto(), i.getQuantidade(), i.getPrecoUnitario()))
                .toList();
        return new Orcamento(momento, carrinho.getCliente().orElse(null), atendente, itens);
    }

    /**
     * Reconstrói um orçamento gravado. Uso da camada de persistência.
     *
     * @param id        número
     * @param criadoEm  data e hora
     * @param cliente   cliente, ou {@code null}
     * @param atendente nome de quem fez
     * @param itens     itens como foram orçados (pode ficar vazio se o único produto foi apagado do cadastro)
     * @return o orçamento
     */
    public static Orcamento restaurar(long id, LocalDateTime criadoEm, Cliente cliente, String atendente,
                                      List<Item> itens) {
        Orcamento orcamento = new Orcamento(criadoEm, cliente, atendente, itens);
        orcamento.atribuirId(id);
        return orcamento;
    }

    /** @return soma dos subtotais, com os preços do dia do orçamento */
    public BigDecimal calcularTotal() {
        return itens.stream().map(Item::calcularSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(CASAS_DINHEIRO, RoundingMode.HALF_UP);
    }

    /**
     * Registra o número gerado pelo banco. Chamado uma única vez pelo repositório.
     *
     * @param idGerado número positivo
     * @throws IllegalStateException    se já tiver número
     * @throws IllegalArgumentException se o número não for positivo
     */
    public void atribuirId(long idGerado) {
        if (this.id != null) {
            throw new IllegalStateException("O orçamento já possui número " + this.id + ".");
        }
        if (idGerado <= 0) {
            throw new IllegalArgumentException("O número deve ser positivo: " + idGerado);
        }
        this.id = idGerado;
    }

    /** @return número do orçamento, ou vazio antes de gravar */
    public Optional<Long> getId() {
        return Optional.ofNullable(id);
    }

    /** @return data e hora do orçamento */
    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    /** @return cliente, se havia um identificado no carrinho */
    public Optional<Cliente> getCliente() {
        return Optional.ofNullable(cliente);
    }

    /** @return nome de quem fez o orçamento */
    public String getAtendente() {
        return atendente;
    }

    /** @return itens, na ordem do carrinho */
    public List<Item> getItens() {
        return itens;
    }
}
