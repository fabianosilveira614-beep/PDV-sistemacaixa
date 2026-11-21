package br.com.ferragem.aplicacao.porta;

import br.com.ferragem.dominio.modelo.caixa.Operador;

import java.util.Optional;

/**
 * Leitura e gravação de operadores.
 */
public interface OperadorRepository {

    /**
     * @param login login, sem diferenciar maiúsculas
     * @return o operador com esse login, se existir
     */
    Optional<Operador> buscarPorLogin(String login);

    /**
     * Insere o operador e atribui o id gerado.
     *
     * @param operador operador a gravar
     */
    void salvar(Operador operador);
}
