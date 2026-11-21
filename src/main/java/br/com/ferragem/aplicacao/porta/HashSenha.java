package br.com.ferragem.aplicacao.porta;

/**
 * Transforma senhas em <i>hash</i> e confere senhas digitadas contra o hash guardado.
 *
 * <p>O hash é uma "impressão digital" de mão única: dá para conferir uma senha, mas não para
 * recuperá-la. Assim, nem quem tem acesso ao banco descobre as senhas.</p>
 */
public interface HashSenha {

    /**
     * @param senha senha em texto
     * @return hash para guardar no banco (inclui o sal e os parâmetros usados)
     */
    String gerar(String senha);

    /**
     * @param senha       senha digitada
     * @param hashGuardado hash guardado no cadastro
     * @return {@code true} se a senha corresponde ao hash
     */
    boolean confere(String senha, String hashGuardado);
}
