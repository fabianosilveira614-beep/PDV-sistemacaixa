package br.com.ferragem.dominio.modelo.venda;

import br.com.ferragem.dominio.comum.Decimais;
import br.com.ferragem.dominio.excecao.EstoqueInsuficienteException;
import br.com.ferragem.dominio.excecao.FiadoNaoPermitidoException;
import br.com.ferragem.dominio.excecao.OperacaoNaoAutorizadaException;
import br.com.ferragem.dominio.excecao.PrecoInvalidoException;
import br.com.ferragem.dominio.excecao.QuantidadeInvalidaException;
import br.com.ferragem.dominio.excecao.VendaVaziaException;
import br.com.ferragem.dominio.modelo.caixa.PerfilOperador;
import br.com.ferragem.dominio.modelo.cliente.Cliente;
import br.com.ferragem.dominio.modelo.produto.Produto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static br.com.ferragem.dominio.comum.Decimais.CASAS_DINHEIRO;

/**
 * Uma venda no caixa: o carrinho com seus itens, o cliente (opcional) e, ao final, a forma de
 * pagamento.
 *
 * <p>A venda é a "dona" dos seus itens: toda inclusão, alteração ou remoção passa por ela, que
 * valida as regras antes de mudar qualquer coisa. Regras garantidas aqui: RN01 (fiado só com
 * cliente ativo), RN04 (não vender mais que o estoque, somando os itens do mesmo produto),
 * RN05 (venda vazia), RN06 (quantidade conforme o tipo do produto) e RN13 (preço alterado).</p>
 *
 * <p><b>Polimorfismo na prática:</b> o carrinho guarda produtos de tipos diferentes
 * (unidade, quilo, metro) e chama {@link Produto#validarQuantidade} sem saber qual é o tipo.
 * Cada produto aplica a sua própria regra.</p>
 *
 * <p>Esta classe <b>não</b> baixa o estoque nem grava nada: ao finalizar, só muda de estado.
 * Baixar estoque, registrar o caixa e gerar o débito do fiado, tudo numa transação, é tarefa
 * do caso de uso {@code RegistrarVenda}.</p>
 */
public class Venda {

    private final List<ItemVenda> itens = new ArrayList<>();
    private Long id;
    private Cliente cliente;
    private FormaPagamento formaPagamento;
    private LocalDateTime dataHora;
    private StatusVenda status = StatusVenda.EM_ABERTO;

    /** Abre uma venda vazia, sem cliente. */
    public Venda() {
        // carrinho vazio e status EM_ABERTO, definidos nos próprios atributos
    }

    // ---------------------------------------------------------------- cliente

    /**
     * Identifica o cliente da venda. Obrigatório para vender fiado.
     *
     * @param cliente cliente cadastrado; não pode ser nulo
     * @throws IllegalStateException se a venda já foi finalizada
     */
    public void identificarCliente(Cliente cliente) {
        exigirEmAberto();
        this.cliente = Objects.requireNonNull(cliente, "cliente");
    }

    /**
     * Volta a venda para "sem cliente".
     *
     * @throws IllegalStateException se a venda já foi finalizada
     */
    public void removerCliente() {
        exigirEmAberto();
        this.cliente = null;
    }

    // ---------------------------------------------------------------- itens

    /**
     * Adiciona um produto ao carrinho. Se o produto já estiver no carrinho, soma a quantidade na
     * mesma linha, em vez de criar outra (cada leitura do código de barras soma 1).
     *
     * @param produto    produto a vender
     * @param quantidade quantidade a acrescentar, na unidade do produto
     * @throws QuantidadeInvalidaException  se a quantidade não for válida para o tipo do produto (RN06)
     * @throws EstoqueInsuficienteException se a quantidade total no carrinho ultrapassar o estoque (RN04);
     *                                      o carrinho não é alterado
     * @throws IllegalStateException        se a venda já foi finalizada
     */
    public void adicionarItem(Produto produto, BigDecimal quantidade)
            throws QuantidadeInvalidaException, EstoqueInsuficienteException {
        exigirEmAberto();
        Objects.requireNonNull(produto, "produto");
        produto.validarQuantidade(quantidade);

        Optional<ItemVenda> existente = buscarItem(produto.getCodigo());
        BigDecimal quantidadeTotal = existente
                .map(item -> item.getQuantidade().add(quantidade))
                .orElse(quantidade);
        exigirEstoque(produto, quantidadeTotal);

        if (existente.isPresent()) {
            existente.get().alterarQuantidade(quantidadeTotal);
        } else {
            itens.add(new ItemVenda(produto, quantidade, produto.getPrecoVenda()));
        }
    }

    /**
     * Troca a quantidade de um item que já está no carrinho.
     *
     * @param codigoProduto  código do produto no carrinho
     * @param novaQuantidade nova quantidade total desse produto
     * @throws QuantidadeInvalidaException  se a quantidade não for válida para o tipo do produto
     * @throws EstoqueInsuficienteException se a nova quantidade ultrapassar o estoque
     * @throws IllegalArgumentException     se o produto não estiver no carrinho
     * @throws IllegalStateException        se a venda já foi finalizada
     */
    public void alterarQuantidade(String codigoProduto, BigDecimal novaQuantidade)
            throws QuantidadeInvalidaException, EstoqueInsuficienteException {
        exigirEmAberto();
        ItemVenda item = exigirItem(codigoProduto);
        item.getProduto().validarQuantidade(novaQuantidade);
        exigirEstoque(item.getProduto(), novaQuantidade);
        item.alterarQuantidade(novaQuantidade);
    }

    /**
     * Altera o preço unitário de um item na hora da venda (desconto negociado no balcão).
     *
     * @param codigoProduto código do produto no carrinho
     * @param novoPreco     preço unitário; maior que zero e até 2 casas decimais
     * @param perfil        perfil de quem está alterando; preço abaixo do custo exige ADMINISTRADOR (RN13)
     * @throws PrecoInvalidoException         se o preço for zero, negativo ou tiver mais de 2 casas
     * @throws OperacaoNaoAutorizadaException se o preço ficar abaixo do custo e o perfil não puder autorizar
     * @throws IllegalArgumentException       se o produto não estiver no carrinho
     * @throws IllegalStateException          se a venda já foi finalizada
     */
    public void alterarPrecoUnitario(String codigoProduto, BigDecimal novoPreco, PerfilOperador perfil)
            throws PrecoInvalidoException, OperacaoNaoAutorizadaException {
        exigirEmAberto();
        Objects.requireNonNull(perfil, "perfil");
        ItemVenda item = exigirItem(codigoProduto);

        if (novoPreco == null || novoPreco.signum() <= 0) {
            throw new PrecoInvalidoException("O preço unitário deve ser maior que zero.");
        }
        if (Decimais.casasDecimais(novoPreco) > CASAS_DINHEIRO) {
            throw new PrecoInvalidoException("O preço unitário aceita no máximo 2 casas decimais (centavos).");
        }
        BigDecimal custo = item.getProduto().getPrecoCusto();
        if (novoPreco.compareTo(custo) < 0 && !perfil.podeVenderAbaixoDoCusto()) {
            throw new OperacaoNaoAutorizadaException("Preço abaixo do custo de \"" + item.getProduto().getNome()
                    + "\" (R$ " + custo.toPlainString() + ") só com autorização de um administrador.");
        }
        item.alterarPrecoUnitario(novoPreco.setScale(CASAS_DINHEIRO));
    }

    /**
     * Tira um produto do carrinho.
     *
     * @param codigoProduto código do produto no carrinho
     * @throws IllegalArgumentException se o produto não estiver no carrinho
     * @throws IllegalStateException    se a venda já foi finalizada
     */
    public void removerItem(String codigoProduto) {
        exigirEmAberto();
        itens.remove(exigirItem(codigoProduto));
    }

    /**
     * Soma dos subtotais de todos os itens.
     *
     * @return total da venda, com 2 casas decimais; zero se o carrinho estiver vazio
     */
    public BigDecimal calcularTotal() {
        return itens.stream()
                .map(ItemVenda::calcularSubtotal)
                .reduce(BigDecimal.ZERO.setScale(CASAS_DINHEIRO, RoundingMode.UNNECESSARY), BigDecimal::add);
    }

    // ---------------------------------------------------------------- finalização

    /**
     * Confere se a venda pode ser finalizada com esta forma de pagamento, <b>sem alterar nada</b>.
     *
     * <p>Existe separado de {@link #finalizar} para o caso de uso validar tudo antes de mexer em
     * estoque e caixa: se algo falhar depois, a venda continua em aberto na tela do operador.</p>
     *
     * @param forma forma de pagamento escolhida
     * @throws VendaVaziaException        se o carrinho estiver vazio (RN05)
     * @throws FiadoNaoPermitidoException se a forma for FIADO sem cliente ou com cliente inativo (RN01)
     * @throws IllegalStateException      se a venda já foi finalizada
     */
    public void verificarPodeFinalizar(FormaPagamento forma) throws VendaVaziaException, FiadoNaoPermitidoException {
        exigirEmAberto();
        Objects.requireNonNull(forma, "forma");
        if (itens.isEmpty()) {
            throw new VendaVaziaException();
        }
        if (forma.exigeCliente()) {
            exigirClienteAptoAoFiado();
        }
    }

    /**
     * Conclui a venda: a partir daqui ela não pode mais ser alterada.
     *
     * @param forma   forma de pagamento escolhida
     * @param momento data e hora da conclusão (recebida de fora para os testes controlarem o "agora")
     * @throws VendaVaziaException        se o carrinho estiver vazio (RN05)
     * @throws FiadoNaoPermitidoException se a forma for FIADO sem cliente ou com cliente inativo (RN01)
     * @throws IllegalStateException      se a venda já foi finalizada
     */
    public void finalizar(FormaPagamento forma, LocalDateTime momento)
            throws VendaVaziaException, FiadoNaoPermitidoException {
        verificarPodeFinalizar(forma);
        Objects.requireNonNull(momento, "momento");
        this.formaPagamento = forma;
        this.dataHora = momento;
        this.status = StatusVenda.FINALIZADA;
    }

    /**
     * Cria uma venda nova, em aberto, com o mesmo cliente e os mesmos itens (quantidades e preços,
     * inclusive descontos já autorizados).
     *
     * <p>Usada pelo caso de uso {@code RegistrarVenda}: ele finaliza e grava a <b>cópia</b>, nunca o
     * carrinho que está na tela. Se o banco falhar no meio da gravação, a transação é desfeita e o
     * carrinho continua intacto e em aberto, pronto para o operador tentar de novo.</p>
     *
     * @return cópia independente desta venda
     * @throws IllegalStateException se a venda já foi finalizada
     */
    public Venda copiar() {
        exigirEmAberto();
        Venda copia = new Venda();
        itens.forEach(item -> copia.itens.add(
                new ItemVenda(item.getProduto(), item.getQuantidade(), item.getPrecoUnitario())));
        copia.cliente = this.cliente;
        return copia;
    }

    /**
     * Dados de um item como foram gravados no momento da venda.
     *
     * @param produto        produto vendido
     * @param quantidade     quantidade vendida
     * @param precoUnitario  preço cobrado (pode diferir do preço atual do produto)
     */
    public record ItemRegistrado(Produto produto, BigDecimal quantidade, BigDecimal precoUnitario) {
        /** Exige os três dados. */
        public ItemRegistrado {
            Objects.requireNonNull(produto, "produto");
            Objects.requireNonNull(quantidade, "quantidade");
            Objects.requireNonNull(precoUnitario, "precoUnitario");
        }
    }

    /**
     * Reconstrói uma venda <b>já finalizada</b> a partir do que está gravado no banco.
     *
     * <p>Uso exclusivo da camada de persistência (histórico do cliente). As regras de
     * <i>criação</i> não são reaplicadas de propósito: o estoque e o preço de custo de hoje não
     * dizem nada sobre uma venda de meses atrás. Um desconto abaixo do custo autorizado por um
     * administrador naquela época continua válido. A venda devolvida está finalizada e,
     * portanto, imutável.</p>
     *
     * @param id       número da venda
     * @param cliente  cliente identificado, ou {@code null}
     * @param forma    forma de pagamento usada
     * @param dataHora momento da finalização
     * @param itens    itens como foram vendidos; não pode ser vazio
     * @return venda finalizada
     */
    public static Venda restaurar(long id, Cliente cliente, FormaPagamento forma, LocalDateTime dataHora,
                                  List<ItemRegistrado> itens) {
        Objects.requireNonNull(forma, "forma");
        Objects.requireNonNull(dataHora, "dataHora");
        if (itens == null || itens.isEmpty()) {
            throw new IllegalArgumentException("Venda #" + id + " gravada sem itens.");
        }
        Venda venda = new Venda();
        itens.forEach(item -> venda.itens.add(
                new ItemVenda(item.produto(), item.quantidade(), item.precoUnitario())));
        venda.cliente = cliente;
        venda.formaPagamento = forma;
        venda.dataHora = dataHora;
        venda.status = StatusVenda.FINALIZADA;
        venda.atribuirId(id);
        return venda;
    }

    /**
     * Registra o identificador gerado pelo banco. Chamado uma única vez pelo repositório.
     *
     * @param idGerado identificador positivo
     * @throws IllegalStateException    se a venda já tiver um id
     * @throws IllegalArgumentException se o id não for positivo
     */
    public void atribuirId(long idGerado) {
        if (this.id != null) {
            throw new IllegalStateException("A venda já possui id " + this.id + ".");
        }
        if (idGerado <= 0) {
            throw new IllegalArgumentException("O id deve ser positivo: " + idGerado);
        }
        this.id = idGerado;
    }

    // ---------------------------------------------------------------- consultas

    /**
     * @return cópia somente leitura dos itens; alterar a lista devolvida lança exceção, e
     *         nenhuma alteração afeta a venda
     */
    public List<ItemVenda> getItens() {
        return List.copyOf(itens);
    }

    /** @return {@code true} se o carrinho não tem itens */
    public boolean estaVazia() {
        return itens.isEmpty();
    }

    /** @return id gerado pelo banco, ou vazio se ainda não foi salva */
    public Optional<Long> getId() {
        return Optional.ofNullable(id);
    }

    /** @return cliente identificado, se houver */
    public Optional<Cliente> getCliente() {
        return Optional.ofNullable(cliente);
    }

    /** @return forma de pagamento; vazio enquanto a venda está em aberto */
    public Optional<FormaPagamento> getFormaPagamento() {
        return Optional.ofNullable(formaPagamento);
    }

    /** @return momento da finalização; vazio enquanto a venda está em aberto */
    public Optional<LocalDateTime> getDataHora() {
        return Optional.ofNullable(dataHora);
    }

    /** @return situação atual da venda */
    public StatusVenda getStatus() {
        return status;
    }

    // ---------------------------------------------------------------- internos

    private void exigirEmAberto() {
        if (status != StatusVenda.EM_ABERTO) {
            throw new IllegalStateException("A venda já foi finalizada e não pode ser alterada.");
        }
    }

    private void exigirClienteAptoAoFiado() throws FiadoNaoPermitidoException {
        if (cliente == null) {
            throw new FiadoNaoPermitidoException("identifique um cliente cadastrado antes de vender fiado.");
        }
        if (!cliente.estaAtivo()) {
            throw new FiadoNaoPermitidoException("o cliente \"" + cliente.getNome() + "\" está inativo.");
        }
    }

    private static void exigirEstoque(Produto produto, BigDecimal quantidade) throws EstoqueInsuficienteException {
        if (!produto.temEstoqueSuficiente(quantidade)) {
            throw new EstoqueInsuficienteException(produto.getNome(), produto.getQuantidadeEstoque(), quantidade);
        }
    }

    private Optional<ItemVenda> buscarItem(String codigoProduto) {
        return itens.stream().filter(item -> item.ehDoProduto(codigoProduto)).findFirst();
    }

    private ItemVenda exigirItem(String codigoProduto) {
        return buscarItem(codigoProduto).orElseThrow(() ->
                new IllegalArgumentException("O produto " + codigoProduto + " não está no carrinho."));
    }
}
