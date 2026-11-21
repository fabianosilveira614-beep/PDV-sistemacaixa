package br.com.ferragem.aplicacao.porta;

import br.com.ferragem.dominio.excecao.RegraNegocioException;

/**
 * Executa um trecho de código como uma transação: ou tudo é gravado, ou nada é (RN07).
 *
 * <p>O caso de uso não sabe <i>como</i> a transação é feita. Na fase 3 a implementação usa a
 * conexão JDBC ({@code commit}/{@code rollback}); nos testes, uma versão em memória.</p>
 */
public interface GerenciadorTransacao {

    /**
     * @param operacao código a executar
     * @param <T>      tipo do resultado
     * @return o que a operação devolveu
     * @throws RegraNegocioException a mesma exceção lançada pela operação, depois de desfazer tudo
     */
    <T> T executar(OperacaoTransacional<T> operacao) throws RegraNegocioException;
}
