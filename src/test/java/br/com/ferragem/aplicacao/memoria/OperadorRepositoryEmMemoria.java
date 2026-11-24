package br.com.ferragem.aplicacao.memoria;

import br.com.ferragem.aplicacao.porta.OperadorRepository;
import br.com.ferragem.dominio.modelo.caixa.Operador;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Operadores em memória, só para testes. */
public class OperadorRepositoryEmMemoria implements OperadorRepository {

    private final List<Operador> operadores = new ArrayList<>();
    private long proximoId = 1;

    @Override
    public Optional<Operador> buscarPorLogin(String login) {
        String procurado = login.trim().toLowerCase(Locale.ROOT);
        return operadores.stream().filter(o -> o.getLogin().equals(procurado)).findFirst();
    }

    @Override
    public Optional<Operador> buscarPorId(long id) {
        return operadores.stream().filter(o -> o.getId().orElseThrow() == id).findFirst();
    }

    @Override
    public List<Operador> listar() {
        return operadores.stream().sorted(Comparator.comparing(Operador::getNome)).toList();
    }

    @Override
    public void salvar(Operador operador) {
        if (operador.getId().isEmpty()) {
            operador.atribuirId(proximoId++);
            operadores.add(operador);
        }
    }
}
