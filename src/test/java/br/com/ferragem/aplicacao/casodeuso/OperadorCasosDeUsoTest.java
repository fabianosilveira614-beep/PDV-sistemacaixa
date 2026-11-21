package br.com.ferragem.aplicacao.casodeuso;

import br.com.ferragem.aplicacao.memoria.HashSenhaFalso;
import br.com.ferragem.aplicacao.memoria.OperadorRepositoryEmMemoria;
import br.com.ferragem.dominio.excecao.CredenciaisInvalidasException;
import br.com.ferragem.dominio.excecao.OperacaoNaoAutorizadaException;
import br.com.ferragem.dominio.excecao.RegistroDuplicadoException;
import br.com.ferragem.dominio.excecao.SenhaFracaException;
import br.com.ferragem.dominio.modelo.caixa.Operador;
import br.com.ferragem.dominio.modelo.caixa.PerfilOperador;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Testes de login e cadastro de operadores. */
class OperadorCasosDeUsoTest {

    private OperadorRepositoryEmMemoria operadores;
    private HashSenhaFalso hash;
    private CadastrarOperador cadastrar;
    private AutenticarOperador autenticar;
    private TrocarSenha trocarSenha;
    private Operador dona;

    @BeforeEach
    void preparar() throws Exception {
        operadores = new OperadorRepositoryEmMemoria();
        hash = new HashSenhaFalso();
        cadastrar = new CadastrarOperador(operadores, hash);
        autenticar = new AutenticarOperador(operadores, hash);
        trocarSenha = new TrocarSenha(operadores, hash);
        dona = new Operador("Dona Marta", "marta", hash.gerar("segredo1"), PerfilOperador.ADMINISTRADOR);
        operadores.salvar(dona);
    }

    @Test
    @DisplayName("Administrador cadastra operador; a senha é guardada como hash, não como texto")
    void cadastraComHash() throws Exception {
        Operador ana = cadastrar.executar(dona,
                new CadastrarOperador.Comando("Ana", "Ana", "minhasenha", PerfilOperador.OPERADOR));

        assertEquals("ana", ana.getLogin());
        assertNotEquals("minhasenha", ana.getSenhaHash());
    }

    @Test
    @DisplayName("Login certo autentica; login sem diferenciar maiúsculas")
    void loginCerto() throws Exception {
        cadastrar.executar(dona, new CadastrarOperador.Comando("Ana", "ana", "minhasenha", PerfilOperador.OPERADOR));

        assertEquals("Ana", autenticar.executar("ANA", "minhasenha").getNome());
    }

    @Test
    @DisplayName("Senha errada e login inexistente dão a MESMA mensagem (não revela quais logins existem)")
    void credenciaisInvalidas() {
        CredenciaisInvalidasException senhaErrada = assertThrows(CredenciaisInvalidasException.class,
                () -> autenticar.executar("marta", "errada"));
        CredenciaisInvalidasException loginInexistente = assertThrows(CredenciaisInvalidasException.class,
                () -> autenticar.executar("ninguem", "segredo1"));

        assertEquals(senhaErrada.getMessage(), loginInexistente.getMessage());
        assertThrows(CredenciaisInvalidasException.class, () -> autenticar.executar("", ""));
    }

    @Test
    @DisplayName("Só administrador cadastra; senha curta e login repetido são recusados")
    void regrasDeCadastro() throws Exception {
        Operador ana = cadastrar.executar(dona,
                new CadastrarOperador.Comando("Ana", "ana", "minhasenha", PerfilOperador.OPERADOR));

        assertThrows(OperacaoNaoAutorizadaException.class, () -> cadastrar.executar(ana,
                new CadastrarOperador.Comando("Bruno", "bruno", "outrasenha", PerfilOperador.OPERADOR)));
        assertThrows(SenhaFracaException.class, () -> cadastrar.executar(dona,
                new CadastrarOperador.Comando("Bruno", "bruno", "12345", PerfilOperador.OPERADOR)));
        assertThrows(RegistroDuplicadoException.class, () -> cadastrar.executar(dona,
                new CadastrarOperador.Comando("Outra Ana", " ANA ", "outrasenha", PerfilOperador.OPERADOR)));
    }

    @Test
    @DisplayName("Operador cadastrado pelo administrador é obrigado a trocar a senha no primeiro login")
    void cadastroExigeTroca() throws Exception {
        Operador ana = cadastrar.executar(dona,
                new CadastrarOperador.Comando("Ana", "ana", "minhasenha", PerfilOperador.OPERADOR));

        assertTrue(ana.deveTrocarSenha());
        trocarSenha.executar(ana, "minhasenha", "sóeusei");

        assertFalse(ana.deveTrocarSenha());
        assertEquals("Ana", autenticar.executar("ana", "sóeusei").getNome());
        assertThrows(CredenciaisInvalidasException.class, () -> autenticar.executar("ana", "minhasenha"));
    }

    @Test
    @DisplayName("Troca de senha: atual errada, nova curta ou igual à atual são recusadas sem alterar nada")
    void regrasDaTrocaDeSenha() throws Exception {
        String hashAntes = dona.getSenhaHash();

        assertThrows(CredenciaisInvalidasException.class, () -> trocarSenha.executar(dona, "errada", "novasenha"));
        assertThrows(SenhaFracaException.class, () -> trocarSenha.executar(dona, "segredo1", "12345"));
        assertThrows(SenhaFracaException.class, () -> trocarSenha.executar(dona, "segredo1", "segredo1"));

        assertEquals(hashAntes, dona.getSenhaHash());
    }
}
