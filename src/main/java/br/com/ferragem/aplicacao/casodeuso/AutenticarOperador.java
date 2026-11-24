package br.com.ferragem.aplicacao.casodeuso;

import br.com.ferragem.aplicacao.porta.HashSenha;
import br.com.ferragem.aplicacao.porta.OperadorRepository;
import br.com.ferragem.dominio.excecao.CredenciaisInvalidasException;
import br.com.ferragem.dominio.modelo.caixa.Operador;

import java.util.Objects;

/**
 * Login do sistema (RF01).
 */
public class AutenticarOperador {

    private final OperadorRepository operadores;
    private final HashSenha hashSenha;

    /**
     * @param operadores operadores
     * @param hashSenha  conferência de senha
     */
    public AutenticarOperador(OperadorRepository operadores, HashSenha hashSenha) {
        this.operadores = Objects.requireNonNull(operadores);
        this.hashSenha = Objects.requireNonNull(hashSenha);
    }

    /**
     * @param login login digitado (sem diferenciar maiúsculas)
     * @param senha senha digitada
     * @return o operador autenticado
     * @throws CredenciaisInvalidasException se o login não existir, a senha não conferir ou o
     *                                       operador estiver desativado (mesma mensagem nos três
     *                                       casos, de propósito: não revela quais logins existem)
     */
    public Operador executar(String login, String senha) throws CredenciaisInvalidasException {
        if (login == null || login.isBlank() || senha == null || senha.isEmpty()) {
            throw new CredenciaisInvalidasException();
        }
        Operador operador = operadores.buscarPorLogin(login.trim()).orElseThrow(CredenciaisInvalidasException::new);
        if (!operador.estaAtivo() || !hashSenha.confere(senha, operador.getSenhaHash())) {
            throw new CredenciaisInvalidasException();
        }
        return operador;
    }
}
