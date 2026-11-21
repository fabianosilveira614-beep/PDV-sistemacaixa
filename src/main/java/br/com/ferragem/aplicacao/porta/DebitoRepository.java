package br.com.ferragem.aplicacao.porta;

import br.com.ferragem.dominio.modelo.cliente.Cliente;
import br.com.ferragem.dominio.modelo.fiado.Debito;

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
     * Insere a compra fiado (atribuindo o id) ou grava os pagamentos novos de uma existente.
     *
     * @param debito compra fiado
     */
    void salvar(Debito debito);
}
