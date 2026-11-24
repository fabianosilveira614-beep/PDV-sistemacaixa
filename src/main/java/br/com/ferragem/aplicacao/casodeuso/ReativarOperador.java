package br.com.ferragem.aplicacao.casodeuso;

import br.com.ferragem.aplicacao.porta.OperadorRepository;
import br.com.ferragem.dominio.excecao.OperacaoNaoAutorizadaException;
import br.com.ferragem.dominio.modelo.caixa.Operador;

import java.util.Objects;

/**
 * Devolve o acesso a um operador desativado (ex.: funcionário que voltou).
 */
public class ReativarOperador {

    private final OperadorRepository operadores;

    /**
     * @param operadores operadores
     */
    public ReativarOperador(OperadorRepository operadores) {
        this.operadores = Objects.requireNonNull(operadores);
    }

    /**
     * @param quem       administrador logado
     * @param operadorId operador a reativar
     * @throws OperacaoNaoAutorizadaException se quem pede não for administrador
     */
    public void executar(Operador quem, long operadorId) throws OperacaoNaoAutorizadaException {
        Administracao.exigirAdministrador(quem, "reativar operadores");
        Operador alvo = Administracao.buscar(operadores, operadorId);
        alvo.reativar();
        operadores.salvar(alvo);
    }
}
