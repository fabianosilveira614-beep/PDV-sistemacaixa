package br.com.ferragem.dominio.modelo.cliente;

import br.com.ferragem.dominio.excecao.DadoObrigatorioException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Testes de {@link Cliente} (RN12, RN16). */
class ClienteTest {

    @Test
    @DisplayName("CT-27 cliente com nome em branco é recusado")
    void ct27_nomeEmBrancoEhRecusado() {
        DadoObrigatorioException erro = assertThrows(DadoObrigatorioException.class,
                () -> new Cliente("   ", null, null));

        assertEquals("nome do cliente", erro.getCampo());
    }

    @Test
    @DisplayName("Cliente só com nome: CPF e telefone são opcionais")
    void cpfETelefoneSaoOpcionais() throws Exception {
        Cliente cliente = new Cliente("Seu Zé", null, "  ");

        assertTrue(cliente.getCpf().isEmpty());
        assertTrue(cliente.getTelefone().isEmpty());
        assertTrue(cliente.estaAtivo());
    }

    @Test
    @DisplayName("Atualizar dados corrige nome e telefone")
    void atualizarDadosCorrigeNomeETelefone() throws Exception {
        Cliente cliente = new Cliente("Jose da Silva", Cpf.de("529.982.247-25"), null);

        cliente.atualizarDados("José da Silva", cliente.getCpf().orElse(null), "(11) 98888-7777");

        assertEquals("José da Silva", cliente.getNome());
        assertEquals("(11) 98888-7777", cliente.getTelefone().orElseThrow());
        assertEquals("529.982.247-25", cliente.getCpf().orElseThrow().formatado());
    }

    @Test
    @DisplayName("Atualização com nome vazio é recusada e o nome antigo é mantido")
    void atualizacaoInvalidaNaoAlteraNada() throws Exception {
        Cliente cliente = new Cliente("Maria", null, null);

        assertThrows(DadoObrigatorioException.class, () -> cliente.atualizarDados("", null, null));
        assertEquals("Maria", cliente.getNome());
    }

    @Test
    @DisplayName("Cliente pode ser inativado (exclusão lógica)")
    void clientePodeSerInativado() throws Exception {
        Cliente cliente = new Cliente("Maria", null, null);

        cliente.inativar();

        assertFalse(cliente.estaAtivo());
    }
}
