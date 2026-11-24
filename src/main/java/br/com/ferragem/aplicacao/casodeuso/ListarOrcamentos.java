package br.com.ferragem.aplicacao.casodeuso;

import br.com.ferragem.aplicacao.porta.OrcamentoRepository;
import br.com.ferragem.dominio.modelo.orcamento.Orcamento;

import java.util.List;
import java.util.Objects;

/**
 * Orçamentos guardados, para reimprimir, excluir ou carregar no caixa.
 */
public class ListarOrcamentos {

    private final OrcamentoRepository orcamentos;

    /**
     * @param orcamentos orçamentos
     */
    public ListarOrcamentos(OrcamentoRepository orcamentos) {
        this.orcamentos = Objects.requireNonNull(orcamentos);
    }

    /** @return do mais recente para o mais antigo */
    public List<Orcamento> executar() {
        return orcamentos.listar();
    }
}
