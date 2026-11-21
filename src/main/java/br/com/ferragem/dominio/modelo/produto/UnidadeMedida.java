package br.com.ferragem.dominio.modelo.produto;

/**
 * Unidades em que a ferragem vende seus produtos.
 *
 * <p>É um {@code enum} porque o conjunto é pequeno e fechado: impede que o sistema aceite uma
 * unidade inexistente como {@code "LITRO"} ou {@code "kilo"} digitada à mão.</p>
 */
public enum UnidadeMedida {

    /** Peças inteiras: parafuso, cadeado, martelo. */
    UNIDADE("un"),
    /** Produtos a granel pesados na balança: prego, arame. */
    KG("kg"),
    /** Produtos cortados no comprimento: corrente, fio, mangueira. */
    METRO("m");

    private final String sigla;

    UnidadeMedida(String sigla) {
        this.sigla = sigla;
    }

    /** @return abreviação para exibir ao lado da quantidade (ex.: "kg") */
    public String getSigla() {
        return sigla;
    }
}
