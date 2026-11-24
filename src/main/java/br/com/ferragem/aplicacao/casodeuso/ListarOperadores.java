package br.com.ferragem.aplicacao.casodeuso;

import br.com.ferragem.aplicacao.porta.OperadorRepository;
import br.com.ferragem.dominio.excecao.OperacaoNaoAutorizadaException;
import br.com.ferragem.dominio.modelo.caixa.Operador;

import java.util.List;
import java.util.Objects;

/**
 * Lista de operadores, ativos e desativados, para a tela de administração.
 */
public class ListarOperadores {

    private final OperadorRepository operadores;

    /**
     * @param operadores operadores
     */
    public ListarOperadores(OperadorRepository operadores) {
        this.operadores = Objects.requireNonNull(operadores);
    }

    /**
     * @param quem operador logado
     * @return todos os operadores em ordem de nome
     * @throws OperacaoNaoAutorizadaException se quem pede não for administrador
     */
    public List<Operador> executar(Operador quem) throws OperacaoNaoAutorizadaException {
        Administracao.exigirAdministrador(quem, "ver a lista de operadores");
        return operadores.listar();
    }
}
