package br.com.ferragem.infraestrutura.persistencia.jdbc;

import br.com.ferragem.aplicacao.porta.OperadorRepository;
import br.com.ferragem.dominio.modelo.caixa.Operador;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Operadores do caixa no PostgreSQL (tabela {@code operador}). As buscas trazem também os
 * desativados: barrar o login deles é regra do caso de uso {@code AutenticarOperador}, não do SQL.
 */
public class OperadorRepositoryJdbc implements OperadorRepository {

    private final ContextoJdbc contexto;

    /**
     * @param contexto acesso ao banco; não pode ser nulo
     */
    public OperadorRepositoryJdbc(ContextoJdbc contexto) {
        this.contexto = Objects.requireNonNull(contexto, "contexto");
    }

    @Override
    public Optional<Operador> buscarPorLogin(String login) {
        return buscarUm("o.login = ?", login.trim().toLowerCase(Locale.ROOT));
    }

    @Override
    public Optional<Operador> buscarPorId(long id) {
        return buscarUm("o.id = ?", id);
    }

    @Override
    public List<Operador> listar() {
        String sql = "SELECT " + Mapeadores.COLUNAS_OPERADOR + " FROM operador o ORDER BY o.nome";
        return contexto.usar(conexao -> {
            try (PreparedStatement comando = conexao.prepareStatement(sql);
                 ResultSet linha = comando.executeQuery()) {
                List<Operador> todos = new ArrayList<>();
                while (linha.next()) {
                    todos.add(Mapeadores.operador(linha));
                }
                return todos;
            }
        });
    }

    private Optional<Operador> buscarUm(String filtro, Object valor) {
        String sql = "SELECT " + Mapeadores.COLUNAS_OPERADOR + " FROM operador o WHERE " + filtro;
        return contexto.usar(conexao -> {
            try (PreparedStatement comando = conexao.prepareStatement(sql)) {
                comando.setObject(1, valor);
                try (ResultSet linha = comando.executeQuery()) {
                    return linha.next() ? Optional.of(Mapeadores.operador(linha)) : Optional.empty();
                }
            }
        });
    }

    @Override
    public void salvar(Operador operador) {
        contexto.usar(conexao -> {
            if (operador.getId().isEmpty()) {
                try (PreparedStatement comando = conexao.prepareStatement(
                        "INSERT INTO operador (nome, login, senha_hash, perfil, trocar_senha, ativo) "
                                + "VALUES (?, ?, ?, ?, ?, ?) RETURNING id")) {
                    preencher(comando, operador);
                    operador.atribuirId(Mapeadores.idGerado(comando));
                }
            } else {
                try (PreparedStatement comando = conexao.prepareStatement(
                        "UPDATE operador SET nome = ?, login = ?, senha_hash = ?, perfil = ?, trocar_senha = ?, "
                                + "ativo = ? WHERE id = ?")) {
                    preencher(comando, operador);
                    comando.setLong(7, operador.getId().orElseThrow());
                    comando.executeUpdate();
                }
            }
            return null;
        });
    }

    private static void preencher(PreparedStatement comando, Operador operador) throws java.sql.SQLException {
        comando.setString(1, operador.getNome());
        comando.setString(2, operador.getLogin());
        comando.setString(3, operador.getSenhaHash());
        comando.setString(4, operador.getPerfil().name());
        comando.setBoolean(5, operador.deveTrocarSenha());
        comando.setBoolean(6, operador.estaAtivo());
    }
}
