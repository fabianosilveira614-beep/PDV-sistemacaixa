package br.com.ferragem.aplicacao.casodeuso;

import br.com.ferragem.aplicacao.porta.OperadorRepository;
import br.com.ferragem.dominio.excecao.OperacaoNaoAutorizadaException;
import br.com.ferragem.dominio.modelo.caixa.Operador;

import java.util.Objects;

/**
 * Regras comuns da administração de operadores, num lugar só: só administrador mexe em
 * operadores, e ninguém mexe no próprio acesso por esta via.
 *
 * <p><b>Por que o sistema nunca fica sem administrador:</b> quem administra é sempre um
 * administrador ativo (senão não teria entrado nem passado por {@link #exigirAdministrador}). Se ele
 * não pode desativar nem rebaixar a si mesmo, sobra pelo menos ele. Não é preciso contar
 * administradores: a regra "não em si mesmo" já garante isso.</p>
 */
final class Administracao {

    private Administracao() {
    }

    /**
     * @param quem operador logado
     * @param acao o que ele tentou fazer, para a mensagem (ex.: "desativar operadores")
     * @throws OperacaoNaoAutorizadaException se não for administrador ativo
     */
    static void exigirAdministrador(Operador quem, String acao) throws OperacaoNaoAutorizadaException {
        Objects.requireNonNull(quem, "quem");
        if (!quem.ehAdministrador() || !quem.estaAtivo()) {
            throw new OperacaoNaoAutorizadaException("Só um administrador pode " + acao + ".");
        }
    }

    /**
     * @param quem  operador logado
     * @param alvo  operador que vai ser alterado
     * @param aviso o que dizer se for a mesma pessoa
     * @throws OperacaoNaoAutorizadaException se o alvo for o próprio operador logado
     */
    static void exigirOutraPessoa(Operador quem, Operador alvo, String aviso) throws OperacaoNaoAutorizadaException {
        if (quem.ehOMesmoQue(alvo)) {
            throw new OperacaoNaoAutorizadaException(aviso);
        }
    }

    /**
     * @param operadores repositório
     * @param id         operador procurado
     * @return o operador
     * @throws IllegalArgumentException se não existir (a tela só oferece operadores da lista)
     */
    static Operador buscar(OperadorRepository operadores, long id) {
        return operadores.buscarPorId(id)
                .orElseThrow(() -> new IllegalArgumentException("Operador " + id + " não encontrado."));
    }
}
