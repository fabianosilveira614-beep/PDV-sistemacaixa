package br.com.ferragem.aplicacao.memoria;

import br.com.ferragem.aplicacao.porta.OrcamentoRepository;
import br.com.ferragem.dominio.modelo.caixa.Operador;
import br.com.ferragem.dominio.modelo.orcamento.Orcamento;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Orçamentos em memória, só para testes. */
public class OrcamentoRepositoryEmMemoria implements OrcamentoRepository {

    private final List<Orcamento> orcamentos = new ArrayList<>();
    private long proximoId = 1;

    @Override
    public void salvar(Orcamento orcamento, Operador autor) {
        orcamento.atribuirId(proximoId++);
        orcamentos.add(orcamento);
    }

    @Override
    public List<Orcamento> listar() {
        return orcamentos.stream().sorted(Comparator.comparing(Orcamento::getCriadoEm)
                .thenComparing(o -> o.getId().orElseThrow()).reversed()).toList();
    }

    @Override
    public Optional<Orcamento> buscarPorId(long id) {
        return orcamentos.stream().filter(o -> o.getId().orElseThrow() == id).findFirst();
    }

    @Override
    public void excluir(long id) {
        orcamentos.removeIf(o -> o.getId().orElseThrow() == id);
    }
}
