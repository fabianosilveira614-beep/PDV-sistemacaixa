package br.com.ferragem.aplicacao.casodeuso;

import br.com.ferragem.aplicacao.porta.ClienteRepository;
import br.com.ferragem.aplicacao.porta.DebitoRepository;
import br.com.ferragem.aplicacao.porta.HistoricoVendas;
import br.com.ferragem.dominio.excecao.ExclusaoNaoPermitidaException;
import br.com.ferragem.dominio.modelo.cliente.Cliente;
import br.com.ferragem.dominio.modelo.fiado.ContaFiado;
import br.com.ferragem.dominio.modelo.fiado.Debito;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * Exclui um cliente respeitando o histórico (RN12):
 * <ul>
 *   <li>deve compras fiado → recusado;</li>
 *   <li>já comprou (venda ou fiado quitado) → <b>inativado</b>, para o histórico continuar consultável;</li>
 *   <li>nunca comprou → apagado de verdade.</li>
 * </ul>
 */
public class ExcluirCliente {

    private final ClienteRepository clientes;
    private final DebitoRepository debitos;
    private final HistoricoVendas historico;

    /** O que aconteceu com o cliente, para a tela dar a mensagem certa. */
    public enum Resultado {
        /** Apagado definitivamente. */
        EXCLUIDO,
        /** Mantido no banco como inativo, por ter histórico. */
        INATIVADO
    }

    /**
     * @param clientes  clientes
     * @param debitos   compras fiado
     * @param historico histórico de vendas
     */
    public ExcluirCliente(ClienteRepository clientes, DebitoRepository debitos, HistoricoVendas historico) {
        this.clientes = Objects.requireNonNull(clientes);
        this.debitos = Objects.requireNonNull(debitos);
        this.historico = Objects.requireNonNull(historico);
    }

    /**
     * @param clienteId cliente a excluir
     * @return se foi excluído ou inativado
     * @throws ExclusaoNaoPermitidaException se o cliente ainda deve compras fiado
     * @throws IllegalArgumentException      se o cliente não existir
     */
    public Resultado executar(long clienteId) throws ExclusaoNaoPermitidaException {
        Cliente cliente = clientes.buscarPorId(clienteId).orElseThrow(() ->
                new IllegalArgumentException("Cliente " + clienteId + " não encontrado."));
        List<Debito> comprasFiado = debitos.buscarPorCliente(cliente);
        BigDecimal devendo = new ContaFiado(cliente, comprasFiado).calcularSaldoDevedor();
        if (devendo.signum() > 0) {
            throw new ExclusaoNaoPermitidaException("\"" + cliente.getNome() + "\" ainda deve R$ "
                    + devendo.toPlainString() + " em compras fiado. Receba o pagamento antes de excluir.");
        }
        if (!comprasFiado.isEmpty() || historico.clienteTemVendas(cliente)) {
            cliente.inativar();
            clientes.salvar(cliente);
            return Resultado.INATIVADO;
        }
        clientes.excluir(cliente);
        return Resultado.EXCLUIDO;
    }
}
