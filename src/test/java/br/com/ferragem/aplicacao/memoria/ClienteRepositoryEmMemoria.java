package br.com.ferragem.aplicacao.memoria;

import br.com.ferragem.aplicacao.porta.ClienteRepository;
import br.com.ferragem.dominio.modelo.cliente.Cliente;
import br.com.ferragem.dominio.modelo.cliente.Cpf;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Clientes em memória, só para testes. */
public class ClienteRepositoryEmMemoria implements ClienteRepository {

    private final List<Cliente> clientes = new ArrayList<>();
    private long proximoId = 1;

    @Override
    public void salvar(Cliente cliente) {
        if (cliente.getId().isEmpty()) {
            cliente.atribuirId(proximoId++);
            clientes.add(cliente);
        }
    }

    @Override
    public Optional<Cliente> buscarPorId(long id) {
        return clientes.stream().filter(c -> c.getId().orElse(-1L) == id).findFirst();
    }

    @Override
    public Optional<Cliente> buscarPorCpf(Cpf cpf) {
        return clientes.stream().filter(c -> c.getCpf().filter(cpf::equals).isPresent()).findFirst();
    }

    @Override
    public List<Cliente> buscarPorNome(String trecho) {
        String procurado = trecho.toLowerCase(Locale.ROOT);
        return clientes.stream().filter(Cliente::estaAtivo)
                .filter(c -> c.getNome().toLowerCase(Locale.ROOT).contains(procurado))
                .sorted(Comparator.comparing(Cliente::getNome)).toList();
    }

    @Override
    public void excluir(Cliente cliente) {
        clientes.remove(cliente);
    }

    /** @return todos os clientes guardados, ativos e inativos */
    public List<Cliente> getTodos() {
        return List.copyOf(clientes);
    }
}
