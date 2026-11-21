package br.com.ferragem.aplicacao.casodeuso;

import br.com.ferragem.aplicacao.porta.ClienteRepository;
import br.com.ferragem.aplicacao.porta.DebitoRepository;
import br.com.ferragem.dominio.modelo.cliente.Cliente;
import br.com.ferragem.dominio.modelo.fiado.ContaFiado;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * Lista clientes para a tela de cadastro, cada um com o total devido e quantas compras fiado estão
 * em aberto.
 *
 * <p><b>O total vem do domínio</b> ({@link ContaFiado#calcularSaldoDevedor()}, RN11), cliente por
 * cliente, e não de uma soma em SQL. Custa uma consulta por cliente, o que para uma ferragem é
 * instantâneo; em troca a regra do saldo devedor continua morando num lugar só. Uma soma em SQL
 * seria mais rápida, mas criaria uma segunda versão da regra que um dia divergiria da primeira
 * (decisão do grupo).</p>
 */
public class ListarClientes {

    /**
     * Linha da lista de clientes.
     *
     * @param cliente          o cliente
     * @param totalDevido      soma dos saldos das compras fiado em aberto
     * @param comprasEmAberto  quantas compras fiado ainda não foram quitadas
     */
    public record ResumoCliente(Cliente cliente, BigDecimal totalDevido, int comprasEmAberto) {
    }

    private final ConsultarClientes consultar;
    private final DebitoRepository debitos;

    /**
     * @param clientes onde os clientes ficam
     * @param debitos  compras fiado de cada cliente
     */
    public ListarClientes(ClienteRepository clientes, DebitoRepository debitos) {
        this.consultar = new ConsultarClientes(Objects.requireNonNull(clientes));
        this.debitos = Objects.requireNonNull(debitos);
    }

    /**
     * @param termo parte do nome ou CPF (só números ou com pontuação); vazio lista todos os ativos
     * @return clientes ativos em ordem de nome, com o total devido de cada um
     */
    public List<ResumoCliente> executar(String termo) {
        return consultar.executar(termo).stream().map(this::resumir).toList();
    }

    private ResumoCliente resumir(Cliente cliente) {
        ContaFiado conta = new ContaFiado(cliente, debitos.buscarPorCliente(cliente));
        return new ResumoCliente(cliente, conta.calcularSaldoDevedor(), conta.getPendentes().size());
    }
}
