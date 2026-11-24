package br.com.ferragem.aplicacao.casodeuso;

import br.com.ferragem.aplicacao.porta.OperadorRepository;
import br.com.ferragem.dominio.excecao.RegraNegocioException;
import br.com.ferragem.dominio.modelo.caixa.Operador;
import br.com.ferragem.dominio.modelo.caixa.PerfilOperador;

import java.util.Objects;

/**
 * Corrige o nome ou muda o perfil de um operador. O login não muda.
 */
public class AtualizarOperador {

    private final OperadorRepository operadores;

    /**
     * @param operadores operadores
     */
    public AtualizarOperador(OperadorRepository operadores) {
        this.operadores = Objects.requireNonNull(operadores);
    }

    /**
     * @param operadorId operador a alterar
     * @param nome       nome novo
     * @param perfil     perfil novo
     */
    public record Comando(long operadorId, String nome, PerfilOperador perfil) {
    }

    /**
     * @param quem    administrador logado
     * @param comando alteração
     * @return o operador alterado
     * @throws RegraNegocioException quem pede não é administrador, nome vazio, ou o administrador
     *                               tentou tirar de si mesmo o perfil de administrador
     */
    public Operador executar(Operador quem, Comando comando) throws RegraNegocioException {
        Administracao.exigirAdministrador(quem, "alterar operadores");
        Operador alvo = Administracao.buscar(operadores, comando.operadorId());
        if (comando.perfil() != PerfilOperador.ADMINISTRADOR) {
            Administracao.exigirOutraPessoa(quem, alvo, "Você não pode tirar o seu próprio perfil de administrador: "
                    + "peça a outro administrador.");
        }
        alvo.atualizarDados(comando.nome(), comando.perfil());
        operadores.salvar(alvo);
        return alvo;
    }
}
