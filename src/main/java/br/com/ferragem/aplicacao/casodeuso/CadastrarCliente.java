package br.com.ferragem.aplicacao.casodeuso;

import br.com.ferragem.aplicacao.porta.ClienteRepository;
import br.com.ferragem.dominio.excecao.RegistroDuplicadoException;
import br.com.ferragem.dominio.excecao.RegraNegocioException;
import br.com.ferragem.dominio.modelo.cliente.Cliente;
import br.com.ferragem.dominio.modelo.cliente.Cpf;

import java.util.Objects;
import java.util.Optional;

/**
 * Cadastra um cliente, pela tela de clientes ou direto do PDV para liberar o fiado.
 * Nome obrigatório; CPF opcional, mas válido e único quando informado (RN03, RN16).
 */
public class CadastrarCliente {

    private final ClienteRepository clientes;

    /**
     * @param clientes clientes
     */
    public CadastrarCliente(ClienteRepository clientes) {
        this.clientes = Objects.requireNonNull(clientes);
    }

    /**
     * Dados do formulário.
     *
     * @param nome     nome do cliente
     * @param cpf      CPF digitado, com ou sem pontuação; vazio se não informado
     * @param telefone telefone; vazio se não informado
     */
    public record Comando(String nome, String cpf, String telefone) {
    }

    /**
     * @param comando dados do cliente
     * @return o cliente cadastrado, já com id
     * @throws RegraNegocioException nome vazio, CPF inválido ou CPF já cadastrado
     */
    public Cliente executar(Comando comando) throws RegraNegocioException {
        Cpf cpf = CpfOpcional.ler(comando.cpf());
        Cliente cliente = new Cliente(comando.nome(), cpf, comando.telefone());
        if (cpf != null) {
            Optional<Cliente> dono = clientes.buscarPorCpf(cpf);
            if (dono.isPresent()) {
                // cliente "excluído" que já tinha compras fica inativo e continua dono do CPF
                throw new RegistroDuplicadoException(dono.get().estaAtivo()
                        ? "Já existe um cliente cadastrado com este CPF."
                        : "Este CPF pertence a um cliente excluído do cadastro (ele tinha compras, "
                          + "por isso foi inativado e não apagado).");
            }
        }
        clientes.salvar(cliente);
        return cliente;
    }
}
