package br.com.ferragem.aplicacao.porta;

import br.com.ferragem.dominio.modelo.caixa.Operador;
import br.com.ferragem.dominio.modelo.orcamento.Orcamento;

import java.util.List;
import java.util.Optional;

/**
 * Orçamentos guardados. Tabelas próprias: gravar ou apagar um orçamento não toca em estoque,
 * caixa nem fiado.
 */
public interface OrcamentoRepository {

    /**
     * Insere o orçamento e atribui o número gerado.
     *
     * @param orcamento orçamento novo
     * @param autor     operador que fez o orçamento
     */
    void salvar(Orcamento orcamento, Operador autor);

    /** @return todos os orçamentos, do mais recente para o mais antigo */
    List<Orcamento> listar();

    /**
     * @param id número do orçamento
     * @return o orçamento, se existir
     */
    Optional<Orcamento> buscarPorId(long id);

    /**
     * @param id número do orçamento a apagar (nada acontece se não existir)
     */
    void excluir(long id);
}
