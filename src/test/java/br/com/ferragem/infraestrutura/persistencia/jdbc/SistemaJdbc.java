package br.com.ferragem.infraestrutura.persistencia.jdbc;

import br.com.ferragem.aplicacao.casodeuso.AbrirCaixa;
import br.com.ferragem.aplicacao.casodeuso.CadastrarCliente;
import br.com.ferragem.aplicacao.casodeuso.CadastrarProduto;
import br.com.ferragem.aplicacao.casodeuso.FecharCaixa;
import br.com.ferragem.aplicacao.casodeuso.PagarDebito;
import br.com.ferragem.aplicacao.casodeuso.RegistrarSangria;
import br.com.ferragem.aplicacao.casodeuso.RegistrarSuprimento;
import br.com.ferragem.aplicacao.casodeuso.RegistrarVenda;
import br.com.ferragem.aplicacao.pagamento.PagamentoCartao;
import br.com.ferragem.aplicacao.pagamento.PagamentoDinheiro;
import br.com.ferragem.aplicacao.pagamento.PagamentoFiado;
import br.com.ferragem.aplicacao.pagamento.PagamentoPix;
import br.com.ferragem.aplicacao.pagamento.ProcessadoresPagamento;
import br.com.ferragem.aplicacao.porta.CaixaRepository;
import br.com.ferragem.dominio.modelo.venda.FormaPagamento;
import br.com.ferragem.infraestrutura.pagamento.ProcessadorCartaoSimulado;
import br.com.ferragem.infraestrutura.persistencia.FabricaConexao;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

/**
 * Um "programa inteiro" montado com repositórios JDBC, para os testes de integração.
 *
 * <p>Criar um segundo {@code SistemaJdbc} sobre o mesmo banco equivale a fechar e reabrir o
 * aplicativo: os objetos novos não compartilham nada com os antigos, só o que está gravado.</p>
 */
final class SistemaJdbc {

    static final Clock RELOGIO = Clock.fixed(Instant.parse("2026-11-20T18:00:00Z"), ZoneId.of("America/Sao_Paulo"));

    final ContextoJdbc contexto;
    final ProdutoRepositoryJdbc produtos;
    final MovimentoEstoqueRepositoryJdbc movimentosEstoque;
    final ClienteRepositoryJdbc clientes;
    final OperadorRepositoryJdbc operadores;
    final CaixaRepositoryJdbc caixas;
    final DebitoRepositoryJdbc debitos;
    final VendaRepositoryJdbc vendas;
    final OrcamentoRepositoryJdbc orcamentos;

    SistemaJdbc(FabricaConexao fabrica) {
        contexto = new ContextoJdbc(fabrica);
        produtos = new ProdutoRepositoryJdbc(contexto);
        movimentosEstoque = new MovimentoEstoqueRepositoryJdbc(contexto);
        clientes = new ClienteRepositoryJdbc(contexto);
        operadores = new OperadorRepositoryJdbc(contexto);
        caixas = new CaixaRepositoryJdbc(contexto);
        debitos = new DebitoRepositoryJdbc(contexto);
        vendas = new VendaRepositoryJdbc(contexto);
        orcamentos = new OrcamentoRepositoryJdbc(contexto);
    }

    ProcessadoresPagamento processadores() {
        ProcessadorCartaoSimulado operadora = new ProcessadorCartaoSimulado();
        return new ProcessadoresPagamento(List.of(new PagamentoDinheiro(), new PagamentoPix(),
                new PagamentoFiado(), new PagamentoCartao(FormaPagamento.CREDITO, operadora),
                new PagamentoCartao(FormaPagamento.DEBITO, operadora)));
    }

    RegistrarVenda registrarVenda() {
        return registrarVendaCom(caixas);
    }

    /** Permite trocar o repositório de caixa por um que falha, para testar o rollback. */
    RegistrarVenda registrarVendaCom(CaixaRepository caixa) {
        return new RegistrarVenda(produtos, movimentosEstoque, vendas, debitos, caixa, processadores(),
                contexto, RELOGIO);
    }

    PagarDebito pagarDebito() {
        return new PagarDebito(debitos, caixas, processadores(), contexto, RELOGIO);
    }

    CadastrarProduto cadastrarProduto() {
        return new CadastrarProduto(produtos, movimentosEstoque, contexto, RELOGIO);
    }

    CadastrarCliente cadastrarCliente() {
        return new CadastrarCliente(clientes);
    }

    AbrirCaixa abrirCaixa() {
        return new AbrirCaixa(caixas, RELOGIO);
    }

    FecharCaixa fecharCaixa() {
        return new FecharCaixa(caixas, RELOGIO);
    }

    RegistrarSuprimento suprimento() {
        return new RegistrarSuprimento(caixas, RELOGIO);
    }

    RegistrarSangria sangria() {
        return new RegistrarSangria(caixas, RELOGIO);
    }
}
