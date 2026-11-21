package br.com.ferragem.aplicacao.porta;

import br.com.ferragem.dominio.modelo.produto.Produto;

import java.util.List;
import java.util.Optional;

/**
 * Consulta de produtos, somente leitura.
 *
 * <p>Separada de {@link ProdutoRepository} de propósito (segregação de interfaces, o "I" do
 * SOLID): a busca do PDV só precisa ler, então depende só desta interface e nem consegue chamar
 * {@code salvar}.</p>
 */
public interface ConsultaProdutos {

    /**
     * @param codigo código interno
     * @return o produto ativo com esse código, se existir
     */
    Optional<Produto> buscarPorCodigo(String codigo);

    /**
     * @param codigoBarras código lido pelo leitor
     * @return o produto ativo com esse código de barras, se existir
     */
    Optional<Produto> buscarPorCodigoBarras(String codigoBarras);

    /**
     * @param trecho parte do nome, sem diferenciar maiúsculas
     * @return produtos ativos cujo nome contém o trecho, em ordem alfabética
     */
    List<Produto> buscarPorNome(String trecho);
}
