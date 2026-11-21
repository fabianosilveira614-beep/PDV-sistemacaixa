package br.com.ferragem.aplicacao.casodeuso;

import br.com.ferragem.aplicacao.porta.HashSenha;
import br.com.ferragem.aplicacao.porta.OperadorRepository;
import br.com.ferragem.dominio.excecao.CredenciaisInvalidasException;
import br.com.ferragem.dominio.excecao.RegraNegocioException;
import br.com.ferragem.dominio.excecao.SenhaFracaException;
import br.com.ferragem.dominio.modelo.caixa.Operador;

import java.util.Objects;

/**
 * O próprio operador troca a sua senha. Obrigatório no primeiro acesso (senha definida pelo
 * administrador ou a senha padrão do {@code admin}).
 *
 * <p>Regras: a senha atual precisa conferir (quem deixou o caixa logado não tem a senha trocada
 * por outra pessoa); a nova precisa ter o tamanho mínimo e ser diferente da atual.</p>
 */
public class TrocarSenha {

    private final OperadorRepository operadores;
    private final HashSenha hashSenha;

    /**
     * @param operadores onde o operador é gravado
     * @param hashSenha  algoritmo de hash de senha
     */
    public TrocarSenha(OperadorRepository operadores, HashSenha hashSenha) {
        this.operadores = Objects.requireNonNull(operadores);
        this.hashSenha = Objects.requireNonNull(hashSenha);
    }

    /**
     * @param operador   quem está trocando a própria senha
     * @param senhaAtual senha em uso
     * @param novaSenha  senha nova
     * @throws RegraNegocioException senha atual errada, nova curta demais ou igual à atual; nada é alterado
     */
    public void executar(Operador operador, String senhaAtual, String novaSenha) throws RegraNegocioException {
        Objects.requireNonNull(operador, "operador");
        if (senhaAtual == null || !hashSenha.confere(senhaAtual, operador.getSenhaHash())) {
            throw new CredenciaisInvalidasException("A senha atual não confere.");
        }
        if (novaSenha == null || novaSenha.length() < CadastrarOperador.TAMANHO_MINIMO_SENHA) {
            throw new SenhaFracaException(CadastrarOperador.TAMANHO_MINIMO_SENHA);
        }
        if (novaSenha.equals(senhaAtual)) {
            throw new SenhaFracaException("A nova senha deve ser diferente da atual.");
        }
        operador.alterarSenha(hashSenha.gerar(novaSenha));
        operadores.salvar(operador);
    }
}
