package br.com.ferragem.aplicacao.casodeuso;

import br.com.ferragem.aplicacao.porta.HashSenha;
import br.com.ferragem.aplicacao.porta.OperadorRepository;
import br.com.ferragem.dominio.excecao.OperacaoNaoAutorizadaException;
import br.com.ferragem.dominio.excecao.RegistroDuplicadoException;
import br.com.ferragem.dominio.excecao.RegraNegocioException;
import br.com.ferragem.dominio.excecao.SenhaFracaException;
import br.com.ferragem.dominio.modelo.caixa.Operador;
import br.com.ferragem.dominio.modelo.caixa.PerfilOperador;

import java.util.Objects;

/**
 * Cadastra um novo operador. Só um administrador pode cadastrar. A senha é guardada como hash.
 */
public class CadastrarOperador {

    /** Tamanho mínimo de senha. */
    public static final int TAMANHO_MINIMO_SENHA = 6;

    private final OperadorRepository operadores;
    private final HashSenha hashSenha;

    /**
     * @param operadores operadores
     * @param hashSenha  geração do hash
     */
    public CadastrarOperador(OperadorRepository operadores, HashSenha hashSenha) {
        this.operadores = Objects.requireNonNull(operadores);
        this.hashSenha = Objects.requireNonNull(hashSenha);
    }

    /**
     * Dados do novo operador.
     *
     * @param nome   nome
     * @param login  login (guardado em minúsculas)
     * @param senha  senha em texto, com pelo menos {@value #TAMANHO_MINIMO_SENHA} caracteres
     * @param perfil perfil de acesso
     */
    public record Comando(String nome, String login, String senha, PerfilOperador perfil) {
    }

    /**
     * @param quem    administrador que está cadastrando
     * @param comando dados do novo operador
     * @return o operador cadastrado
     * @throws RegraNegocioException quem cadastra não é administrador, dado vazio, senha curta ou login em uso
     */
    public Operador executar(Operador quem, Comando comando) throws RegraNegocioException {
        Objects.requireNonNull(quem, "quem");
        if (!quem.ehAdministrador()) {
            throw new OperacaoNaoAutorizadaException("Só um administrador pode cadastrar operadores.");
        }
        if (comando.senha() == null || comando.senha().length() < TAMANHO_MINIMO_SENHA) {
            throw new SenhaFracaException(TAMANHO_MINIMO_SENHA);
        }
        String login = comando.login() == null ? "" : comando.login().trim();
        if (!login.isEmpty() && operadores.buscarPorLogin(login).isPresent()) {
            throw new RegistroDuplicadoException("O login \"" + login + "\" já está em uso.");
        }
        // o hash é gerado por último: é lento de propósito, não vale gastá-lo num cadastro recusado
        Operador novo = new Operador(comando.nome(), login, hashSenha.gerar(comando.senha()), comando.perfil());
        novo.exigirTrocaDeSenha(); // o administrador conhece esta senha: o operador deve trocá-la
        operadores.salvar(novo);
        return novo;
    }
}
