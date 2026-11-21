package br.com.ferragem.aplicacao.porta;

import br.com.ferragem.aplicacao.pagamento.ResultadoPagamento;
import br.com.ferragem.dominio.modelo.caixa.Operador;
import br.com.ferragem.dominio.modelo.caixa.SessaoCaixa;
import br.com.ferragem.dominio.modelo.venda.Venda;

/**
 * Gravação de vendas finalizadas. As consultas de histórico ficam em outra interface (fase 2b),
 * porque quem registra a venda não precisa consultar histórico.
 */
public interface RegistroVendas {

    /**
     * Grava a venda e seus itens e atribui o id gerado à venda.
     *
     * @param venda     venda finalizada
     * @param sessao    caixa em que foi feita
     * @param operador  quem vendeu
     * @param pagamento valor recebido e troco
     */
    void salvar(Venda venda, SessaoCaixa sessao, Operador operador, ResultadoPagamento pagamento);
}
