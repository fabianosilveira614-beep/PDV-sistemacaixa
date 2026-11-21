package br.com.ferragem.infraestrutura.persistencia.jdbc;

import br.com.ferragem.aplicacao.porta.CaixaRepository;
import br.com.ferragem.dominio.excecao.RegraNegocioException;
import br.com.ferragem.dominio.modelo.caixa.MovimentoCaixa;
import br.com.ferragem.dominio.modelo.caixa.Operador;
import br.com.ferragem.dominio.modelo.caixa.SessaoCaixa;
import br.com.ferragem.dominio.modelo.caixa.TipoMovimentoCaixa;
import br.com.ferragem.dominio.modelo.venda.FormaPagamento;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Sessões de caixa no PostgreSQL (tabelas {@code sessao_caixa} e {@code movimento_caixa}).
 *
 * <p><b>Como a sessão é reconstruída:</b> o repositório abre a sessão com
 * {@link SessaoCaixa#abrir} e "repete" cada movimento gravado, na ordem, chamando os mesmos
 * métodos que o operador chamou (registrar venda, suprimento, sangria...). Não existe um
 * setter de saldo: o saldo é sempre recalculado a partir dos movimentos, como no domínio.
 * Se o banco tiver uma sangria maior que o dinheiro da gaveta, a leitura falha.</p>
 *
 * <p><b>Como a sessão é salva:</b> os movimentos são só acrescentados, nunca alterados. O
 * repositório conta quantos já estão no banco e insere apenas os novos.</p>
 */
public class CaixaRepositoryJdbc implements CaixaRepository {

    private final ContextoJdbc contexto;

    /**
     * @param contexto acesso ao banco; não pode ser nulo
     */
    public CaixaRepositoryJdbc(ContextoJdbc contexto) {
        this.contexto = Objects.requireNonNull(contexto, "contexto");
    }

    @Override
    public Optional<SessaoCaixa> buscarAberta() {
        String sql = "SELECT s.id, s.aberto_em, s.valor_abertura, " + Mapeadores.COLUNAS_OPERADOR + """
                 FROM sessao_caixa s JOIN operador o ON o.id = s.operador_id
                WHERE s.status = 'ABERTO'""";
        return contexto.usar(conexao -> {
            try (PreparedStatement comando = conexao.prepareStatement(sql);
                 ResultSet linha = comando.executeQuery()) {
                if (!linha.next()) {
                    return Optional.empty();
                }
                long id = linha.getLong("id");
                Operador operador = Mapeadores.operador(linha);
                try {
                    SessaoCaixa sessao = SessaoCaixa.abrir(operador, linha.getBigDecimal("valor_abertura"),
                            linha.getObject("aberto_em", LocalDateTime.class));
                    repetirMovimentos(conexao, id, sessao);
                    sessao.atribuirId(id);
                    return Optional.of(sessao);
                } catch (RegraNegocioException e) {
                    throw Mapeadores.dadoInvalido("caixa", id, e);
                }
            }
        });
    }

    @Override
    public void salvar(SessaoCaixa sessao) {
        contexto.usar(conexao -> {
            if (sessao.getId().isEmpty()) {
                inserir(conexao, sessao);
            } else {
                atualizar(conexao, sessao);
            }
            inserirMovimentosNovos(conexao, sessao);
            return null;
        });
    }

    private static void repetirMovimentos(Connection conexao, long sessaoId, SessaoCaixa sessao)
            throws SQLException, RegraNegocioException {
        String sql = """
                SELECT tipo, forma_pagamento, valor, descricao, data_hora
                  FROM movimento_caixa WHERE sessao_caixa_id = ? ORDER BY id""";
        try (PreparedStatement comando = conexao.prepareStatement(sql)) {
            comando.setLong(1, sessaoId);
            try (ResultSet linha = comando.executeQuery()) {
                while (linha.next()) {
                    FormaPagamento forma = FormaPagamento.valueOf(linha.getString("forma_pagamento"));
                    BigDecimal valor = linha.getBigDecimal("valor");
                    String descricao = linha.getString("descricao");
                    LocalDateTime momento = linha.getObject("data_hora", LocalDateTime.class);
                    switch (TipoMovimentoCaixa.valueOf(linha.getString("tipo"))) {
                        case ENTRADA_VENDA -> sessao.registrarVenda(forma, valor, descricao, momento);
                        case ENTRADA_PAGAMENTO_DEBITO ->
                                sessao.registrarRecebimentoFiado(forma, valor, descricao, momento);
                        case SUPRIMENTO -> sessao.registrarSuprimento(valor, descricao, momento);
                        case SANGRIA -> sessao.registrarSangria(valor, descricao, momento);
                    }
                }
            }
        }
    }

    private static void inserir(Connection conexao, SessaoCaixa sessao) throws SQLException {
        String sql = """
                INSERT INTO sessao_caixa (operador_id, aberto_em, valor_abertura, fechado_em, valor_contado, status)
                VALUES (?, ?, ?, ?, ?, ?) RETURNING id""";
        try (PreparedStatement comando = conexao.prepareStatement(sql)) {
            comando.setLong(1, sessao.getOperador().getId().orElseThrow(() ->
                    new IllegalStateException("Operador sem id não pode abrir caixa.")));
            comando.setObject(2, sessao.getAbertoEm());
            comando.setBigDecimal(3, sessao.getValorAbertura());
            Mapeadores.definirOuNulo(comando, 4, sessao.getFechadoEm().orElse(null), Types.TIMESTAMP);
            Mapeadores.definirOuNulo(comando, 5, sessao.getValorContado().orElse(null), Types.NUMERIC);
            comando.setString(6, sessao.getStatus().name());
            sessao.atribuirId(Mapeadores.idGerado(comando));
        }
    }

    private static void atualizar(Connection conexao, SessaoCaixa sessao) throws SQLException {
        String sql = "UPDATE sessao_caixa SET fechado_em = ?, valor_contado = ?, status = ? WHERE id = ?";
        try (PreparedStatement comando = conexao.prepareStatement(sql)) {
            Mapeadores.definirOuNulo(comando, 1, sessao.getFechadoEm().orElse(null), Types.TIMESTAMP);
            Mapeadores.definirOuNulo(comando, 2, sessao.getValorContado().orElse(null), Types.NUMERIC);
            comando.setString(3, sessao.getStatus().name());
            comando.setLong(4, sessao.getId().orElseThrow());
            comando.executeUpdate();
        }
    }

    private static void inserirMovimentosNovos(Connection conexao, SessaoCaixa sessao) throws SQLException {
        long sessaoId = sessao.getId().orElseThrow();
        List<MovimentoCaixa> todos = sessao.getMovimentos();
        int jaGravados = contarMovimentos(conexao, sessaoId);
        String sql = """
                INSERT INTO movimento_caixa (sessao_caixa_id, tipo, forma_pagamento, valor, descricao, data_hora)
                VALUES (?, ?, ?, ?, ?, ?)""";
        try (PreparedStatement comando = conexao.prepareStatement(sql)) {
            for (MovimentoCaixa movimento : todos.subList(jaGravados, todos.size())) {
                comando.setLong(1, sessaoId);
                comando.setString(2, movimento.tipo().name());
                comando.setString(3, movimento.forma().name());
                comando.setBigDecimal(4, movimento.valor());
                comando.setString(5, movimento.descricao());
                comando.setObject(6, movimento.dataHora());
                comando.addBatch();
            }
            comando.executeBatch();
        }
    }

    private static int contarMovimentos(Connection conexao, long sessaoId) throws SQLException {
        try (PreparedStatement comando = conexao.prepareStatement(
                "SELECT count(*) FROM movimento_caixa WHERE sessao_caixa_id = ?")) {
            comando.setLong(1, sessaoId);
            try (ResultSet resultado = comando.executeQuery()) {
                resultado.next();
                return resultado.getInt(1);
            }
        }
    }
}
