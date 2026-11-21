package br.com.ferragem.dominio.modelo.cliente;

import br.com.ferragem.dominio.excecao.DadoObrigatorioException;

import java.util.Optional;

/**
 * Cliente cadastrado da ferragem. Só clientes cadastrados e ativos podem comprar fiado (RN01).
 *
 * <p>Nome é obrigatório (RN16); CPF e telefone são opcionais. O CPF chega já validado, porque
 * é um {@link Cpf}: esta classe não precisa saber como se valida um CPF.</p>
 */
public class Cliente {

    private Long id;
    private String nome;
    private Cpf cpf;
    private String telefone;
    private boolean ativo;

    /**
     * Cadastra um cliente ativo.
     *
     * @param nome     nome do cliente, obrigatório
     * @param cpf      CPF já validado; pode ser nulo (cliente sem CPF)
     * @param telefone telefone para contato; pode ser nulo ou vazio
     * @throws DadoObrigatorioException se o nome estiver vazio
     */
    public Cliente(String nome, Cpf cpf, String telefone) throws DadoObrigatorioException {
        atualizarDados(nome, cpf, telefone);
        this.ativo = true;
    }

    /**
     * Corrige os dados do cliente (nome digitado errado, telefone novo).
     *
     * @param novoNome     nome, obrigatório
     * @param novoCpf      CPF já validado; nulo remove o CPF
     * @param novoTelefone telefone; nulo ou vazio remove o telefone
     * @throws DadoObrigatorioException se o nome estiver vazio; nada é alterado
     */
    public final void atualizarDados(String novoNome, Cpf novoCpf, String novoTelefone)
            throws DadoObrigatorioException {
        if (novoNome == null || novoNome.isBlank()) {
            throw new DadoObrigatorioException("nome do cliente");
        }
        this.nome = novoNome.trim();
        this.cpf = novoCpf;
        this.telefone = (novoTelefone == null || novoTelefone.isBlank()) ? null : novoTelefone.trim();
    }

    /**
     * Desativa o cliente (exclusão lógica, RN12). Ele deixa de poder comprar fiado, mas o
     * histórico de compras é preservado.
     */
    public final void inativar() {
        this.ativo = false;
    }

    /**
     * Registra o identificador gerado pelo banco. Chamado uma única vez pelo repositório.
     *
     * @param idGerado identificador positivo
     * @throws IllegalStateException    se o cliente já tiver um id
     * @throws IllegalArgumentException se o id não for positivo
     */
    public final void atribuirId(long idGerado) {
        if (this.id != null) {
            throw new IllegalStateException("O cliente já possui id " + this.id + ".");
        }
        if (idGerado <= 0) {
            throw new IllegalArgumentException("O id deve ser positivo: " + idGerado);
        }
        this.id = idGerado;
    }

    /** @return id gerado pelo banco, ou vazio se ainda não foi salvo */
    public Optional<Long> getId() {
        return Optional.ofNullable(id);
    }

    /** @return nome do cliente */
    public String getNome() {
        return nome;
    }

    /** @return CPF, se informado */
    public Optional<Cpf> getCpf() {
        return Optional.ofNullable(cpf);
    }

    /** @return telefone, se informado */
    public Optional<String> getTelefone() {
        return Optional.ofNullable(telefone);
    }

    /** @return {@code true} se o cliente está ativo (pode comprar fiado) */
    public boolean estaAtivo() {
        return ativo;
    }

    /**
     * Representação para logs e depuração. <b>Não inclui CPF nem telefone</b>: são dados pessoais
     * e só devem aparecer no cadastro e no perfil do cliente (princípio da necessidade, LGPD).
     *
     * @return id e nome do cliente
     */
    @Override
    public String toString() {
        return "Cliente{id=" + id + ", nome='" + nome + "'}";
    }
}
