package br.com.ferragem.aplicacao.porta;

import br.com.ferragem.dominio.modelo.cliente.Cliente;
import br.com.ferragem.dominio.modelo.produto.Produto;
import br.com.ferragem.dominio.modelo.venda.Venda;

import java.time.LocalDate;
import java.util.List;

/**
 * Consulta de vendas já feitas. Separada de {@link RegistroVendas} (segregação de interfaces):
 * quem registra a venda não precisa consultar histórico, e vice-versa.
 */
public interface HistoricoVendas {

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
}
