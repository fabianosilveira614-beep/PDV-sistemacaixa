package br.com.ferragem.aplicacao.memoria;

import br.com.ferragem.aplicacao.porta.DebitoRepository;
import br.com.ferragem.dominio.modelo.cliente.Cliente;
import br.com.ferragem.dominio.modelo.fiado.Debito;
import br.com.ferragem.dominio.modelo.fiado.PagamentoDebito;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Compras fiado em memória, só para testes. */
public class DebitoRepositoryEmMemoria implements DebitoRepository {

    private final List<Debito> debitos = new ArrayList<>();
    private long proximoId = 1;
    private int gravacoes;

    @Override
    public List<Debito> buscarPorCliente(Cliente cliente) {
        return debitos.stream().filter(d -> d.getCliente() == cliente).toList();
    }

    @Override
    public List<Debito> buscarPorPeriodoDeCompra(LocalDate inicio, LocalDate fim) {
        return debitos.stream().filter(d -> dentro(d.getCriadoEm(), inicio, fim))
                .sorted(Comparator.comparing(Debito::getCriadoEm)).toList();
    }

    @Override
    public List<PagamentoDebito> buscarPagamentosNoPeriodo(LocalDate inicio, LocalDate fim) {
        return debitos.stream().flatMap(d -> d.getPagamentos().stream())
                .filter(p -> dentro(p.dataHora(), inicio, fim))
                .sorted(Comparator.comparing(PagamentoDebito::dataHora)).toList();
    }

    private static boolean dentro(LocalDateTime momento, LocalDate inicio, LocalDate fim) {
        return !momento.isBefore(inicio.atStartOfDay()) && momento.isBefore(fim.plusDays(1).atStartOfDay());
    }

    @Override
    public void salvar(Debito debito) {
        gravacoes++;
        if (debito.getId().isEmpty()) {
            debito.atribuirId(proximoId++);
            debitos.add(debito);
        }
    }

    /** @return todas as compras fiado gravadas */
    public List<Debito> getTodos() {
        return List.copyOf(debitos);
    }

    /** @return quantas vezes {@link #salvar} foi chamado */
    public int getGravacoes() {
        return gravacoes;
    }
}
