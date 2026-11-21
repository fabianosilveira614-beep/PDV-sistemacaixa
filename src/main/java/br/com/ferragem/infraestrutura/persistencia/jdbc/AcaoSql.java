package br.com.ferragem.infraestrutura.persistencia.jdbc;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Trecho de código que usa uma conexão JDBC. Existe para que os repositórios não precisem
 * saber se estão dentro de uma transação ou não: quem decide é o {@link ContextoJdbc}.
 *
 * @param <T> tipo do resultado
 */
@FunctionalInterface
public interface AcaoSql<T> {

    /**
     * @param conexao conexão já aberta; não deve ser fechada aqui
     * @return resultado da operação
     * @throws SQLException erro do driver, convertido em {@code PersistenciaException} pelo contexto
     */
    T executar(Connection conexao) throws SQLException;
}
