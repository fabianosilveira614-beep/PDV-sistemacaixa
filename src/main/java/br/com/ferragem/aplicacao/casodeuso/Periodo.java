package br.com.ferragem.aplicacao.casodeuso;

import br.com.ferragem.dominio.excecao.PeriodoInvalidoException;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Regra comum a toda consulta por período (histórico do cliente e relatórios): as duas datas são
 * obrigatórias e a inicial não pode vir depois da final. Os dois dias contam (inclusive).
 */
final class Periodo {

    private Periodo() {
    }

    /**
     * @param inicio primeiro dia
     * @param fim    último dia
     * @throws PeriodoInvalidoException se o início for depois do fim
     */
    static void validar(LocalDate inicio, LocalDate fim) throws PeriodoInvalidoException {
        Objects.requireNonNull(inicio, "inicio");
        Objects.requireNonNull(fim, "fim");
        if (inicio.isAfter(fim)) {
            throw new PeriodoInvalidoException();
        }
    }
}
