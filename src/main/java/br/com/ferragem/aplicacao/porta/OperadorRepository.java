package br.com.ferragem.aplicacao.porta;

import br.com.ferragem.dominio.modelo.caixa.Operador;

import java.util.List;
import java.util.Optional;

/**
 * Leitura e gravação de operadores.
 */
public interface OperadorRepository {

    /**
     * Procura ativos e desativados: quem decide se um desativado pode entrar é o caso de uso
     * {@code AutenticarOperador}, e o cadastro precisa saber que o login já foi usado.
     *
     * @param login login, sem diferenciar maiúsculas
     * @return o operador com esse login, se existir
     */
    Optional<Operador> buscarPorLogin(String login);

    /**
     * @param id identificador
     * @return o operador, ativo ou não, se existir
     */
    Optional<Operador> buscarPorId(long id);

    /**
     * @return todos os operadores, ativos e desativados, em ordem de nome
     */
    List<Operador> listar();

    /**
     * Insere o operador (atribuindo o id gerado) ou, se já tiver id, grava as alterações.
     *
     * @param operador operador a gravar
     */
    void salvar(Operador operador);
}
