package br.com.ferragem.aplicacao.casodeuso;

import br.com.ferragem.aplicacao.porta.DebitoRepository;
import br.com.ferragem.dominio.modelo.cliente.Cliente;
import br.com.ferragem.dominio.modelo.fiado.ContaFiado;

import java.util.Objects;

/**
 * Monta a conta fiado de um cliente para a tela de fiado: compras pendentes, quitadas e total
 * devido (RN11).
 */
public class ConsultarContaFiado {

    private final DebitoRepository debitos;

    /**
     * @param debitos compras fiado
     */
    public ConsultarContaFiado(DebitoRepository debitos) {
        this.debitos = Objects.requireNonNull(debitos);
    }

    /**
     * @param cliente cliente consultado
     * @return conta fiado com todas as compras do cliente
     */
    public ContaFiado executar(Cliente cliente) {
        return new ContaFiado(cliente, debitos.buscarPorCliente(cliente));
    }
}
