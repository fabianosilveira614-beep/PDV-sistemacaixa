package br.com.ferragem.apresentacao.comum;

import br.com.ferragem.dominio.modelo.caixa.Operador;

import java.util.Optional;

/**
 * Quem está usando o sistema neste momento. Existe só na apresentação: o domínio não tem
 * "usuário logado", cada caso de uso recebe o operador como parâmetro.
 */
public class Sessao {

    private Operador operador;

    /** @param operador quem acabou de entrar */
    public void entrar(Operador operador) {
        this.operador = operador;
    }

    /** Encerra a sessão (botão "Sair"). */
    public void sair() {
        this.operador = null;
    }

    /** @return o operador logado */
    public Operador operador() {
        return Optional.ofNullable(operador)
                .orElseThrow(() -> new IllegalStateException("Nenhum operador logado."));
    }
}
