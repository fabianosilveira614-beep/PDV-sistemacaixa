package br.com.ferragem.aplicacao.casodeuso;

import br.com.ferragem.dominio.excecao.CpfInvalidoException;
import br.com.ferragem.dominio.modelo.cliente.Cpf;

/**
 * Converte o texto do campo CPF dos formulários: vazio significa "sem CPF".
 * Evita repetir essa regra em cadastro e edição de cliente.
 */
final class CpfOpcional {

    private CpfOpcional() {
    }

    /**
     * @param texto conteúdo do campo
     * @return o CPF, ou nulo se o campo estiver vazio
     * @throws CpfInvalidoException se o campo tiver algo que não é um CPF válido
     */
    static Cpf ler(String texto) throws CpfInvalidoException {
        return texto == null || texto.isBlank() ? null : Cpf.de(texto);
    }
}
