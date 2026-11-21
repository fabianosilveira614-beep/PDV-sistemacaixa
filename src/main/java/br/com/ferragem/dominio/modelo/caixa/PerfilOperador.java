package br.com.ferragem.dominio.modelo.caixa;

/**
 * Perfil de quem opera o sistema. Define o que cada um pode fazer.
 */
public enum PerfilOperador {

    /** Opera o caixa: vende, recebe, abre e fecha caixa. */
    OPERADOR(false),
    /** Tudo o que o operador faz, mais autorizações especiais (RN13) e gestão de cadastros. */
    ADMINISTRADOR(true);

    private final boolean podeVenderAbaixoDoCusto;

    PerfilOperador(boolean podeVenderAbaixoDoCusto) {
        this.podeVenderAbaixoDoCusto = podeVenderAbaixoDoCusto;
    }

    /** @return {@code true} se pode aplicar preço menor que o custo do produto (RN13) */
    public boolean podeVenderAbaixoDoCusto() {
        return podeVenderAbaixoDoCusto;
    }
}
