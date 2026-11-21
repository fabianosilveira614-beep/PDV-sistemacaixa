package br.com.ferragem.aplicacao.porta;

import br.com.ferragem.dominio.modelo.caixa.SessaoCaixa;

import java.util.Optional;

/**
 * Leitura e gravação das sessões de caixa.
 */
public interface CaixaRepository {

    /** @return o caixa aberto no momento, se houver (no máximo um, RN08) */
    Optional<SessaoCaixa> buscarAberta();

    /**
     * Insere a sessão (atribuindo o id) ou grava o fechamento e os movimentos novos.
     *
     * @param sessao sessão de caixa
     */
    void salvar(SessaoCaixa sessao);
}
