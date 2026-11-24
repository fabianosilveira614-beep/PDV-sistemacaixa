package br.com.ferragem.aplicacao.casodeuso;

import br.com.ferragem.aplicacao.memoria.CaixaRepositoryEmMemoria;
import br.com.ferragem.aplicacao.memoria.HashSenhaFalso;
import br.com.ferragem.aplicacao.memoria.OperadorRepositoryEmMemoria;
import br.com.ferragem.dominio.excecao.CredenciaisInvalidasException;
import br.com.ferragem.dominio.excecao.OperacaoNaoAutorizadaException;
import br.com.ferragem.dominio.excecao.RegistroDuplicadoException;
import br.com.ferragem.dominio.excecao.SenhaFracaException;
import br.com.ferragem.dominio.modelo.caixa.Operador;
import br.com.ferragem.dominio.modelo.caixa.PerfilOperador;
import br.com.ferragem.dominio.modelo.caixa.SessaoCaixa;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Testes de login, cadastro e administração de operadores. */
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

    // ---------------------------------------------------------------- administração

    private Operador ana() throws Exception {
        return cadastrar.executar(dona, new CadastrarOperador.Comando("Ana", "ana", "minhasenha", PerfilOperador.OPERADOR));
    }

    private long id(Operador operador) {
        return operador.getId().orElseThrow();
    }

    @Test
    @DisplayName("Desativado não entra (mesma mensagem de senha errada); reativado volta a entrar")
    void desativarEReativar() throws Exception {
        Operador ana = ana();
        DesativarOperador desativar = new DesativarOperador(operadores, new CaixaRepositoryEmMemoria());

        desativar.executar(dona, id(ana));
        CredenciaisInvalidasException barrada = assertThrows(CredenciaisInvalidasException.class,
                () -> autenticar.executar("ana", "minhasenha"));
        CredenciaisInvalidasException senhaErrada = assertThrows(CredenciaisInvalidasException.class,
                () -> autenticar.executar("marta", "errada"));
        assertEquals(senhaErrada.getMessage(), barrada.getMessage());

        new ReativarOperador(operadores).executar(dona, id(ana));
        assertEquals("Ana", autenticar.executar("ana", "minhasenha").getNome());
    }

    @Test
    @DisplayName("Ninguém desativa a si mesmo nem quem está com o caixa aberto; operador comum não administra")
    void protecoesDaDesativacao() throws Exception {
        Operador ana = ana();
        CaixaRepositoryEmMemoria caixas = new CaixaRepositoryEmMemoria();
        DesativarOperador desativar = new DesativarOperador(operadores, caixas);

        assertThrows(OperacaoNaoAutorizadaException.class, () -> desativar.executar(dona, id(dona)));
        assertThrows(OperacaoNaoAutorizadaException.class, () -> desativar.executar(ana, id(dona)));

        caixas.salvar(SessaoCaixa.abrir(ana, BigDecimal.TEN, LocalDateTime.of(2026, 11, 21, 8, 0)));
        OperacaoNaoAutorizadaException comCaixa = assertThrows(OperacaoNaoAutorizadaException.class,
                () -> desativar.executar(dona, id(ana)));
        assertTrue(comCaixa.getMessage().contains("caixa aberto"));

        assertTrue(dona.estaAtivo());
        assertTrue(ana.estaAtivo());
    }

    @Test
    @DisplayName("Administrador não tira o próprio perfil; muda nome e perfil dos outros; login não muda")
    void atualizacao() throws Exception {
        Operador ana = ana();
        AtualizarOperador atualizar = new AtualizarOperador(operadores);

        assertThrows(OperacaoNaoAutorizadaException.class, () -> atualizar.executar(dona,
                new AtualizarOperador.Comando(id(dona), "Marta", PerfilOperador.OPERADOR)));
        assertEquals(PerfilOperador.ADMINISTRADOR, dona.getPerfil());

        atualizar.executar(dona, new AtualizarOperador.Comando(id(dona), "Marta Souza", PerfilOperador.ADMINISTRADOR));
        atualizar.executar(dona, new AtualizarOperador.Comando(id(ana), "Ana Lima", PerfilOperador.ADMINISTRADOR));
        assertEquals("Marta Souza", dona.getNome());
        assertEquals("Ana Lima", ana.getNome());
        assertEquals("ana", ana.getLogin());
        assertTrue(ana.ehAdministrador());
        assertThrows(OperacaoNaoAutorizadaException.class, () -> atualizar.executar(
                new Operador("Zé", "ze", "h", PerfilOperador.OPERADOR),
                new AtualizarOperador.Comando(id(ana), "X", PerfilOperador.OPERADOR)));
    }

    @Test
    @DisplayName("Senha redefinida pelo administrador é provisória: entra com ela e é obrigado a trocar")
    void redefinicaoDeSenha() throws Exception {
        Operador ana = ana();
        trocarSenha.executar(ana, "minhasenha", "sóeusei");
        RedefinirSenha redefinir = new RedefinirSenha(operadores, hash);

        assertThrows(SenhaFracaException.class, () -> redefinir.executar(dona, id(ana), "123"));
        assertThrows(OperacaoNaoAutorizadaException.class, () -> redefinir.executar(dona, id(dona), "novasenha"));
        redefinir.executar(dona, id(ana), "provisoria");

        Operador entrou = autenticar.executar("ana", "provisoria");
        assertTrue(entrou.deveTrocarSenha());
        assertThrows(CredenciaisInvalidasException.class, () -> autenticar.executar("ana", "sóeusei"));
    }

    @Test
    @DisplayName("Lista traz ativos e desativados; login de desativado não pode ser recadastrado")
    void listaELoginDeDesativado() throws Exception {
        Operador ana = ana();
        new DesativarOperador(operadores, new CaixaRepositoryEmMemoria()).executar(dona, id(ana));

        List<Operador> todos = new ListarOperadores(operadores).executar(dona);
        assertEquals(List.of("Ana", "Dona Marta"), todos.stream().map(Operador::getNome).toList());
        assertThrows(OperacaoNaoAutorizadaException.class, () -> new ListarOperadores(operadores).executar(ana));

        RegistroDuplicadoException duplicado = assertThrows(RegistroDuplicadoException.class, () ->
                cadastrar.executar(dona, new CadastrarOperador.Comando("Ana 2", "ana", "outrasenha", PerfilOperador.OPERADOR)));
        assertTrue(duplicado.getMessage().contains("desativado"));
    }
}
