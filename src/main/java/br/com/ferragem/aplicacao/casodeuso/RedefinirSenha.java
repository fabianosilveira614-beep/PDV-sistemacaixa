package br.com.ferragem.aplicacao.casodeuso;

import br.com.ferragem.aplicacao.porta.HashSenha;
import br.com.ferragem.aplicacao.porta.OperadorRepository;
import br.com.ferragem.dominio.excecao.RegraNegocioException;
import br.com.ferragem.dominio.excecao.SenhaFracaException;
import br.com.ferragem.dominio.modelo.caixa.Operador;

import java.util.Objects;

/**
 * O administrador define uma senha provisória para quem esqueceu a sua. O operador é obrigado a
 * trocá-la no próximo login, porque o administrador a conhece.
 */
public class RedefinirSenha {

    private final OperadorRepository operadores;
    private final HashSenha hashSenha;

    /**
     * @param operadores operadores
     * @param hashSenha  geração do hash
     */
    public RedefinirSenha(OperadorRepository operadores, HashSenha hashSenha) {
        this.operadores = Objects.requireNonNull(operadores);
        this.hashSenha = Objects.requireNonNull(hashSenha);
    }

    /**
     * @param quem            administrador logado
     * @param operadorId      quem esqueceu a senha
     * @param senhaProvisoria senha nova, com pelo menos {@value CadastrarOperador#TAMANHO_MINIMO_SENHA} caracteres
     * @throws RegraNegocioException quem pede não é administrador, senha curta, ou é a própria senha
     *                               (essa se troca pelo menu "Trocar senha", informando a atual)
     */
    public void executar(Operador quem, long operadorId, String senhaProvisoria) throws RegraNegocioException {
        Administracao.exigirAdministrador(quem, "redefinir senhas");
        Operador alvo = Administracao.buscar(operadores, operadorId);
        Administracao.exigirOutraPessoa(quem, alvo,
                "Para mudar a sua própria senha use \"Trocar senha\" no menu com o seu nome.");
        if (senhaProvisoria == null || senhaProvisoria.length() < CadastrarOperador.TAMANHO_MINIMO_SENHA) {
            throw new SenhaFracaException(CadastrarOperador.TAMANHO_MINIMO_SENHA);
        }
        alvo.definirSenhaProvisoria(hashSenha.gerar(senhaProvisoria));
        operadores.salvar(alvo);
    }
}
