package br.com.ferragem.aplicacao.casodeuso;

import br.com.ferragem.aplicacao.porta.OperadorRepository;
import br.com.ferragem.dominio.excecao.OperacaoNaoAutorizadaException;
import br.com.ferragem.dominio.modelo.caixa.Operador;

import java.util.Objects;

/**
 * Regras comuns da administração de operadores, só administrador mexe em
 * operadores
 *
 * Sistema precisa de um admin ativo senão não passa por {@link #exigirAdministrador}). Se ele
 * não pode desativar nem rebaixar a si mesmo, sobra somente admin.
 */
final class Administracao {

    private Administracao() {
    }

    /**
     * @param quem operador logado
     * @param acao o que ele tentou fazer, mensagem (ex.: "desativar operadores")
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
     * @throws OperacaoNaoAutorizadaException se o alvo for o próprio operador
     */
    static void exigirOutraPessoa(Operador quem, Operador alvo, String aviso) throws OperacaoNaoAutorizadaException {
        if (quem.ehOMesmoQue(alvo)) {
            throw new OperacaoNaoAutorizadaException(aviso);
        }
    }

    /**
     * @param operadores repositório do banco
     * @param id         operador
     * @return o operador
     * @throws IllegalArgumentException se não existir
     */
    static Operador buscar(OperadorRepository operadores, long id) {
        return operadores.buscarPorId(id)
                .orElseThrow(() -> new IllegalArgumentException("Operador " + id + " não encontrado."));
    }
}
