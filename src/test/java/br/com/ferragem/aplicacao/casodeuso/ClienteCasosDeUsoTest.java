package br.com.ferragem.aplicacao.casodeuso;

import br.com.ferragem.aplicacao.memoria.ClienteRepositoryEmMemoria;
import br.com.ferragem.aplicacao.memoria.DebitoRepositoryEmMemoria;
import br.com.ferragem.aplicacao.memoria.RegistroVendasEmMemoria;
import br.com.ferragem.aplicacao.pagamento.ResultadoPagamento;
import br.com.ferragem.dominio.excecao.CpfInvalidoException;
import br.com.ferragem.dominio.excecao.DadoObrigatorioException;
import br.com.ferragem.dominio.excecao.ExclusaoNaoPermitidaException;
import br.com.ferragem.dominio.excecao.PeriodoInvalidoException;
import br.com.ferragem.dominio.excecao.RegistroDuplicadoException;
import br.com.ferragem.dominio.modelo.caixa.Operador;
import br.com.ferragem.dominio.modelo.caixa.PerfilOperador;
import br.com.ferragem.dominio.modelo.caixa.SessaoCaixa;
import br.com.ferragem.dominio.modelo.cliente.Cliente;
import br.com.ferragem.dominio.modelo.fiado.Debito;
import br.com.ferragem.dominio.modelo.produto.ProdutoUnidade;
import br.com.ferragem.dominio.modelo.venda.FormaPagamento;
import br.com.ferragem.dominio.modelo.venda.Venda;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Testes de cadastro, edição, exclusão, busca e histórico de clientes. */
class ClienteCasosDeUsoTest {

    private ClienteRepositoryEmMemoria clientes;
    private DebitoRepositoryEmMemoria debitos;
    private RegistroVendasEmMemoria vendas;
    private CadastrarCliente cadastrar;
    private ExcluirCliente excluir;

    private static BigDecimal valor(String texto) {
        return new BigDecimal(texto);
    }

    @BeforeEach
    void preparar() {
        clientes = new ClienteRepositoryEmMemoria();
        debitos = new DebitoRepositoryEmMemoria();
        vendas = new RegistroVendasEmMemoria();
        cadastrar = new CadastrarCliente(clientes);
        excluir = new ExcluirCliente(clientes, debitos, vendas);
    }

    /** Grava uma venda já finalizada para o cliente, no dia indicado. */
    private void venderPara(Cliente cliente, LocalDateTime quando) throws Exception {
        ProdutoUnidade cadeado = new ProdutoUnidade("CAD-" + quando.getDayOfMonth(), null, "Cadeado",
                valor("20"), valor("35"));
        cadeado.reporEstoque(valor("1"));
        Venda venda = new Venda();
        venda.adicionarItem(cadeado, valor("1"));
        venda.identificarCliente(cliente);
        venda.finalizar(FormaPagamento.PIX, quando);
        Operador ana = new Operador("Ana", "ana", "hash", PerfilOperador.OPERADOR);
        vendas.salvar(venda, SessaoCaixa.abrir(ana, valor("0"), quando), ana,
                new ResultadoPagamento(valor("35"), valor("0"), ""));
    }

    @Test
    @DisplayName("Cadastro com CPF pontuado; nome é obrigatório; CPF inválido é recusado")
    void cadastro() throws Exception {
        Cliente cliente = cadastrar.executar(new CadastrarCliente.Comando("Seu Zé", "529.982.247-25", "(11) 98888-7777"));

        assertTrue(cliente.getId().isPresent());
        assertThrows(DadoObrigatorioException.class, () -> cadastrar.executar(new CadastrarCliente.Comando(" ", "", "")));
        assertThrows(CpfInvalidoException.class,
                () -> cadastrar.executar(new CadastrarCliente.Comando("Maria", "529.982.247-26", "")));
    }

    @Test
    @DisplayName("CT-21 CPF já cadastrado é recusado (com ou sem pontuação)")
    void ct21_cpfDuplicado() throws Exception {
        cadastrar.executar(new CadastrarCliente.Comando("Seu Zé", "529.982.247-25", ""));

        assertThrows(RegistroDuplicadoException.class,
                () -> cadastrar.executar(new CadastrarCliente.Comando("Outro", "52998224725", "")));
        assertEquals(1, clientes.getTodos().size());
    }

    @Test
    @DisplayName("Edição pode manter o próprio CPF, mas não usar o de outro cliente")
    void edicaoDeCpf() throws Exception {
        Cliente ze = cadastrar.executar(new CadastrarCliente.Comando("Seu Zé", "529.982.247-25", ""));
        cadastrar.executar(new CadastrarCliente.Comando("Maria", "111.444.777-35", ""));
        AtualizarCliente atualizar = new AtualizarCliente(clientes);
        long id = ze.getId().orElseThrow();

        atualizar.executar(new AtualizarCliente.Comando(id, "José da Silva", "529.982.247-25", "1199"));
        assertEquals("José da Silva", ze.getNome());

        assertThrows(RegistroDuplicadoException.class,
                () -> atualizar.executar(new AtualizarCliente.Comando(id, "José", "111.444.777-35", "")));
        assertEquals("529.982.247-25", ze.getCpf().orElseThrow().formatado());
    }

    @Test
    @DisplayName("CT-22 cliente que deve fiado não pode ser excluído")
    void ct22_naoExcluiQuemDeve() throws Exception {
        Cliente ze = cadastrar.executar(new CadastrarCliente.Comando("Seu Zé", "", ""));
        debitos.salvar(new Debito(ze, 102, valor("100"), LocalDateTime.of(2026, 11, 5, 9, 0)));

        assertThrows(ExclusaoNaoPermitidaException.class, () -> excluir.executar(ze.getId().orElseThrow()));
        assertTrue(ze.estaAtivo());
    }

    @Test
    @DisplayName("Cliente com fiado quitado é inativado, não apagado")
    void comHistoricoDeFiadoEhInativado() throws Exception {
        Cliente ze = cadastrar.executar(new CadastrarCliente.Comando("Seu Zé", "", ""));
        Debito quitado = new Debito(ze, 102, valor("100"), LocalDateTime.of(2026, 11, 5, 9, 0));
        quitado.registrarPagamento(valor("100"), FormaPagamento.PIX, LocalDateTime.of(2026, 11, 6, 9, 0));
        debitos.salvar(quitado);

        assertEquals(ExcluirCliente.Resultado.INATIVADO, excluir.executar(ze.getId().orElseThrow()));
        assertFalse(ze.estaAtivo());
        assertEquals(1, clientes.getTodos().size());
    }

    @Test
    @DisplayName("Cliente com compras à vista é inativado; cliente sem nenhuma compra é apagado")
    void exclusaoConformeHistorico() throws Exception {
        Cliente comCompras = cadastrar.executar(new CadastrarCliente.Comando("Maria", "", ""));
        Cliente semCompras = cadastrar.executar(new CadastrarCliente.Comando("João", "", ""));
        venderPara(comCompras, LocalDateTime.of(2026, 11, 10, 10, 0));

        assertEquals(ExcluirCliente.Resultado.INATIVADO, excluir.executar(comCompras.getId().orElseThrow()));
        assertEquals(ExcluirCliente.Resultado.EXCLUIDO, excluir.executar(semCompras.getId().orElseThrow()));
        assertEquals(1, clientes.getTodos().size());
    }

    @Test
    @DisplayName("Busca por nome ou por CPF; inativos não aparecem")
    void busca() throws Exception {
        cadastrar.executar(new CadastrarCliente.Comando("José da Silva", "529.982.247-25", ""));
        Cliente maria = cadastrar.executar(new CadastrarCliente.Comando("Maria José", "", ""));
        ConsultarClientes consultar = new ConsultarClientes(clientes);

        assertEquals(2, consultar.executar("josé").size());
        assertEquals("José da Silva", consultar.executar("52998224725").get(0).getNome());
        assertTrue(consultar.executar("123").isEmpty());

        maria.inativar();
        assertEquals(1, consultar.executar("josé").size());
    }

    @Test
    @DisplayName("Histórico filtra pelo período (dias inclusive) e recusa período invertido")
    void historicoPorPeriodo() throws Exception {
        Cliente ze = cadastrar.executar(new CadastrarCliente.Comando("Seu Zé", "", ""));
        venderPara(ze, LocalDateTime.of(2026, 11, 1, 8, 0));
        venderPara(ze, LocalDateTime.of(2026, 11, 15, 23, 59));
        venderPara(ze, LocalDateTime.of(2026, 11, 16, 0, 0));
        ConsultarHistoricoCliente historico = new ConsultarHistoricoCliente(vendas);

        List<Venda> doDia1Ao15 = historico.executar(ze, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 15));

        assertEquals(2, doDia1Ao15.size());
        assertEquals(15, doDia1Ao15.get(0).getDataHora().orElseThrow().getDayOfMonth());
        assertThrows(PeriodoInvalidoException.class,
                () -> historico.executar(ze, LocalDate.of(2026, 11, 15), LocalDate.of(2026, 11, 1)));
    }
}
