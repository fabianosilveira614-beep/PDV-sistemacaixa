package br.com.ferragem.aplicacao.memoria;

import br.com.ferragem.aplicacao.porta.CaixaRepository;
import br.com.ferragem.dominio.modelo.caixa.SessaoCaixa;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Sessões de caixa em memória, só para testes. */
public class CaixaRepositoryEmMemoria implements CaixaRepository {

    private final List<SessaoCaixa> sessoes = new ArrayList<>();
    private long proximoId = 1;

    @Override
    public Optional<SessaoCaixa> buscarAberta() {
        return sessoes.stream().filter(SessaoCaixa::estaAberta).findFirst();
    }

    @Override
    public void salvar(SessaoCaixa sessao) {
        if (sessao.getId().isEmpty()) {
            sessao.atribuirId(proximoId++);
            sessoes.add(sessao);
        }
    }

    /** @return todas as sessões, abertas e fechadas */
    public List<SessaoCaixa> getTodas() {
        return List.copyOf(sessoes);
    }
}
