package br.com.ferragem.aplicacao.memoria;

import br.com.ferragem.aplicacao.porta.GerenciadorTransacao;
import br.com.ferragem.aplicacao.porta.OperacaoTransacional;
import br.com.ferragem.dominio.excecao.RegraNegocioException;

/**
 * Transação de mentira: só executa a operação e conta quantas vezes foi chamada.
 *
 * <p>Não desfaz nada. Por isso os casos de uso validam tudo antes de alterar dados: os testes
 * provam que, numa recusa, nada foi alterado <i>mesmo sem</i> rollback. O rollback real é testado
 * com o PostgreSQL na fase 3.</p>
 */
public class TransacaoEmMemoria implements GerenciadorTransacao {

    private int execucoes;

    @Override
    public <T> T executar(OperacaoTransacional<T> operacao) throws RegraNegocioException {
        execucoes++;
        return operacao.executar();
    }

    /** @return quantas transações foram abertas */
    public int getExecucoes() {
        return execucoes;
    }
}
