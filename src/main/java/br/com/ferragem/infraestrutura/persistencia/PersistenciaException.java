package br.com.ferragem.infraestrutura.persistencia;

/**
 * Falha técnica ao acessar o banco de dados (conexão recusada, SQL inválido, migração com erro).
 *
 * <p>É uma exceção não verificada porque não representa uma regra de negócio violada: o usuário
 * não tem como corrigi-la preenchendo o formulário de outro jeito. A causa original
 * ({@link java.sql.SQLException}, por exemplo) é sempre preservada para facilitar o diagnóstico.</p>
 */
public class PersistenciaException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * @param mensagem descrição do que se tentava fazer quando a falha ocorreu
     * @param causa    exceção original do driver ou da biblioteca
     */
    public PersistenciaException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
