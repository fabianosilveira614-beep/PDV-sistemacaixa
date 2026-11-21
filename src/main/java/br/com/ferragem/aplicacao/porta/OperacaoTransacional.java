package br.com.ferragem.aplicacao.porta;

import br.com.ferragem.dominio.excecao.RegraNegocioException;

/**
 * Trecho de código que precisa rodar dentro de uma transação.
 *
 * <p>É uma interface funcional (um único método): pode ser escrita como lambda,
 * {@code () -> { ...; return resultado; }}.</p>
 *
 * @param <T> tipo do resultado
 */
@FunctionalInterface
public interface OperacaoTransacional<T> {

    /**
     * @return resultado da operação
     * @throws RegraNegocioException se uma regra for violada (a transação é desfeita)
     */
    T executar() throws RegraNegocioException;
}
