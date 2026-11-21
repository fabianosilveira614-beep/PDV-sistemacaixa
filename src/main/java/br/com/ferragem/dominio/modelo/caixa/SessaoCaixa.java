package br.com.ferragem.dominio.modelo.caixa;

import br.com.ferragem.dominio.comum.Decimais;
import br.com.ferragem.dominio.excecao.CaixaFechadoException;
import br.com.ferragem.dominio.excecao.OperacaoNaoAutorizadaException;
import br.com.ferragem.dominio.excecao.SaldoCaixaInsuficienteException;
import br.com.ferragem.dominio.excecao.ValorCaixaInvalidoException;
import br.com.ferragem.dominio.modelo.venda.FormaPagamento;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static br.com.ferragem.dominio.comum.Decimais.CASAS_DINHEIRO;

/**
 * Um expediente de caixa: da abertura (com o dinheiro inicial da gaveta) até o fechamento
 * (com a conferência).
 *
 * <p>Regras garantidas aqui:</p>
 * <ul>
 *   <li>RN08: só um caixa aberto aceita movimentos; depois de fechado, nada mais entra;</li>
 *   <li>RN14: esperado = abertura + entradas em dinheiro + suprimentos − sangrias; a diferença
 *       para o contado define QUEBRA, SOBRA ou CONFERE;</li>
 *   <li>sangria não pode retirar mais dinheiro do que há na gaveta;</li>
 *   <li>só quem abriu ou um administrador pode fechar.</li>
 * </ul>
 *
 * <p>"Só um caixa aberto <i>no sistema</i>" depende de consultar o banco, então é garantido pelo
 * caso de uso {@code AbrirCaixa} e por um índice único no banco, não por esta classe.</p>
 */
public class SessaoCaixa {

    private final Operador operador;
    private final LocalDateTime abertoEm;
    private final BigDecimal valorAbertura;
    private final List<MovimentoCaixa> movimentos = new ArrayList<>();
    private Long id;
    private StatusCaixa status;
    private LocalDateTime fechadoEm;
    private BigDecimal valorContado;

    private SessaoCaixa(Operador operador, BigDecimal valorAbertura, LocalDateTime abertoEm) {
        this.operador = operador;
        this.valorAbertura = valorAbertura;
        this.abertoEm = abertoEm;
        this.status = StatusCaixa.ABERTO;
    }

    /**
     * Abre o caixa com o dinheiro que está na gaveta.
     *
     * @param operador      quem está abrindo
     * @param valorAbertura dinheiro inicial na gaveta; maior ou igual a zero
     * @param momento       data e hora da abertura
     * @return sessão aberta
     * @throws ValorCaixaInvalidoException se o valor for negativo ou tiver mais de 2 casas
     */
    public static SessaoCaixa abrir(Operador operador, BigDecimal valorAbertura, LocalDateTime momento)
            throws ValorCaixaInvalidoException {
        Objects.requireNonNull(operador, "operador");
        Objects.requireNonNull(momento, "momento");
        validarValor(valorAbertura, "valor de abertura", true);
        return new SessaoCaixa(operador, valorAbertura.setScale(CASAS_DINHEIRO), momento);
    }

    // ---------------------------------------------------------------- movimentos

    /**
     * Registra a entrada de uma venda paga. Venda fiado não passa por aqui.
     *
     * @param forma     forma de pagamento (dinheiro, Pix, crédito, débito)
     * @param valor     valor da venda (em dinheiro, o total, não o recebido: o troco sai na hora)
     * @param descricao identificação (ex.: "venda #102")
     * @param momento   data e hora
     * @throws CaixaFechadoException    se o caixa não estiver aberto
     * @throws IllegalArgumentException se a forma for FIADO ou o valor não for positivo
     */
    public void registrarVenda(FormaPagamento forma, BigDecimal valor, String descricao, LocalDateTime momento)
            throws CaixaFechadoException {
        adicionar(new MovimentoCaixa(TipoMovimentoCaixa.ENTRADA_VENDA, forma, valor, descricao, momento));
    }

    /**
     * Registra o recebimento de compras fiado.
     *
     * @param forma     forma de pagamento (dinheiro, Pix, crédito, débito)
     * @param valor     valor recebido
     * @param descricao identificação (ex.: "fiado Seu Zé")
     * @param momento   data e hora
     * @throws CaixaFechadoException    se o caixa não estiver aberto
     * @throws IllegalArgumentException se a forma for FIADO ou o valor não for positivo
     */
    public void registrarRecebimentoFiado(FormaPagamento forma, BigDecimal valor, String descricao,
                                          LocalDateTime momento) throws CaixaFechadoException {
        adicionar(new MovimentoCaixa(TipoMovimentoCaixa.ENTRADA_PAGAMENTO_DEBITO, forma, valor, descricao, momento));
    }

    /**
     * Registra um reforço de dinheiro na gaveta.
     *
     * @param valor  valor colocado; maior que zero
     * @param motivo motivo (ex.: "troco")
     * @param momento data e hora
     * @throws CaixaFechadoException       se o caixa não estiver aberto
     * @throws ValorCaixaInvalidoException se o valor for zero, negativo ou tiver mais de 2 casas
     */
    public void registrarSuprimento(BigDecimal valor, String motivo, LocalDateTime momento)
            throws CaixaFechadoException, ValorCaixaInvalidoException {
        exigirAberto();
        validarValor(valor, "valor do suprimento", false);
        adicionar(new MovimentoCaixa(TipoMovimentoCaixa.SUPRIMENTO, FormaPagamento.DINHEIRO, valor, motivo, momento));
    }

    /**
     * Registra uma retirada de dinheiro da gaveta.
     *
     * @param valor   valor retirado; maior que zero e no máximo o dinheiro da gaveta
     * @param motivo  motivo (ex.: "levado ao cofre")
     * @param momento data e hora
     * @throws CaixaFechadoException           se o caixa não estiver aberto
     * @throws ValorCaixaInvalidoException     se o valor for zero, negativo ou tiver mais de 2 casas
     * @throws SaldoCaixaInsuficienteException se o valor for maior que o dinheiro na gaveta
     */
    public void registrarSangria(BigDecimal valor, String motivo, LocalDateTime momento)
            throws CaixaFechadoException, ValorCaixaInvalidoException, SaldoCaixaInsuficienteException {
        exigirAberto();
        validarValor(valor, "valor da sangria", false);
        BigDecimal naGaveta = calcularSaldoEsperado();
        if (valor.compareTo(naGaveta) > 0) {
            throw new SaldoCaixaInsuficienteException(naGaveta, valor);
        }
        adicionar(new MovimentoCaixa(TipoMovimentoCaixa.SANGRIA, FormaPagamento.DINHEIRO, valor, motivo, momento));
    }

    // ---------------------------------------------------------------- conferência (RN14)

    /**
     * Dinheiro que deveria estar na gaveta agora: abertura + entradas em dinheiro + suprimentos
     * − sangrias. Pix e cartão não entram nesta conta.
     *
     * @return valor esperado na gaveta
     */
    public BigDecimal calcularSaldoEsperado() {
        return movimentos.stream()
                .map(MovimentoCaixa::efeitoNaGaveta)
                .reduce(valorAbertura, BigDecimal::add);
    }

    /**
     * Quanto entrou de clientes (vendas e fiado) em cada forma de pagamento.
     *
     * @return mapa forma → total; formas sem movimento não aparecem
     */
    public Map<FormaPagamento, BigDecimal> calcularRecebidoPorForma() {
        Map<FormaPagamento, BigDecimal> totais = new EnumMap<>(FormaPagamento.class);
        movimentos.stream()
                .filter(MovimentoCaixa::ehRecebimento)
                .forEach(movimento -> totais.merge(movimento.forma(), movimento.valor(), BigDecimal::add));
        return totais;
    }

    /**
     * Fecha o caixa e faz a conferência.
     *
     * @param quem         quem está fechando; precisa ser quem abriu ou um administrador
     * @param contado      dinheiro contado na gaveta; maior ou igual a zero
     * @param momento      data e hora do fechamento
     * @return relatório com esperado, contado, diferença, situação e totais por forma
     * @throws CaixaFechadoException          se o caixa já estiver fechado
     * @throws OperacaoNaoAutorizadaException se quem fecha não abriu o caixa e não é administrador
     * @throws ValorCaixaInvalidoException    se o valor contado for negativo ou tiver mais de 2 casas
     */
    public ResultadoFechamento fechar(Operador quem, BigDecimal contado, LocalDateTime momento)
            throws CaixaFechadoException, OperacaoNaoAutorizadaException, ValorCaixaInvalidoException {
        exigirAberto();
        Objects.requireNonNull(quem, "quem");
        Objects.requireNonNull(momento, "momento");
        if (!quem.ehOMesmoQue(operador) && !quem.ehAdministrador()) {
            throw new OperacaoNaoAutorizadaException("Só " + operador.getNome()
                    + " (quem abriu o caixa) ou um administrador pode fechá-lo.");
        }
        validarValor(contado, "valor contado", true);

        ResultadoFechamento resultado = ResultadoFechamento.calcular(
                calcularSaldoEsperado(), contado.setScale(CASAS_DINHEIRO), calcularRecebidoPorForma());
        this.valorContado = contado.setScale(CASAS_DINHEIRO);
        this.fechadoEm = momento;
        this.status = StatusCaixa.FECHADO;
        return resultado;
    }

    /**
     * Registra o identificador gerado pelo banco. Chamado uma única vez pelo repositório.
     *
     * @param idGerado identificador positivo
     * @throws IllegalStateException    se a sessão já tiver um id
     * @throws IllegalArgumentException se o id não for positivo
     */
    public void atribuirId(long idGerado) {
        if (this.id != null) {
            throw new IllegalStateException("A sessão de caixa já possui id " + this.id + ".");
        }
        if (idGerado <= 0) {
            throw new IllegalArgumentException("O id deve ser positivo: " + idGerado);
        }
        this.id = idGerado;
    }

    // ---------------------------------------------------------------- consultas

    /** @return {@code true} se o caixa aceita movimentos */
    public boolean estaAberta() {
        return status == StatusCaixa.ABERTO;
    }

    /** @return id gerado pelo banco, ou vazio se ainda não foi salva */
    public Optional<Long> getId() {
        return Optional.ofNullable(id);
    }

    /** @return quem abriu o caixa */
    public Operador getOperador() {
        return operador;
    }

    /** @return momento da abertura */
    public LocalDateTime getAbertoEm() {
        return abertoEm;
    }

    /** @return dinheiro inicial da gaveta */
    public BigDecimal getValorAbertura() {
        return valorAbertura;
    }

    /** @return situação do caixa */
    public StatusCaixa getStatus() {
        return status;
    }

    /** @return momento do fechamento; vazio enquanto aberto */
    public Optional<LocalDateTime> getFechadoEm() {
        return Optional.ofNullable(fechadoEm);
    }

    /** @return valor contado no fechamento; vazio enquanto aberto */
    public Optional<BigDecimal> getValorContado() {
        return Optional.ofNullable(valorContado);
    }

    /** @return cópia somente leitura dos movimentos, na ordem em que aconteceram */
    public List<MovimentoCaixa> getMovimentos() {
        return List.copyOf(movimentos);
    }

    // ---------------------------------------------------------------- internos

    private void adicionar(MovimentoCaixa movimento) throws CaixaFechadoException {
        exigirAberto();
        movimentos.add(movimento);
    }

    private void exigirAberto() throws CaixaFechadoException {
        if (status != StatusCaixa.ABERTO) {
            throw new CaixaFechadoException();
        }
    }

    private static void validarValor(BigDecimal valor, String campo, boolean aceitaZero)
            throws ValorCaixaInvalidoException {
        if (valor == null) {
            throw new ValorCaixaInvalidoException("Informe o " + campo + ".");
        }
        boolean invalido = aceitaZero ? valor.signum() < 0 : valor.signum() <= 0;
        if (invalido) {
            throw new ValorCaixaInvalidoException("O " + campo
                    + (aceitaZero ? " não pode ser negativo." : " deve ser maior que zero."));
        }
        if (Decimais.casasDecimais(valor) > CASAS_DINHEIRO) {
            throw new ValorCaixaInvalidoException("O " + campo + " aceita no máximo 2 casas decimais (centavos).");
        }
    }
}
