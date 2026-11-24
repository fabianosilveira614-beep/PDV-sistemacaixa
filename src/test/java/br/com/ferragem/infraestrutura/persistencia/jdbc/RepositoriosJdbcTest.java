package br.com.ferragem.infraestrutura.persistencia.jdbc;

import br.com.ferragem.dominio.modelo.caixa.Operador;
import br.com.ferragem.dominio.modelo.caixa.PerfilOperador;
import br.com.ferragem.dominio.modelo.cliente.Cliente;
import br.com.ferragem.dominio.modelo.cliente.Cpf;
import br.com.ferragem.dominio.modelo.produto.Produto;
import br.com.ferragem.dominio.modelo.produto.ProdutoKg;
import br.com.ferragem.dominio.modelo.produto.ProdutoMetro;
import br.com.ferragem.dominio.modelo.produto.ProdutoUnidade;
import br.com.ferragem.dominio.modelo.produto.UnidadeMedida;
import br.com.ferragem.infraestrutura.persistencia.FabricaConexao;
import br.com.ferragem.infraestrutura.persistencia.PersistenciaException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Cada repositório JDBC grava e relê corretamente (ida e volta), contra um PostgreSQL real.
 * Toda leitura é feita por um {@link SistemaJdbc} novo, para não ler um objeto que ficou em memória.
 */
class RepositoriosJdbcTest {

    private FabricaConexao fabrica;
    private SistemaJdbc sistema;

    private static BigDecimal valor(String texto) {
        return new BigDecimal(texto);
    }

    @BeforeEach
    void preparar() {
        fabrica = BancoDeTeste.limparEPreparar();
        sistema = new SistemaJdbc(fabrica);
    }

    private SistemaJdbc reabrir() {
        return new SistemaJdbc(fabrica);
    }

    @Test
    @DisplayName("Produto volta do banco com o subtipo certo, preços, estoque fracionado e id")
    void produtoIdaEVolta() throws Exception {
        ProdutoKg prego = new ProdutoKg("PRE-01", "7891234567895", "Prego 17x27", valor("6.00"), valor("9.99"));
        prego.reporEstoque(valor("2.500"));
        sistema.produtos.salvar(prego);
        ProdutoMetro corrente = new ProdutoMetro("COR-01", null, "Corrente 5mm", valor("8.00"), valor("15.90"));
        sistema.produtos.salvar(corrente);

        Produto lido = reabrir().produtos.buscarPorCodigoBarras("7891234567895").orElseThrow();
        assertInstanceOf(ProdutoKg.class, lido);
        assertEquals(prego.getId(), lido.getId());
        assertEquals(0, valor("2.5").compareTo(lido.getQuantidadeEstoque()));
        assertEquals(0, valor("9.99").compareTo(lido.getPrecoVenda()));
        assertEquals(UnidadeMedida.METRO, reabrir().produtos.buscarPorCodigo("COR-01").orElseThrow().getUnidade());
        assertTrue(reabrir().produtos.buscarPorCodigo("COR-01").orElseThrow().getCodigoBarras().isEmpty());
    }

    @Test
    @DisplayName("Alteração de produto é gravada; inativo some das buscas do PDV mas não do id")
    void produtoAtualizarEInativar() throws Exception {
        ProdutoUnidade martelo = new ProdutoUnidade("MAR-01", null, "Martelo", valor("20.00"), valor("35.00"));
        sistema.produtos.salvar(martelo);
        martelo.alterarPrecos(valor("22.00"), valor("39.90"));
        martelo.inativar();
        sistema.produtos.salvar(martelo);

        SistemaJdbc depois = reabrir();
        assertTrue(depois.produtos.buscarPorCodigo("MAR-01").isEmpty());
        assertTrue(depois.produtos.buscarPorNome("mart").isEmpty());
        Produto lido = depois.produtos.buscarPorId(martelo.getId().orElseThrow()).orElseThrow();
        assertFalse(lido.estaAtivo());
        assertEquals(0, valor("39.90").compareTo(lido.getPrecoVenda()));
    }

    @Test
    @DisplayName("Estoque mínimo volta do banco; listagem mostra inativos só quando pedido")
    void estoqueMinimoEListagem() throws Exception {
        ProdutoKg prego = new ProdutoKg("PRE-01", null, "Prego 17x27", valor("6.00"), valor("9.99"));
        prego.reporEstoque(valor("3"));
        prego.definirEstoqueMinimo(valor("5.5"));
        sistema.produtos.salvar(prego);
        ProdutoUnidade cadeado = new ProdutoUnidade("CAD-30", null, "Cadeado", valor("18"), valor("32.50"));
        cadeado.inativar();
        sistema.produtos.salvar(cadeado);

        Produto lido = reabrir().produtos.buscarPorCodigo("PRE-01").orElseThrow();
        assertEquals(0, valor("5.5").compareTo(lido.getEstoqueMinimo().orElseThrow()));
        assertTrue(lido.precisaRepor());
        assertEquals(1, reabrir().produtos.listar("", false).size());
        assertEquals(2, reabrir().produtos.listar("", true).size());
        assertEquals(1, reabrir().produtos.listar("cad", true).size());

        lido.definirEstoqueMinimo(null);
        reabrir().produtos.salvar(lido);
        assertTrue(reabrir().produtos.buscarPorCodigo("PRE-01").orElseThrow().getEstoqueMinimo().isEmpty());
    }

    @Test
    @DisplayName("Busca por nome ignora maiúsculas e trata % digitado como texto, não curinga")
    void produtoBuscaPorNome() throws Exception {
        sistema.produtos.salvar(new ProdutoUnidade("A", null, "Broca 100% aço", valor("1"), valor("2")));
        sistema.produtos.salvar(new ProdutoUnidade("B", null, "Broca madeira", valor("1"), valor("2")));

        assertEquals(2, reabrir().produtos.buscarPorNome("BROCA").size());
        assertEquals(1, reabrir().produtos.buscarPorNome("100%").size());
        assertEquals(0, reabrir().produtos.buscarPorNome("1%0").size());
    }

    @Test
    @DisplayName("CT-03 no banco: código duplicado é detectado, ignorando o próprio produto na edição")
    void produtoCodigoDuplicado() throws Exception {
        ProdutoUnidade a = new ProdutoUnidade("X-1", "123", "A", valor("1"), valor("2"));
        sistema.produtos.salvar(a);
        long id = a.getId().orElseThrow();

        assertTrue(sistema.produtos.existeCodigo("X-1", null));
        assertFalse(sistema.produtos.existeCodigo("X-1", id));
        assertTrue(sistema.produtos.existeCodigoBarras("123", null));
        assertFalse(sistema.produtos.existeCodigoBarras("999", null));
    }

    @Test
    @DisplayName("Cliente volta com CPF; busca por nome só traz ativos; exclusão apaga")
    void clienteIdaEVolta() throws Exception {
        Cliente ze = new Cliente("José da Silva", Cpf.de("529.982.247-25"), "(11) 99999-0000");
        Cliente ana = new Cliente("Ana Souza", null, null);
        sistema.clientes.salvar(ze);
        sistema.clientes.salvar(ana);
        ana.inativar();
        sistema.clientes.salvar(ana);

        SistemaJdbc depois = reabrir();
        Cliente lido = depois.clientes.buscarPorCpf(Cpf.de("52998224725")).orElseThrow();
        assertEquals("José da Silva", lido.getNome());
        assertEquals("(11) 99999-0000", lido.getTelefone().orElseThrow());
        assertTrue(depois.clientes.buscarPorNome("souza").isEmpty());
        assertFalse(depois.clientes.buscarPorId(ana.getId().orElseThrow()).orElseThrow().estaAtivo());

        depois.clientes.excluir(lido);
        assertTrue(reabrir().clientes.buscarPorId(ze.getId().orElseThrow()).isEmpty());
    }

    @Test
    @DisplayName("Operador é encontrado pelo login sem diferenciar maiúsculas")
    void operadorPorLogin() throws Exception {
        sistema.operadores.salvar(new Operador("Ana", "Ana", "hash", PerfilOperador.OPERADOR));

        Operador lido = reabrir().operadores.buscarPorLogin("  ANA ").orElseThrow();
        assertEquals("ana", lido.getLogin());
        assertEquals(PerfilOperador.OPERADOR, lido.getPerfil());
        assertTrue(reabrir().operadores.buscarPorLogin("bia").isEmpty());
    }

    @Test
    @DisplayName("Obrigação de trocar a senha e a senha nova sobrevivem a reabrir o programa")
    void operadorTrocaDeSenhaPersiste() throws Exception {
        Operador ana = new Operador("Ana", "ana", "hash-antigo", PerfilOperador.OPERADOR);
        ana.exigirTrocaDeSenha();
        sistema.operadores.salvar(ana);
        assertTrue(reabrir().operadores.buscarPorLogin("ana").orElseThrow().deveTrocarSenha());

        Operador lida = reabrir().operadores.buscarPorLogin("ana").orElseThrow();
        lida.alterarSenha("hash-novo");
        reabrir().operadores.salvar(lida);

        Operador depois = reabrir().operadores.buscarPorLogin("ana").orElseThrow();
        assertFalse(depois.deveTrocarSenha());
        assertEquals("hash-novo", depois.getSenhaHash());
    }

    @Test
    @DisplayName("Operador desativado, nome e perfil alterados sobrevivem a reabrir; desativado continua listado")
    void operadorDesativadoPersiste() throws Exception {
        Operador bia = new Operador("Bia", "bia", "hash", PerfilOperador.OPERADOR);
        sistema.operadores.salvar(bia);
        long id = bia.getId().orElseThrow();

        Operador lida = reabrir().operadores.buscarPorId(id).orElseThrow();
        lida.atualizarDados("Beatriz", PerfilOperador.ADMINISTRADOR);
        lida.desativar();
        reabrir().operadores.salvar(lida);

        Operador depois = reabrir().operadores.buscarPorLogin("BIA").orElseThrow();
        assertFalse(depois.estaAtivo());
        assertEquals("Beatriz", depois.getNome());
        assertTrue(depois.ehAdministrador());
        assertTrue(reabrir().operadores.listar().stream().anyMatch(o -> o.getLogin().equals("bia")));
    }

    @Test
    @DisplayName("Dado corrompido no banco é rejeitado na leitura, não vira objeto inválido")
    void leituraRejeitaDadoInvalido() throws Exception {
        sistema.produtos.salvar(new ProdutoUnidade("P-1", null, "Parafuso", valor("1"), valor("2")));
        try (Connection conexao = fabrica.abrirConexao(); Statement comando = conexao.createStatement()) {
            comando.execute("UPDATE produto SET nome = '   ' WHERE codigo = 'P-1'");
        }

        PersistenciaException erro = assertThrows(PersistenciaException.class,
                () -> reabrir().produtos.buscarPorCodigo("P-1"));
        assertTrue(erro.getMessage().contains("produto #1"));
    }
}
