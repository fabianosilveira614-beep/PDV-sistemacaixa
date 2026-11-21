package br.com.ferragem.infraestrutura.persistencia.jdbc;

import br.com.ferragem.aplicacao.porta.OperadorRepository;
import br.com.ferragem.dominio.modelo.caixa.Operador;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Operadores do caixa no PostgreSQL (tabela {@code operador}). Só operadores ativos conseguem
 * ser encontrados pelo login, portanto só eles conseguem entrar no sistema.
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
        String sql = "SELECT " + Mapeadores.COLUNAS_OPERADOR + " FROM operador o WHERE o.login = ? AND o.ativo";
        return contexto.usar(conexao -> {
            try (PreparedStatement comando = conexao.prepareStatement(sql)) {
                comando.setString(1, login.trim().toLowerCase(Locale.ROOT));
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
                        "INSERT INTO operador (nome, login, senha_hash, perfil, trocar_senha) VALUES (?, ?, ?, ?, ?) RETURNING id")) {
                    preencher(comando, operador);
                    operador.atribuirId(Mapeadores.idGerado(comando));
                }
            } else {
                try (PreparedStatement comando = conexao.prepareStatement(
                        "UPDATE operador SET nome = ?, login = ?, senha_hash = ?, perfil = ?, trocar_senha = ? WHERE id = ?")) {
                    preencher(comando, operador);
                    comando.setLong(6, operador.getId().orElseThrow());
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
    }
}
