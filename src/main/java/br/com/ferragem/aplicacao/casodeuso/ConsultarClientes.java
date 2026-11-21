package br.com.ferragem.aplicacao.casodeuso;

import br.com.ferragem.aplicacao.porta.ClienteRepository;
import br.com.ferragem.dominio.excecao.CpfInvalidoException;
import br.com.ferragem.dominio.modelo.cliente.Cliente;
import br.com.ferragem.dominio.modelo.cliente.Cpf;

import java.util.List;
import java.util.Objects;

/**
 * Busca de clientes por nome ou CPF. O operador pode <b>procurar</b> digitando o CPF, mas a tela
 * mostra só o nome no resultado (o CPF aparece apenas no cadastro e no perfil).
 */
public class ConsultarClientes {

    private final ClienteRepository clientes;

    /**
     * @param clientes clientes
     */
    public ConsultarClientes(ClienteRepository clientes) {
        this.clientes = Objects.requireNonNull(clientes);
    }

    /**
     * @param termo parte do nome, ou um CPF completo (com ou sem pontuação); vazio ou nulo lista todos
     * @return clientes ativos encontrados, em ordem de nome (excluídos nunca aparecem: não podem comprar)
     */
    public List<Cliente> executar(String termo) {
        if (termo == null || termo.isBlank()) {
            return clientes.buscarPorNome("");
        }
        String procurado = termo.trim();
        if (procurado.matches("[\\d.\\-\\s]+")) {
            return buscarPorCpf(procurado);
        }
        return clientes.buscarPorNome(procurado);
    }

    private List<Cliente> buscarPorCpf(String texto) {
        try {
            return clientes.buscarPorCpf(Cpf.de(texto)).filter(Cliente::estaAtivo).map(List::of).orElse(List.of());
        } catch (CpfInvalidoException e) {
            return List.of();   // só números, mas não é um CPF válido: não há quem encontrar
        }
    }
}
