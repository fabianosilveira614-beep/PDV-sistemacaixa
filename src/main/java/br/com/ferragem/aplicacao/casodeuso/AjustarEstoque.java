package br.com.ferragem.aplicacao.casodeuso;

import br.com.ferragem.aplicacao.porta.GerenciadorTransacao;
import br.com.ferragem.aplicacao.porta.MovimentoEstoqueRepository;
import br.com.ferragem.aplicacao.porta.ProdutoRepository;
import br.com.ferragem.dominio.excecao.QuantidadeInvalidaException;
import br.com.ferragem.dominio.excecao.RegraNegocioException;
import br.com.ferragem.dominio.modelo.produto.MovimentoEstoque;
import br.com.ferragem.dominio.modelo.produto.Produto;
import br.com.ferragem.dominio.modelo.produto.TipoMovimentoEstoque;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Movimenta o estoque fora de uma venda: chegada de mercadoria, perda ou correção de contagem.
 * Toda movimentação fica registrada na trilha de estoque. O motivo é opcional (decisão do
 * grupo), mas recomendado em perdas e correções, que são onde mercadoria some sem explicação.
 */
public class AjustarEstoque {

    private final ProdutoRepository produtos;
    private final MovimentoEstoqueRepository movimentos;
    private final GerenciadorTransacao transacao;
    private final Clock relogio;

    /**
     * @param produtos   produtos
     * @param movimentos trilha de estoque
     * @param transacao  controle de transação
     * @param relogio    fonte do "agora"
     */
    public AjustarEstoque(ProdutoRepository produtos, MovimentoEstoqueRepository movimentos,
                          GerenciadorTransacao transacao, Clock relogio) {
        this.produtos = Objects.requireNonNull(produtos);
        this.movimentos = Objects.requireNonNull(movimentos);
        this.transacao = Objects.requireNonNull(transacao);
        this.relogio = Objects.requireNonNull(relogio);
    }

    /**
     * Dados do ajuste.
     *
     * @param produtoId  produto movimentado
     * @param tipo       ENTRADA_COMPRA e PERDA usam quantidade positiva; AJUSTE usa positiva para
     *                   acrescentar e negativa para retirar
     * @param quantidade quantidade, na unidade do produto
     * @param motivo     motivo, opcional (ex.: "nota fiscal 4521", "caixa caiu", "contagem")
     */
    public record Comando(long produtoId, TipoMovimentoEstoque tipo, BigDecimal quantidade, String motivo) {
        /** Exige tipo e quantidade; venda não é ajuste. */
        public Comando {
            Objects.requireNonNull(tipo, "tipo");
            Objects.requireNonNull(quantidade, "quantidade");
            if (tipo == TipoMovimentoEstoque.SAIDA_VENDA) {
                throw new IllegalArgumentException("Saída por venda acontece só pelo caixa, não por ajuste.");
            }
        }
    }

    /**
     * @param comando dados do ajuste
     * @return estoque do produto depois do ajuste
     * @throws RegraNegocioException quantidade inválida para o tipo do produto ou retirada
     *                               maior que o estoque; nesses casos nada é alterado
     * @throws IllegalArgumentException se o produto não existir
     */
    public BigDecimal executar(Comando comando) throws RegraNegocioException {
        Produto produto = produtos.buscarPorId(comando.produtoId()).orElseThrow(() ->
                new IllegalArgumentException("Produto " + comando.produtoId() + " não encontrado."));
        if (comando.tipo() != TipoMovimentoEstoque.AJUSTE && comando.quantidade().signum() <= 0) {
            throw new QuantidadeInvalidaException("Em entrada de mercadoria e em perda, informe uma quantidade "
                    + "positiva. Para corrigir a contagem para menos, use o tipo AJUSTE com quantidade negativa.");
        }
        BigDecimal efeito = comando.tipo() == TipoMovimentoEstoque.PERDA
                ? comando.quantidade().negate()
                : comando.quantidade();

        if (efeito.signum() > 0) {
            produto.reporEstoque(efeito);
        } else {
            produto.baixarEstoque(efeito.negate());
        }

        return transacao.executar(() -> {
            produtos.salvar(produto);
            movimentos.registrar(new MovimentoEstoque(produto, comando.tipo(), efeito, comando.motivo(),
                    LocalDateTime.now(relogio), null));
            return produto.getQuantidadeEstoque();
        });
    }
}
