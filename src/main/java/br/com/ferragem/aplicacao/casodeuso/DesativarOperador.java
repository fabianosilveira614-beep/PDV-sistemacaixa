package br.com.ferragem.aplicacao.casodeuso;

import br.com.ferragem.aplicacao.porta.CaixaRepository;
import br.com.ferragem.aplicacao.porta.OperadorRepository;
import br.com.ferragem.dominio.excecao.OperacaoNaoAutorizadaException;
import br.com.ferragem.dominio.modelo.caixa.Operador;
import br.com.ferragem.dominio.modelo.caixa.SessaoCaixa;

import java.util.Objects;
import java.util.Optional;

/**
 * Tira o acesso de um operador sem apagá-lo: ele continua no histórico dos caixas.
 */
public class DesativarOperador {

    private final OperadorRepository operadores;
    private final CaixaRepository caixas;

    /**
     * @param operadores operadores
     * @param caixas     para saber quem está com o caixa aberto
     */
    public DesativarOperador(OperadorRepository operadores, CaixaRepository caixas) {
        this.operadores = Objects.requireNonNull(operadores);
        this.caixas = Objects.requireNonNull(caixas);
    }

    /**
     * @param quem       administrador logado
     * @param operadorId operador a desativar
     * @throws OperacaoNaoAutorizadaException quem pede não é administrador, é ele mesmo, ou o
     *                                        operador está com o caixa aberto (precisa fechá-lo antes)
     */
    public void executar(Operador quem, long operadorId) throws OperacaoNaoAutorizadaException {
        Administracao.exigirAdministrador(quem, "desativar operadores");
        Operador alvo = Administracao.buscar(operadores, operadorId);
        Administracao.exigirOutraPessoa(quem, alvo, "Você não pode desativar a si mesmo.");
        Optional<SessaoCaixa> aberto = caixas.buscarAberta();
        if (aberto.isPresent() && aberto.get().getOperador().ehOMesmoQue(alvo)) {
            throw new OperacaoNaoAutorizadaException("\"" + alvo.getNome()
                    + "\" está com o caixa aberto. O caixa precisa ser fechado antes de desativá-lo.");
        }
        alvo.desativar();
        operadores.salvar(alvo);
    }
}
