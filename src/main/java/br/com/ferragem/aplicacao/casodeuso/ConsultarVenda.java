package br.com.ferragem.aplicacao.casodeuso;

import br.com.ferragem.aplicacao.porta.HistoricoVendas;
import br.com.ferragem.dominio.modelo.venda.Venda;

import java.util.Objects;
import java.util.Optional;

/**
 * Consulta uma venda pelo número, com os itens como foram vendidos (preço da época). Usado para
 * mostrar "o que foi comprado" numa compra fiado.
 */
public class ConsultarVenda {

    private final HistoricoVendas historico;

    /**
     * @param historico vendas gravadas
     */
    public ConsultarVenda(HistoricoVendas historico) {
        this.historico = Objects.requireNonNull(historico);
    }

    /**
     * @param vendaId número da venda
     * @return a venda, se existir
     */
    public Optional<Venda> executar(long vendaId) {
        return historico.buscarPorId(vendaId);
    }
}
