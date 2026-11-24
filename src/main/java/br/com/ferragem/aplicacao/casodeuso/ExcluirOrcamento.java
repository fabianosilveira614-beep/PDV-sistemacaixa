package br.com.ferragem.aplicacao.casodeuso;

import br.com.ferragem.aplicacao.porta.OrcamentoRepository;

import java.util.Objects;

/**
 * Apaga um orçamento. Orçamento não tem valor fiscal nem afeta estoque ou caixa, então pode ser
 * apagado de verdade (diferente de produto, cliente e operador, que só são inativados).
 */
public class ExcluirOrcamento {

    private final OrcamentoRepository orcamentos;

    /**
     * @param orcamentos orçamentos
     */
    public ExcluirOrcamento(OrcamentoRepository orcamentos) {
        this.orcamentos = Objects.requireNonNull(orcamentos);
    }

    /**
     * @param orcamentoId número do orçamento
     */
    public void executar(long orcamentoId) {
        orcamentos.excluir(orcamentoId);
    }
}
