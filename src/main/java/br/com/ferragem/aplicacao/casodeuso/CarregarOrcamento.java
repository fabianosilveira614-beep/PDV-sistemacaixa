package br.com.ferragem.aplicacao.casodeuso;

import br.com.ferragem.aplicacao.porta.ConsultaProdutos;
import br.com.ferragem.aplicacao.porta.OrcamentoRepository;
import br.com.ferragem.dominio.excecao.EstoqueInsuficienteException;
import br.com.ferragem.dominio.excecao.QuantidadeInvalidaException;
import br.com.ferragem.dominio.modelo.cliente.Cliente;
import br.com.ferragem.dominio.modelo.orcamento.Orcamento;
import br.com.ferragem.dominio.modelo.produto.Produto;
import br.com.ferragem.dominio.modelo.venda.Venda;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Monta um carrinho novo a partir de um orçamento guardado, para o cliente que voltou para
 * comprar. Só monta: nada é vendido, baixado ou cobrado até o operador fechar a venda no F9, que
 * segue o fluxo normal (e confere estoque de novo).
 *
 * <p><b>Regras:</b> valem os preços de hoje (o orçamento não tem validade; um preço antigo poderia
 * ficar abaixo do custo atual); produto fora de linha ou sem estoque suficiente fica de fora;
 * cliente excluído do cadastro não volta. Cada uma dessas situações vira um {@link Aviso} para o
 * operador conferir com o cliente.</p>
 */
public class CarregarOrcamento {

    /** O que mudou desde o orçamento. */
    public enum TipoAviso {
        /** O preço de hoje é outro; o carrinho usa o de hoje. */
        PRECO_MUDOU,
        /** Produto inativado depois do orçamento: ficou de fora. */
        SAIU_DE_LINHA,
        /** Não há estoque para a quantidade orçada: ficou de fora. */
        SEM_ESTOQUE,
        /** O cliente do orçamento foi excluído do cadastro: o carrinho ficou sem cliente. */
        CLIENTE_EXCLUIDO
    }

    /**
     * Uma diferença entre o orçamento e hoje.
     *
     * @param tipo  o que aconteceu
     * @param nome  produto ou cliente envolvido
     * @param antes preço no orçamento (preço) ou quantidade orçada (estoque); nulo nos demais
     * @param agora preço de hoje (preço) ou estoque atual (estoque); nulo nos demais
     */
    public record Aviso(TipoAviso tipo, String nome, BigDecimal antes, BigDecimal agora) {
    }

    /**
     * @param orcamento o orçamento carregado
     * @param carrinho  venda em aberto pronta para o caixa (pode ficar vazia se nada pôde entrar)
     * @param avisos    diferenças a conferir
     */
    public record Resultado(Orcamento orcamento, Venda carrinho, List<Aviso> avisos) {
    }

    private final OrcamentoRepository orcamentos;
    private final ConsultaProdutos produtos;

    /**
     * @param orcamentos orçamentos guardados
     * @param produtos   produtos ativos, com preço e estoque de hoje
     */
    public CarregarOrcamento(OrcamentoRepository orcamentos, ConsultaProdutos produtos) {
        this.orcamentos = Objects.requireNonNull(orcamentos);
        this.produtos = Objects.requireNonNull(produtos);
    }

    /**
     * @param orcamentoId número do orçamento
     * @return o carrinho montado e os avisos
     * @throws IllegalArgumentException se o orçamento não existir (a tela só oferece os da lista)
     */
    public Resultado executar(long orcamentoId) {
        Orcamento orcamento = orcamentos.buscarPorId(orcamentoId).orElseThrow(() ->
                new IllegalArgumentException("Orçamento " + orcamentoId + " não encontrado."));
        Venda carrinho = new Venda();
        List<Aviso> avisos = new ArrayList<>();

        for (Orcamento.Item item : orcamento.getItens()) {
            String nome = item.produto().getNome();
            Optional<Produto> hoje = produtos.buscarPorCodigo(item.produto().getCodigo());
            if (hoje.isEmpty()) {
                avisos.add(new Aviso(TipoAviso.SAIU_DE_LINHA, nome, null, null));
                continue;
            }
            Produto produto = hoje.get();
            try {
                carrinho.adicionarItem(produto, item.quantidade()); // usa o preço de hoje
            } catch (EstoqueInsuficienteException e) {
                avisos.add(new Aviso(TipoAviso.SEM_ESTOQUE, nome, item.quantidade(), produto.getQuantidadeEstoque()));
                continue;
            } catch (QuantidadeInvalidaException e) {
                // a unidade de um produto não muda depois do cadastro: a quantidade orçada continua válida
                throw new IllegalStateException("Quantidade do orçamento inválida para \"" + nome + "\".", e);
            }
            if (produto.getPrecoVenda().compareTo(item.precoUnitario()) != 0) {
                avisos.add(new Aviso(TipoAviso.PRECO_MUDOU, nome, item.precoUnitario(), produto.getPrecoVenda()));
            }
        }

        Optional<Cliente> cliente = orcamento.getCliente();
        if (cliente.isPresent()) {
            if (cliente.get().estaAtivo()) {
                carrinho.identificarCliente(cliente.get());
            } else {
                avisos.add(new Aviso(TipoAviso.CLIENTE_EXCLUIDO, cliente.get().getNome(), null, null));
            }
        }
        return new Resultado(orcamento, carrinho, List.copyOf(avisos));
    }
}
