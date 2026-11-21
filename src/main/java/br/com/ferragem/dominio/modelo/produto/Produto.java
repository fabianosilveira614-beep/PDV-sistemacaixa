package br.com.ferragem.dominio.modelo.produto;

import br.com.ferragem.dominio.comum.Decimais;
import br.com.ferragem.dominio.excecao.DadoObrigatorioException;
import br.com.ferragem.dominio.excecao.EstoqueInsuficienteException;
import br.com.ferragem.dominio.excecao.PrecoInvalidoException;
import br.com.ferragem.dominio.excecao.QuantidadeInvalidaException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

import static br.com.ferragem.dominio.comum.Decimais.CASAS_DINHEIRO;

/**
 * Produto vendido pela ferragem.
 *
 * <p><b>Abstração:</b> a classe é abstrata porque não existe produto "sem unidade" na loja. Todo
 * produto é vendido por unidade, por quilo ou por metro, e cada tipo é uma subclasse.</p>
 *
 * <p><b>Encapsulamento:</b> nenhum atributo é público e não há setters. O estoque só muda por
 * {@link #baixarEstoque} e {@link #reporEstoque}, e os preços só por {@link #alterarPrecos}. Todos
 * validam as regras antes de alterar qualquer coisa: se a operação é recusada, o objeto fica
 * exatamente como estava.</p>
 *
 * <p><b>Polimorfismo:</b> {@link #validarQuantidade} é igual para todos os tipos, mas delega a
 * {@link #verificarFracionamento}, que cada subclasse implementa do seu jeito. Quem usa um
 * {@code Produto} não precisa saber qual é o tipo concreto.</p>
 *
 * <p>Regras garantidas aqui: RN02 (margem), RN04 (estoque), RN06 (quantidade por tipo) e
 * RN15 (preços).</p>
 */
public abstract class Produto {

    private static final BigDecimal CEM = new BigDecimal("100");

    private Long id;
    private String codigo;
    private String codigoBarras;
    private String nome;
    private BigDecimal precoCusto;
    private BigDecimal precoVenda;
    private BigDecimal quantidadeEstoque;
    private BigDecimal estoqueMinimo;
    private boolean ativo;

    /**
     * Cria um produto válido, ativo e com estoque zero. Chamado pelas subclasses com {@code super(...)}.
     *
     * <p>O estoque não entra pelo construtor: mercadoria chega por {@link #reporEstoque}, que valida
     * a quantidade conforme o tipo do produto. Isso também evita chamar um método da subclasse
     * ({@link #verificarFracionamento}) antes de ela terminar de ser construída.</p>
     *
     * @param codigo       código interno, obrigatório e único na loja
     * @param codigoBarras código de barras (EAN); opcional, pode ser nulo ou vazio
     * @param nome         nome do produto, obrigatório
     * @param precoCusto   preço pago ao fornecedor; maior ou igual a zero, até 2 casas
     * @param precoVenda   preço cobrado do cliente; maior que zero, até 2 casas
     * @throws DadoObrigatorioException se código ou nome estiverem vazios
     * @throws PrecoInvalidoException   se algum preço violar a RN15
     */
    protected Produto(String codigo, String codigoBarras, String nome,
                      BigDecimal precoCusto, BigDecimal precoVenda)
            throws DadoObrigatorioException, PrecoInvalidoException {
        alterarIdentificacao(codigo, codigoBarras, nome);
        alterarPrecos(precoCusto, precoVenda);
        this.quantidadeEstoque = BigDecimal.ZERO;
        this.ativo = true;
    }

    // ------------------------------------------------------------------ regras de quantidade (RN06)

    /**
     * Confere se uma quantidade pode ser vendida ou movimentada para este produto.
     *
     * <p>É {@code final}: a parte comum (maior que zero) vale para todos os tipos e nenhuma
     * subclasse pode enfraquecê-la. Só o fracionamento muda, em {@link #verificarFracionamento}.</p>
     *
     * @param quantidade quantidade a validar
     * @throws QuantidadeInvalidaException se for nula, zero, negativa ou fracionada demais para a unidade
     */
    public final void validarQuantidade(BigDecimal quantidade) throws QuantidadeInvalidaException {
        if (quantidade == null) {
            throw new QuantidadeInvalidaException("Informe a quantidade de \"" + nome + "\".");
        }
        if (quantidade.signum() <= 0) {
            throw new QuantidadeInvalidaException(
                    "A quantidade de \"" + nome + "\" deve ser maior que zero.");
        }
        verificarFracionamento(quantidade);
    }

    /**
     * Regra específica de cada tipo: quantas casas decimais a quantidade pode ter.
     * Recebe sempre um valor maior que zero.
     *
     * @param quantidade quantidade positiva
     * @throws QuantidadeInvalidaException se o tipo não aceitar esse fracionamento
     */
    protected abstract void verificarFracionamento(BigDecimal quantidade) throws QuantidadeInvalidaException;

    /** @return unidade em que este produto é vendido */
    public abstract UnidadeMedida getUnidade();

    // ------------------------------------------------------------------ estoque (RN04)

    /**
     * Retira uma quantidade do estoque (venda, perda).
     *
     * @param quantidade quantidade a retirar
     * @throws QuantidadeInvalidaException  se a quantidade não for válida para o tipo do produto
     * @throws EstoqueInsuficienteException se a quantidade for maior que o estoque; nada é alterado
     */
    public final void baixarEstoque(BigDecimal quantidade)
            throws QuantidadeInvalidaException, EstoqueInsuficienteException {
        validarQuantidade(quantidade);
        if (!temEstoqueSuficiente(quantidade)) {
            throw new EstoqueInsuficienteException(nome, quantidadeEstoque, quantidade);
        }
        quantidadeEstoque = quantidadeEstoque.subtract(quantidade);
    }

    /**
     * Acrescenta uma quantidade ao estoque (chegada de mercadoria, correção).
     *
     * @param quantidade quantidade a acrescentar
     * @throws QuantidadeInvalidaException se a quantidade não for válida para o tipo do produto
     */
    public final void reporEstoque(BigDecimal quantidade) throws QuantidadeInvalidaException {
        validarQuantidade(quantidade);
        quantidadeEstoque = quantidadeEstoque.add(quantidade);
    }

    /**
     * @param quantidade quantidade desejada
     * @return {@code true} se houver estoque para atendê-la
     */
    public final boolean temEstoqueSuficiente(BigDecimal quantidade) {
        return quantidade.compareTo(quantidadeEstoque) <= 0;
    }

    // ------------------------------------------------------------------ preços (RN02, RN15)

    /**
     * Altera os dois preços juntos, porque a validade de um pode depender do outro no futuro
     * (ex.: proibir venda abaixo do custo).
     *
     * @param novoCusto preço de custo; maior ou igual a zero, até 2 casas decimais
     * @param novaVenda preço de venda; maior que zero, até 2 casas decimais
     * @throws PrecoInvalidoException se algum preço violar a RN15; nada é alterado
     */
    public final void alterarPrecos(BigDecimal novoCusto, BigDecimal novaVenda) throws PrecoInvalidoException {
        if (novoCusto == null || novaVenda == null) {
            throw new PrecoInvalidoException("Informe o preço de custo e o preço de venda.");
        }
        if (novoCusto.signum() < 0) {
            throw new PrecoInvalidoException("O preço de custo não pode ser negativo.");
        }
        if (novaVenda.signum() <= 0) {
            throw new PrecoInvalidoException("O preço de venda deve ser maior que zero.");
        }
        if (casasDecimais(novoCusto) > CASAS_DINHEIRO || casasDecimais(novaVenda) > CASAS_DINHEIRO) {
            throw new PrecoInvalidoException("Preços aceitam no máximo 2 casas decimais (centavos).");
        }
        this.precoCusto = novoCusto.setScale(CASAS_DINHEIRO);
        this.precoVenda = novaVenda.setScale(CASAS_DINHEIRO);
    }

    /**
     * Margem de lucro sobre o preço de venda (RN02): {@code (venda − custo) ÷ venda × 100}.
     *
     * <p>É calculada a cada chamada, nunca armazenada: assim nunca fica desatualizada quando
     * um preço muda. Exemplo: custo 6,00 e venda 10,00 resultam em 40,00%.</p>
     *
     * @return margem em porcentagem, com 2 casas decimais (arredondamento HALF_UP); pode ser
     *         negativa se o produto for vendido abaixo do custo
     */
    public final BigDecimal calcularMargem() {
        return precoVenda.subtract(precoCusto)
                .multiply(CEM)
                .divide(precoVenda, CASAS_DINHEIRO, RoundingMode.HALF_UP);
    }

    // ------------------------------------------------------------------ identificação e situação

    /**
     * Altera código, código de barras e nome.
     *
     * @param novoCodigo       código interno, obrigatório
     * @param novoCodigoBarras código de barras; nulo ou vazio remove o código
     * @param novoNome         nome, obrigatório
     * @throws DadoObrigatorioException se código ou nome estiverem vazios; nada é alterado
     */
    public final void alterarIdentificacao(String novoCodigo, String novoCodigoBarras, String novoNome)
            throws DadoObrigatorioException {
        String codigoValidado = exigirTexto(novoCodigo, "código");
        String nomeValidado = exigirTexto(novoNome, "nome do produto");
        this.codigo = codigoValidado;
        this.nome = nomeValidado;
        this.codigoBarras = (novoCodigoBarras == null || novoCodigoBarras.isBlank())
                ? null
                : novoCodigoBarras.trim();
    }

    /**
     * Tira o produto de linha (exclusão lógica, RN12). Ele some das buscas do PDV, mas continua
     * existindo para o histórico de vendas antigas.
     */
    public final void inativar() {
        this.ativo = false;
    }

    /**
     * Devolve à venda um produto inativado (por engano ou que voltou a ser vendido).
     */
    public final void reativar() {
        this.ativo = true;
    }

    /**
     * Define a partir de quanto o produto precisa ser reposto (relatório de estoque baixo).
     *
     * <p>Não é um limite fixo para todos porque 5 parafusos é nada, 5 kg de prego é razoável e 5
     * sacos de cimento é bastante: cada produto tem o seu. O mínimo segue a mesma regra de
     * fracionamento da unidade (não existe mínimo de 2,5 martelos).</p>
     *
     * @param minimo quantidade mínima desejada; {@code null} desliga o controle para este produto
     * @throws QuantidadeInvalidaException se o mínimo for zero, negativo ou fracionado demais para a unidade
     */
    public final void definirEstoqueMinimo(BigDecimal minimo) throws QuantidadeInvalidaException {
        if (minimo != null) {
            validarQuantidade(minimo);
        }
        this.estoqueMinimo = minimo;
    }

    /**
     * @return {@code true} se há mínimo definido e o estoque chegou nele ou abaixo dele
     */
    public final boolean precisaRepor() {
        return estoqueMinimo != null && quantidadeEstoque.compareTo(estoqueMinimo) <= 0;
    }

    /** @return estoque mínimo, se o produto tiver controle de reposição */
    public final Optional<BigDecimal> getEstoqueMinimo() {
        return Optional.ofNullable(estoqueMinimo);
    }

    /**
     * Registra o identificador gerado pelo banco. Chamado uma única vez pelo repositório,
     * logo após o INSERT.
     *
     * @param idGerado identificador positivo
     * @throws IllegalStateException    se o produto já tiver um id
     * @throws IllegalArgumentException se o id não for positivo
     */
    public final void atribuirId(long idGerado) {
        if (this.id != null) {
            throw new IllegalStateException("O produto já possui id " + this.id + ".");
        }
        if (idGerado <= 0) {
            throw new IllegalArgumentException("O id deve ser positivo: " + idGerado);
        }
        this.id = idGerado;
    }

    // ------------------------------------------------------------------ apoio às subclasses

    /**
     * Atalho para as subclasses: conta as casas decimais significativas de uma quantidade.
     *
     * @param valor número a analisar
     * @return quantidade de casas decimais, ignorando zeros no final
     * @see Decimais#casasDecimais(BigDecimal)
     */
    protected static int casasDecimais(BigDecimal valor) {
        return Decimais.casasDecimais(valor);
    }

    // ------------------------------------------------------------------ consultas

    /** @return id gerado pelo banco, ou vazio se o produto ainda não foi salvo */
    public Optional<Long> getId() {
        return Optional.ofNullable(id);
    }

    /** @return código interno */
    public String getCodigo() {
        return codigo;
    }

    /** @return código de barras, se houver */
    public Optional<String> getCodigoBarras() {
        return Optional.ofNullable(codigoBarras);
    }

    /** @return nome do produto */
    public String getNome() {
        return nome;
    }

    /** @return preço de custo, com 2 casas decimais */
    public BigDecimal getPrecoCusto() {
        return precoCusto;
    }

    /** @return preço de venda, com 2 casas decimais */
    public BigDecimal getPrecoVenda() {
        return precoVenda;
    }

    /** @return quantidade atual em estoque */
    public BigDecimal getQuantidadeEstoque() {
        return quantidadeEstoque;
    }

    /** @return {@code true} se o produto ainda é vendido */
    public boolean estaAtivo() {
        return ativo;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{codigo='" + codigo + "', nome='" + nome
                + "', estoque=" + quantidadeEstoque.toPlainString() + " " + getUnidade().getSigla() + "}";
    }

    // ------------------------------------------------------------------ internos

    private static String exigirTexto(String valor, String campo) throws DadoObrigatorioException {
        if (valor == null || valor.isBlank()) {
            throw new DadoObrigatorioException(campo);
        }
        return valor.trim();
    }
}
