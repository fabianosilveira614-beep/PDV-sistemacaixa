package br.com.ferragem;

import br.com.ferragem.aplicacao.casodeuso.AbrirCaixa;
import br.com.ferragem.aplicacao.casodeuso.AjustarEstoque;
import br.com.ferragem.aplicacao.casodeuso.AtualizarCliente;
import br.com.ferragem.aplicacao.casodeuso.AtualizarProduto;
import br.com.ferragem.aplicacao.casodeuso.AutenticarOperador;
import br.com.ferragem.aplicacao.casodeuso.CadastrarCliente;
import br.com.ferragem.aplicacao.casodeuso.CadastrarOperador;
import br.com.ferragem.aplicacao.casodeuso.CadastrarProduto;
import br.com.ferragem.aplicacao.casodeuso.ConsultarCaixaAberto;
import br.com.ferragem.aplicacao.casodeuso.ConsultarClientes;
import br.com.ferragem.aplicacao.casodeuso.ConsultarContaFiado;
import br.com.ferragem.aplicacao.casodeuso.ConsultarHistoricoCliente;
import br.com.ferragem.aplicacao.casodeuso.ConsultarProdutos;
import br.com.ferragem.aplicacao.casodeuso.ExcluirCliente;
import br.com.ferragem.aplicacao.casodeuso.ExcluirProduto;
import br.com.ferragem.aplicacao.casodeuso.FecharCaixa;
import br.com.ferragem.aplicacao.casodeuso.InativarProduto;
import br.com.ferragem.aplicacao.casodeuso.ListarProdutos;
import br.com.ferragem.aplicacao.casodeuso.PagarDebito;
import br.com.ferragem.aplicacao.casodeuso.RegistrarSangria;
import br.com.ferragem.aplicacao.casodeuso.RegistrarSuprimento;
import br.com.ferragem.aplicacao.casodeuso.ReativarProduto;
import br.com.ferragem.aplicacao.casodeuso.RegistrarVenda;
import br.com.ferragem.aplicacao.casodeuso.TrocarSenha;
import br.com.ferragem.aplicacao.pagamento.PagamentoCartao;
import br.com.ferragem.aplicacao.pagamento.PagamentoDinheiro;
import br.com.ferragem.aplicacao.pagamento.PagamentoFiado;
import br.com.ferragem.aplicacao.pagamento.PagamentoPix;
import br.com.ferragem.aplicacao.pagamento.ProcessadoresPagamento;
import br.com.ferragem.aplicacao.porta.HashSenha;
import br.com.ferragem.aplicacao.porta.ProcessadorCartao;
import br.com.ferragem.dominio.modelo.venda.FormaPagamento;
import br.com.ferragem.infraestrutura.config.ConfiguracaoBanco;
import br.com.ferragem.infraestrutura.pagamento.ProcessadorCartaoSimulado;
import br.com.ferragem.infraestrutura.persistencia.FabricaConexao;
import br.com.ferragem.infraestrutura.persistencia.MigradorBanco;
import br.com.ferragem.infraestrutura.persistencia.jdbc.CaixaRepositoryJdbc;
import br.com.ferragem.infraestrutura.persistencia.jdbc.ClienteRepositoryJdbc;
import br.com.ferragem.infraestrutura.persistencia.jdbc.ContextoJdbc;
import br.com.ferragem.infraestrutura.persistencia.jdbc.DebitoRepositoryJdbc;
import br.com.ferragem.infraestrutura.persistencia.jdbc.MovimentoEstoqueRepositoryJdbc;
import br.com.ferragem.infraestrutura.persistencia.jdbc.OperadorRepositoryJdbc;
import br.com.ferragem.infraestrutura.persistencia.jdbc.ProdutoRepositoryJdbc;
import br.com.ferragem.infraestrutura.persistencia.jdbc.VendaRepositoryJdbc;
import br.com.ferragem.infraestrutura.seguranca.HashSenhaPbkdf2;

import java.time.Clock;
import java.util.List;

/**
 * <i>Composition root</i> da aplicação: o único lugar que cria as implementações concretas
 * (JDBC, pagamentos simulados etc.) e as entrega a quem precisa.
 *
 * <p>Os casos de uso e as telas nunca dão {@code new} em classes de infraestrutura: recebem as
 * dependências prontas daqui. É isso que permite trocar o banco ou testar as regras com
 * repositórios em memória (princípio da inversão de dependência, o "D" do SOLID).</p>
 *
 * <p><b>Se amanhã o banco mudar</b> (outro SGBD, arquivo, API), só esta classe e o pacote
 * {@code infraestrutura.persistencia} mudam: os casos de uso dependem das interfaces em
 * {@code aplicacao.porta}, não das classes JDBC. O mesmo vale para trocar o cartão simulado
 * por uma maquininha de verdade: basta outra implementação de {@link ProcessadorCartao}.</p>
 */
public final class ConfiguracaoAplicacao {

    private final ConfiguracaoBanco configuracaoBanco;
    private final FabricaConexao fabricaConexao;
    private final MigradorBanco migradorBanco;

    private ConfiguracaoAplicacao(ConfiguracaoBanco configuracaoBanco) {
        this.configuracaoBanco = configuracaoBanco;
        this.fabricaConexao = new FabricaConexao(configuracaoBanco);
        this.migradorBanco = new MigradorBanco(configuracaoBanco);
    }

    /**
     * Monta a aplicação com a configuração padrão ({@code config.properties} + variáveis de ambiente).
     *
     * @return aplicação configurada
     * @throws IllegalStateException se a configuração do banco estiver incompleta
     */
    public static ConfiguracaoAplicacao criarPadrao() {
        return new ConfiguracaoAplicacao(ConfiguracaoBanco.carregarPadrao());
    }

    /** @return configuração do banco em uso */
    public ConfiguracaoBanco getConfiguracaoBanco() {
        return configuracaoBanco;
    }

    /** @return fábrica de conexões compartilhada pelos repositórios */
    public FabricaConexao getFabricaConexao() {
        return fabricaConexao;
    }

    /**
     * Monta todos os casos de uso sobre o PostgreSQL. Não abre conexão nenhuma: cada operação
     * abre (e fecha) a sua quando for executada.
     *
     * @return casos de uso prontos para as telas
     */
    public CasosDeUso criarCasosDeUso() {
        Clock relogio = Clock.systemDefaultZone();
        ContextoJdbc contexto = new ContextoJdbc(fabricaConexao);
        var produtos = new ProdutoRepositoryJdbc(contexto);
        var movimentosEstoque = new MovimentoEstoqueRepositoryJdbc(contexto);
        var clientes = new ClienteRepositoryJdbc(contexto);
        var operadores = new OperadorRepositoryJdbc(contexto);
        var caixas = new CaixaRepositoryJdbc(contexto);
        var debitos = new DebitoRepositoryJdbc(contexto);
        var vendas = new VendaRepositoryJdbc(contexto);
        HashSenha hashSenha = new HashSenhaPbkdf2();
        ProcessadoresPagamento processadores = criarProcessadores(new ProcessadorCartaoSimulado());

        return new CasosDeUso(
                new AutenticarOperador(operadores, hashSenha),
                new CadastrarOperador(operadores, hashSenha),
                new TrocarSenha(operadores, hashSenha),
                new ConsultarCaixaAberto(caixas),
                new AbrirCaixa(caixas, relogio),
                new RegistrarSuprimento(caixas, relogio),
                new RegistrarSangria(caixas, relogio),
                new FecharCaixa(caixas, relogio),
                new ConsultarProdutos(produtos),
                new ListarProdutos(produtos),
                new CadastrarProduto(produtos, movimentosEstoque, contexto, relogio),
                new AtualizarProduto(produtos),
                new InativarProduto(produtos),
                new ReativarProduto(produtos),
                new ExcluirProduto(produtos, vendas, contexto),
                new AjustarEstoque(produtos, movimentosEstoque, contexto, relogio),
                new RegistrarVenda(produtos, movimentosEstoque, vendas, debitos, caixas, processadores,
                        contexto, relogio),
                new CadastrarCliente(clientes),
                new AtualizarCliente(clientes),
                new ExcluirCliente(clientes, debitos, vendas),
                new ConsultarClientes(clientes),
                new ConsultarHistoricoCliente(vendas),
                new ConsultarContaFiado(debitos),
                new PagarDebito(debitos, caixas, processadores, contexto, relogio));
    }

    private static ProcessadoresPagamento criarProcessadores(ProcessadorCartao operadora) {
        return new ProcessadoresPagamento(List.of(
                new PagamentoDinheiro(),
                new PagamentoPix(),
                new PagamentoCartao(FormaPagamento.CREDITO, operadora),
                new PagamentoCartao(FormaPagamento.DEBITO, operadora),
                new PagamentoFiado()));
    }

    /** @return componente que cria e atualiza as tabelas */
    public MigradorBanco getMigradorBanco() {
        return migradorBanco;
    }
}
