package br.com.ferragem.aplicacao.casodeuso;

import br.com.ferragem.aplicacao.porta.GerenciadorTransacao;
import br.com.ferragem.aplicacao.porta.MovimentoEstoqueRepository;
import br.com.ferragem.aplicacao.porta.ProdutoRepository;
import br.com.ferragem.dominio.excecao.RegistroDuplicadoException;
import br.com.ferragem.dominio.excecao.RegraNegocioException;
import br.com.ferragem.dominio.modelo.produto.FabricaProduto;
import br.com.ferragem.dominio.modelo.produto.MovimentoEstoque;
import br.com.ferragem.dominio.modelo.produto.Produto;
import br.com.ferragem.dominio.modelo.produto.TipoMovimentoEstoque;
import br.com.ferragem.dominio.modelo.produto.UnidadeMedida;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Cadastra um produto. Código e código de barras não podem repetir (RN03). Se houver estoque
 * inicial, ele entra como mercadoria recebida e fica registrado na trilha de estoque.
 */
public class CadastrarProduto {

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
    public CadastrarProduto(ProdutoRepository produtos, MovimentoEstoqueRepository movimentos,
                            GerenciadorTransacao transacao, Clock relogio) {
        this.produtos = Objects.requireNonNull(produtos);
        this.movimentos = Objects.requireNonNull(movimentos);
        this.transacao = Objects.requireNonNull(transacao);
        this.relogio = Objects.requireNonNull(relogio);
    }

    /**
     * Dados do formulário de cadastro.
     *
     * @param unidade        unidade de venda (define o tipo do produto)
     * @param codigo         código interno
     * @param codigoBarras   código de barras, opcional
     * @param nome           nome
     * @param precoCusto     preço de custo
     * @param precoVenda     preço de venda
     * @param estoqueInicial quantidade já na loja; nulo ou zero se ainda não chegou
     * @param estoqueMinimo  a partir de quanto repor; nulo se o produto não tem controle de reposição
     */
    public record Comando(UnidadeMedida unidade, String codigo, String codigoBarras, String nome,
                          BigDecimal precoCusto, BigDecimal precoVenda, BigDecimal estoqueInicial,
                          BigDecimal estoqueMinimo) {

        /**
         * Cadastro sem estoque mínimo (produto sem controle de reposição).
         *
         * @param unidade        unidade de venda
         * @param codigo         código interno
         * @param codigoBarras   código de barras, opcional
         * @param nome           nome
         * @param precoCusto     preço de custo
         * @param precoVenda     preço de venda
         * @param estoqueInicial quantidade já na loja, opcional
         */
        public Comando(UnidadeMedida unidade, String codigo, String codigoBarras, String nome,
                       BigDecimal precoCusto, BigDecimal precoVenda, BigDecimal estoqueInicial) {
            this(unidade, codigo, codigoBarras, nome, precoCusto, precoVenda, estoqueInicial, null);
        }
    }

    /**
     * @param comando dados do produto
     * @return o produto cadastrado, já com id
     * @throws RegraNegocioException dado obrigatório vazio, preço inválido, estoque inicial incompatível
     *                               com a unidade, código ou código de barras já usados
     */
    public Produto executar(Comando comando) throws RegraNegocioException {
        Produto produto = FabricaProduto.criar(comando.unidade(), comando.codigo(), comando.codigoBarras(),
                comando.nome(), comando.precoCusto(), comando.precoVenda());
        boolean temEstoqueInicial = comando.estoqueInicial() != null && comando.estoqueInicial().signum() != 0;
        if (temEstoqueInicial) {
            produto.reporEstoque(comando.estoqueInicial());
        }
        produto.definirEstoqueMinimo(comando.estoqueMinimo());
        exigirCodigosLivres(produto);

        return transacao.executar(() -> {
            produtos.salvar(produto);
            if (temEstoqueInicial) {
                movimentos.registrar(new MovimentoEstoque(produto, TipoMovimentoEstoque.ENTRADA_COMPRA,
                        comando.estoqueInicial(), "estoque inicial", LocalDateTime.now(relogio), null));
            }
            return produto;
        });
    }

    private void exigirCodigosLivres(Produto produto) throws RegistroDuplicadoException {
        if (produtos.existeCodigo(produto.getCodigo(), null)) {
            throw new RegistroDuplicadoException("Já existe um produto com o código " + produto.getCodigo() + ".");
        }
        if (produto.getCodigoBarras().filter(barras -> produtos.existeCodigoBarras(barras, null)).isPresent()) {
            throw new RegistroDuplicadoException("Já existe um produto com este código de barras.");
        }
    }
}
