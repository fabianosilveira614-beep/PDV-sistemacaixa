package br.com.ferragem.dominio.modelo.cliente;

import br.com.ferragem.dominio.excecao.CpfInvalidoException;

import java.util.Objects;

/**
 * CPF de um cliente (<i>value object</i>).
 *
 * <p>Um value object é definido pelo seu <b>valor</b>, não por uma identidade: dois objetos
 * {@code Cpf} com os mesmos números são iguais ({@link #equals}). Ele é <b>imutável</b> (não muda
 * depois de criado) e <b>sempre válido</b>: a única forma de obter um {@code Cpf} é por
 * {@link #de(String)}, que confere os dígitos verificadores. Assim nenhuma outra classe
 * precisa validar CPF de novo.</p>
 */
public final class Cpf {

    private static final int TAMANHO = 11;

    private final String digitos;

    private Cpf(String digitos) {
        this.digitos = digitos;
    }

    /**
     * Cria um CPF a partir do texto digitado, com ou sem pontuação ({@code 529.982.247-25} ou
     * {@code 52998224725}).
     *
     * @param texto CPF digitado
     * @return CPF válido, guardado só com os 11 dígitos
     * @throws CpfInvalidoException se não tiver 11 dígitos, se todos forem iguais ou se os
     *                              dígitos verificadores não conferirem
     */
    public static Cpf de(String texto) throws CpfInvalidoException {
        if (texto == null) {
            throw new CpfInvalidoException("");
        }
        String digitos = texto.replaceAll("[.\\-\\s]", "");
        if (!digitos.matches("\\d{" + TAMANHO + "}")
                || todosIguais(digitos)
                || !digitosVerificadoresConferem(digitos)) {
            throw new CpfInvalidoException(texto);
        }
        return new Cpf(digitos);
    }

    /** @return os 11 dígitos, sem pontuação (formato usado no banco) */
    public String getDigitos() {
        return digitos;
    }

    /** @return CPF no formato de exibição, {@code 000.000.000-00} */
    public String formatado() {
        return digitos.substring(0, 3) + "." + digitos.substring(3, 6) + "."
                + digitos.substring(6, 9) + "-" + digitos.substring(9);
    }

    /**
     * Dois CPFs são iguais quando têm os mesmos dígitos, independentemente de serem objetos
     * diferentes na memória.
     *
     * @param outro objeto a comparar
     * @return {@code true} se for um {@code Cpf} com os mesmos dígitos
     */
    @Override
    public boolean equals(Object outro) {
        return this == outro || (outro instanceof Cpf cpf && digitos.equals(cpf.digitos));
    }

    /**
     * Coerente com {@link #equals}: CPFs iguais têm o mesmo hash (obrigatório para usar em
     * {@code HashSet} e {@code HashMap}).
     *
     * @return hash dos dígitos
     */
    @Override
    public int hashCode() {
        return Objects.hash(digitos);
    }

    @Override
    public String toString() {
        return formatado();
    }

    // ---------------------------------------------------------------- algoritmo da Receita Federal

    private static boolean todosIguais(String digitos) {
        return digitos.chars().allMatch(c -> c == digitos.charAt(0));
    }

    /**
     * Cada dígito verificador é calculado a partir dos dígitos anteriores, multiplicados por pesos
     * decrescentes (10..2 para o primeiro, 11..2 para o segundo). O resto da divisão da soma por 11
     * define o dígito: resto menor que 2 dá 0; caso contrário, 11 menos o resto.
     */
    private static boolean digitosVerificadoresConferem(String digitos) {
        return calcularDigito(digitos, 9) == digito(digitos, 9)
                && calcularDigito(digitos, 10) == digito(digitos, 10);
    }

    private static int calcularDigito(String digitos, int quantidadeBase) {
        int soma = 0;
        int peso = quantidadeBase + 1;
        for (int i = 0; i < quantidadeBase; i++) {
            soma += digito(digitos, i) * peso--;
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }

    private static int digito(String digitos, int posicao) {
        return digitos.charAt(posicao) - '0';
    }
}
