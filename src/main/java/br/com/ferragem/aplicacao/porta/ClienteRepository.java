package br.com.ferragem.aplicacao.porta;

import br.com.ferragem.dominio.modelo.cliente.Cliente;
import br.com.ferragem.dominio.modelo.cliente.Cpf;

import java.util.List;
import java.util.Optional;

/**
 * Leitura e gravação de clientes.
 */
public interface ClienteRepository {

    /**
     * Insere o cliente (atribuindo o id) ou atualiza o existente.
     *
     * @param cliente cliente a gravar
     */
    void salvar(Cliente cliente);

    /**
     * @param id identificador
     * @return o cliente, ativo ou inativo, se existir
     */
    Optional<Cliente> buscarPorId(long id);

    /**
     * @param cpf CPF procurado
     * @return o cliente com esse CPF, ativo ou inativo, se existir
     */
    Optional<Cliente> buscarPorCpf(Cpf cpf);

    /**
     * @param trecho parte do nome, sem diferenciar maiúsculas
     * @return clientes <b>ativos</b> cujo nome contém o trecho, em ordem alfabética
     */
    List<Cliente> buscarPorNome(String trecho);

    /**
     * Apaga definitivamente um cliente sem nenhum histórico.
     *
     * @param cliente cliente a apagar
     */
    void excluir(Cliente cliente);
}
