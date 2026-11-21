package br.com.ferragem.dominio.modelo.caixa;

import br.com.ferragem.dominio.excecao.DadoObrigatorioException;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Pessoa que usa o sistema: abre o caixa, vende, recebe.
 *
 * <p>A senha nunca é guardada como texto, só o seu <i>hash</i> (uma "impressão digital"
 * calculada a partir dela, que não permite recuperar a senha). Gerar e conferir o hash é
 * tarefa da infraestrutura; o domínio só guarda o resultado.</p>
 */
public class Operador {

    private Long id;
    private final String nome;
    private final String login;
    private String senhaHash;
    private boolean trocarSenha;
    private final PerfilOperador perfil;

    /**
     * @param nome      nome do operador, obrigatório
     * @param login     login, obrigatório; guardado em minúsculas
     * @param senhaHash hash da senha, gerado pela infraestrutura; obrigatório
     * @param perfil    perfil de acesso
     * @throws DadoObrigatorioException se nome, login ou senha estiverem vazios
     */
    public Operador(String nome, String login, String senhaHash, PerfilOperador perfil)
            throws DadoObrigatorioException {
        this.nome = exigirTexto(nome, "nome do operador");
        this.login = exigirTexto(login, "login").toLowerCase(Locale.ROOT);
        this.senhaHash = exigirTexto(senhaHash, "senha");
        this.perfil = Objects.requireNonNull(perfil, "perfil");
    }

    /**
     * Diz se dois objetos representam a mesma pessoa. Compara pelo login, que é único.
     *
     * @param outro outro operador
     * @return {@code true} se for o mesmo login
     */
    public boolean ehOMesmoQue(Operador outro) {
        return outro != null && login.equals(outro.login);
    }

    /** @return {@code true} se o perfil for ADMINISTRADOR */
    public boolean ehAdministrador() {
        return perfil == PerfilOperador.ADMINISTRADOR;
    }

    /**
     * Obriga o operador a trocar a senha no próximo login. Usado quando outra pessoa definiu a
     * senha (o administrador, ao cadastrá-lo) e portanto ela não é segredo só dele.
     */
    public void exigirTrocaDeSenha() {
        this.trocarSenha = true;
    }

    /**
     * Substitui a senha e libera a obrigação de troca.
     *
     * @param novoHash hash da nova senha (a regra de tamanho mínimo é do caso de uso {@code TrocarSenha})
     * @throws DadoObrigatorioException se o hash estiver vazio
     */
    public void alterarSenha(String novoHash) throws DadoObrigatorioException {
        this.senhaHash = exigirTexto(novoHash, "senha");
        this.trocarSenha = false;
    }

    /** @return {@code true} se o operador precisa definir uma senha nova antes de usar o sistema */
    public boolean deveTrocarSenha() {
        return trocarSenha;
    }

    /**
     * Registra o identificador gerado pelo banco. Chamado uma única vez pelo repositório.
     *
     * @param idGerado identificador positivo
     * @throws IllegalStateException    se o operador já tiver um id
     * @throws IllegalArgumentException se o id não for positivo
     */
    public void atribuirId(long idGerado) {
        if (this.id != null) {
            throw new IllegalStateException("O operador já possui id " + this.id + ".");
        }
        if (idGerado <= 0) {
            throw new IllegalArgumentException("O id deve ser positivo: " + idGerado);
        }
        this.id = idGerado;
    }

    /** @return id gerado pelo banco, ou vazio se ainda não foi salvo */
    public Optional<Long> getId() {
        return Optional.ofNullable(id);
    }

    /** @return nome do operador */
    public String getNome() {
        return nome;
    }

    /** @return login, em minúsculas */
    public String getLogin() {
        return login;
    }

    /** @return hash da senha (nunca a senha em texto) */
    public String getSenhaHash() {
        return senhaHash;
    }

    /** @return perfil de acesso */
    public PerfilOperador getPerfil() {
        return perfil;
    }

    /** @return nome, login e perfil; nunca o hash da senha */
    @Override
    public String toString() {
        return "Operador{" + nome + ", login=" + login + ", " + perfil + "}";
    }

    private static String exigirTexto(String valor, String campo) throws DadoObrigatorioException {
        if (valor == null || valor.isBlank()) {
            throw new DadoObrigatorioException(campo);
        }
        return valor.trim();
    }
}
