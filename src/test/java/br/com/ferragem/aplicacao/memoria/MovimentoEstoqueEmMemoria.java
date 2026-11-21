package br.com.ferragem.aplicacao.memoria;

import br.com.ferragem.aplicacao.porta.MovimentoEstoqueRepository;
import br.com.ferragem.dominio.modelo.produto.MovimentoEstoque;

import java.util.ArrayList;
import java.util.List;

/** Trilha de estoque em memória, só para testes. */
public class MovimentoEstoqueEmMemoria implements MovimentoEstoqueRepository {

    private final List<MovimentoEstoque> movimentos = new ArrayList<>();

    @Override
    public void registrar(MovimentoEstoque movimento) {
        movimentos.add(movimento);
    }

    /** @return movimentos registrados, em ordem */
    public List<MovimentoEstoque> getMovimentos() {
        return List.copyOf(movimentos);
    }
}
