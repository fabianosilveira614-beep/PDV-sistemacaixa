package br.com.ferragem.aplicacao.memoria;

import br.com.ferragem.aplicacao.pagamento.ResultadoPagamento;
import br.com.ferragem.aplicacao.porta.HistoricoVendas;
import br.com.ferragem.aplicacao.porta.RegistroVendas;
import br.com.ferragem.dominio.modelo.caixa.Operador;
import br.com.ferragem.dominio.modelo.caixa.SessaoCaixa;
import br.com.ferragem.dominio.modelo.cliente.Cliente;
import br.com.ferragem.dominio.modelo.produto.Produto;
import br.com.ferragem.dominio.modelo.venda.Venda;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Vendas em memória, só para testes. A primeira venda recebe o número 1.
 *
 * <p>Uma única classe implementa as <b>duas</b> interfaces ({@link RegistroVendas} e
 * {@link HistoricoVendas}); cada caso de uso enxerga só a parte de que precisa. A fase 3 fará o
 * mesmo com o repositório JDBC.</p>
 */
public class RegistroVendasEmMemoria implements RegistroVendas, HistoricoVendas {

    /** O que foi gravado em cada venda. */
    public record Registro(Venda venda, SessaoCaixa sessao, Operador operador, ResultadoPagamento pagamento) {
    }

    private final List<Registro> registros = new ArrayList<>();
    private long proximoId = 1;

    @Override
    public void salvar(Venda venda, SessaoCaixa sessao, Operador operador, ResultadoPagamento pagamento) {
        venda.atribuirId(proximoId++);
        registros.add(new Registro(venda, sessao, operador, pagamento));
    }

    @Override
    public List<Venda> buscarPorCliente(Cliente cliente, LocalDate inicio, LocalDate fim) {
        LocalDateTime de = inicio.atStartOfDay();
        LocalDateTime ate = fim.plusDays(1).atStartOfDay();
        return registros.stream().map(Registro::venda)
                .filter(v -> v.getCliente().filter(c -> c == cliente).isPresent())
                .filter(v -> !v.getDataHora().orElseThrow().isBefore(de) && v.getDataHora().orElseThrow().isBefore(ate))
                .sorted(Comparator.comparing((Venda v) -> v.getDataHora().orElseThrow()).reversed())
                .toList();
    }

    @Override
    public boolean clienteTemVendas(Cliente cliente) {
        return registros.stream().anyMatch(r -> r.venda().getCliente().filter(c -> c == cliente).isPresent());
    }

    @Override
    public Optional<Venda> buscarPorId(long vendaId) {
        return registros.stream().map(Registro::venda)
                .filter(v -> v.getId().orElse(-1L) == vendaId).findFirst();
    }

    @Override
    public boolean produtoFoiVendido(Produto produto) {
        return registros.stream().flatMap(r -> r.venda().getItens().stream())
                .anyMatch(item -> item.ehDoProduto(produto.getCodigo()));
    }

    /** @return vendas gravadas, em ordem */
    public List<Registro> getRegistros() {
        return List.copyOf(registros);
    }
}
