package br.com.ferragem.aplicacao.casodeuso;

import br.com.ferragem.aplicacao.porta.ProdutoRepository;
import br.com.ferragem.dominio.modelo.produto.Produto;

import java.util.List;
import java.util.Objects;

/**
 * Lista produtos para a tela de cadastro, com filtro por nome ou código e a opção de ver os
 * inativos.
 *
 * <p>É diferente de {@link ConsultarProdutos}, que serve ao PDV: lá só interessa o que pode ser
 * vendido agora, e o código de barras exato vem primeiro. Aqui o dono da loja precisa ver tudo,
 * inclusive o que saiu de linha, para poder reativar.</p>
 */
public class ListarProdutos {

    private final ProdutoRepository produtos;

    /**
     * @param produtos onde os produtos ficam
     */
    public ListarProdutos(ProdutoRepository produtos) {
        this.produtos = Objects.requireNonNull(produtos);
    }

    /**
     * @param trecho          parte do nome ou do código; nulo ou vazio lista todos
     * @param incluirInativos se mostra também os produtos fora de linha
     * @return produtos em ordem de nome
     */
    public List<Produto> executar(String trecho, boolean incluirInativos) {
        return produtos.listar(trecho == null ? "" : trecho.trim(), incluirInativos);
    }
}
