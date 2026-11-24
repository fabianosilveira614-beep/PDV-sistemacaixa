package br.com.ferragem.aplicacao.porta;

import br.com.ferragem.dominio.modelo.cliente.Cliente;
import br.com.ferragem.dominio.modelo.fiado.Debito;
import br.com.ferragem.dominio.modelo.fiado.PagamentoDebito;

import java.time.LocalDate;

import java.util.List;

/**
 * Leitura e gravação das compras fiado.
 */
public interface DebitoRepository {

    /**
     * @param cliente cliente devedor
     * @return todas as compras fiado do cliente, pendentes e quitadas, com seus pagamentos
     */
    List<Debito> buscarPorCliente(Cliente cliente);

    /**
     * Compras fiado feitas no período, pendentes e quitadas, com cliente e pagamentos. Quem
     * decide o que está "em aberto" é o domínio ({@link Debito#estaPendente()}), não o SQL.
     *
     * @param inicio primeiro dia da compra (inclusive)
     * @param fim    último dia da compra (inclusive)
     * @return compras da mais antiga para a mais nova
     */
    List<Debito> buscarPorPeriodoDeCompra(LocalDate inicio, LocalDate fim);

    /**
     * @param inicio primeiro dia (inclusive)
     * @param fim    último dia (inclusive)
     * @return pagamentos de fiado recebidos no período, de qualquer compra
     */
    List<PagamentoDebito> buscarPagamentosNoPeriodo(LocalDate inicio, LocalDate fim);

    /**
     * Insere a compra fiado (atribuindo o id) ou grava os pagamentos novos de uma existente.
     *
     * @param debito compra fiado
     */
    void salvar(Debito debito);
}
