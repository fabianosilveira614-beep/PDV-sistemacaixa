package br.com.ferragem.aplicacao.porta;

import br.com.ferragem.dominio.modelo.cliente.Cliente;
import br.com.ferragem.dominio.modelo.produto.Produto;
import br.com.ferragem.dominio.modelo.venda.Venda;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Consulta de vendas já feitas. Separada de {@link RegistroVendas} (segregação de interfaces):
 * quem registra a venda não precisa consultar histórico, e vice-versa.
 */
public interface HistoricoVendas {

    /**
     * Venda do relatório por período, com o nome de quem a registrou.
     *
     * @param venda    venda finalizada, com cliente e itens
     * @param operador nome do operador que registrou a venda
     */
    record VendaDoPeriodo(Venda venda, String operador) {
    }

    /**
     * @param inicio primeiro dia do período (inclusive)
     * @param fim    último dia do período (inclusive)
     * @return todas as vendas do período, da mais recente para a mais antiga
     */
    List<VendaDoPeriodo> buscarPorPeriodo(LocalDate inicio, LocalDate fim);

    /**
     * @param cliente cliente consultado
     * @param inicio  primeiro dia do período (inclusive)
     * @param fim     último dia do período (inclusive)
     * @return vendas do cliente no período, da mais recente para a mais antiga
     */
    List<Venda> buscarPorCliente(Cliente cliente, LocalDate inicio, LocalDate fim);

    /**
     * @param cliente cliente consultado
     * @return {@code true} se o cliente tem ao menos uma venda registrada
     */
    boolean clienteTemVendas(Cliente cliente);

    /**
     * @param produto produto cadastrado
     * @return {@code true} se o produto aparece em alguma venda (então não pode ser apagado)
     */
    boolean produtoFoiVendido(Produto produto);

    /**
     * @param vendaId número da venda
     * @return a venda finalizada, com cliente e itens como foram vendidos, se existir
     */
    Optional<Venda> buscarPorId(long vendaId);
}
