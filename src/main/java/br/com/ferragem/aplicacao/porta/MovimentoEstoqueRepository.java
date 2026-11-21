package br.com.ferragem.aplicacao.porta;

import br.com.ferragem.dominio.modelo.produto.MovimentoEstoque;

/**
 * Gravação da trilha de movimentações de estoque.
 */
public interface MovimentoEstoqueRepository {

    /**
     * @param movimento movimentação a registrar
     */
    void registrar(MovimentoEstoque movimento);
}
