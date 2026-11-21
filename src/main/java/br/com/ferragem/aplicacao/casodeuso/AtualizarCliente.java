package br.com.ferragem.aplicacao.casodeuso;

import br.com.ferragem.aplicacao.porta.ClienteRepository;
import br.com.ferragem.dominio.excecao.RegistroDuplicadoException;
import br.com.ferragem.dominio.excecao.RegraNegocioException;
import br.com.ferragem.dominio.modelo.cliente.Cliente;
import br.com.ferragem.dominio.modelo.cliente.Cpf;

import java.util.Objects;

/**
 * Corrige os dados de um cliente (nome digitado errado, telefone novo, CPF).
 */
public class AtualizarCliente {

    private final ClienteRepository clientes;

    /**
     * @param clientes clientes
     */
    public AtualizarCliente(ClienteRepository clientes) {
        this.clientes = Objects.requireNonNull(clientes);
    }

    /**
     * Dados do formulário de edição.
     *
     * @param clienteId cliente editado
     * @param nome      novo nome
     * @param cpf       novo CPF (vazio remove)
     * @param telefone  novo telefone (vazio remove)
     */
    public record Comando(long clienteId, String nome, String cpf, String telefone) {
    }

    /**
     * @param comando novos dados
     * @return o cliente atualizado
     * @throws RegraNegocioException nome vazio, CPF inválido ou CPF de outro cliente; nada é alterado
     * @throws IllegalArgumentException se o cliente não existir
     */
    public Cliente executar(Comando comando) throws RegraNegocioException {
        Cliente cliente = clientes.buscarPorId(comando.clienteId()).orElseThrow(() ->
                new IllegalArgumentException("Cliente " + comando.clienteId() + " não encontrado."));
        Cpf cpf = CpfOpcional.ler(comando.cpf());
        boolean cpfDeOutro = cpf != null && clientes.buscarPorCpf(cpf)
                .filter(outro -> !outro.getId().equals(cliente.getId()))
                .isPresent();
        if (cpfDeOutro) {
            throw new RegistroDuplicadoException("Já existe outro cliente cadastrado com este CPF.");
        }
        cliente.atualizarDados(comando.nome(), cpf, comando.telefone());
        clientes.salvar(cliente);
        return cliente;
    }
}
