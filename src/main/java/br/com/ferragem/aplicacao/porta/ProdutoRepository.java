package br.com.ferragem.aplicacao.porta;

import br.com.ferragem.dominio.modelo.produto.Produto;

import java.util.List;
import java.util.Optional;

/**
 * Leitura e gravação de produtos.
 */
public interface ProdutoRepository extends ConsultaProdutos {

    /**
     * Insere o produto (se ainda não tem id, atribuindo o id gerado) ou atualiza o existente.
     *
     * @param produto produto a gravar
     */
    void salvar(Produto produto);

    /**
     * @param codigo    código a verificar
     * @param ignorarId id do próprio produto, numa edição; nulo num cadastro novo
     * @return {@code true} se outro produto já usa esse código
     */
    boolean existeCodigo(String codigo, Long ignorarId);

    /**
     * @param codigoBarras código de barras a verificar
     * @param ignorarId    id do próprio produto, numa edição; nulo num cadastro novo
     * @return {@code true} se outro produto já usa esse código de barras
     */
    boolean existeCodigoBarras(String codigoBarras, Long ignorarId);

    /**
     * @param id identificador do produto
     * @return o produto, ativo ou inativo, se existir
     */
    Optional<Produto> buscarPorId(long id);

    /**
     * Lista para a tela de cadastro (diferente da busca do PDV, que só vê produtos ativos e para no
     * primeiro achado pelo código).
     *
     * @param trecho          parte do nome ou do código; vazio lista todos
     * @param incluirInativos se {@code true}, mostra também os produtos fora de linha
     * @return produtos em ordem de nome
     */
    List<Produto> listar(String trecho, boolean incluirInativos);

    /**
     * Apaga de verdade um produto cadastrado por engano, junto com seus movimentos de estoque
     * (estoque inicial, ajustes). Só pode ser chamado para produto que nunca foi vendido: quem
     * garante isso é o caso de uso {@code ExcluirProduto}.
     *
     * @param produto produto a apagar
     */
    void excluir(Produto produto);
}
