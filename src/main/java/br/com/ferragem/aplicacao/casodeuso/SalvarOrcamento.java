package br.com.ferragem.aplicacao.casodeuso;

import br.com.ferragem.aplicacao.porta.GerenciadorTransacao;
import br.com.ferragem.aplicacao.porta.OrcamentoRepository;
import br.com.ferragem.dominio.excecao.RegraNegocioException;
import br.com.ferragem.dominio.modelo.caixa.Operador;
import br.com.ferragem.dominio.modelo.orcamento.Orcamento;
import br.com.ferragem.dominio.modelo.venda.Venda;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Guarda o carrinho como orçamento (tecla F7 no caixa).
 *
 * <p><b>Por que não mexe em estoque nem em caixa:</b> esta classe só recebe o repositório de
 * orçamentos. Ela não tem como baixar estoque ou lançar dinheiro, porque não recebe quem faz
 * isso. A garantia está na própria montagem do objeto, não num "if".</p>
 */
public class SalvarOrcamento {

    private final OrcamentoRepository orcamentos;
    private final GerenciadorTransacao transacao;
    private final Clock relogio;

    /**
     * @param orcamentos orçamentos
     * @param transacao  o orçamento e seus itens são gravados juntos ou não são gravados
     * @param relogio    fonte do "agora"
     */
    public SalvarOrcamento(OrcamentoRepository orcamentos, GerenciadorTransacao transacao, Clock relogio) {
        this.orcamentos = Objects.requireNonNull(orcamentos);
        this.transacao = Objects.requireNonNull(transacao);
        this.relogio = Objects.requireNonNull(relogio);
    }

    /**
     * @param carrinho venda em aberto (não é alterada)
     * @param autor    operador logado
     * @return o orçamento gravado, já com número
     * @throws RegraNegocioException se o carrinho estiver vazio
     */
    public Orcamento executar(Venda carrinho, Operador autor) throws RegraNegocioException {
        Objects.requireNonNull(autor, "autor");
        Orcamento orcamento = Orcamento.doCarrinho(carrinho, autor.getNome(), LocalDateTime.now(relogio));
        return transacao.executar(() -> {
            orcamentos.salvar(orcamento, autor);
            return orcamento;
        });
    }
}
