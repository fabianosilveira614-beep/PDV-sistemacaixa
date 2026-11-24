package br.com.ferragem.apresentacao.tela;

import br.com.ferragem.CasosDeUso;
import br.com.ferragem.apresentacao.Navegador;
import br.com.ferragem.apresentacao.comum.Avisos;
import br.com.ferragem.apresentacao.comum.Sessao;
import br.com.ferragem.apresentacao.comum.Tarefas;
import br.com.ferragem.dominio.modelo.caixa.Operador;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Administração de quem usa o sistema (só administrador).
 *
 * <p><b>Teclado:</b> F2 novo · Enter edita · F6 redefine a senha · F7 desativa/reativa ·
 * ↑/↓ navegam. Não existe "excluir": operador só é desativado, porque aparece no histórico dos
 * caixas.</p>
 */
public final class TelaOperadores {

    private final CasosDeUso casos;
    private final Sessao sessao;
    private final Navegador navegador;
    private final VBox raiz = new VBox(12);
    private final TableView<Operador> tabela = new TableView<>();
    private final Button desativarOuReativar = new Button("Desativar (F7)");
    private final Label mensagem = new Label();

    /**
     * @param casos     casos de uso
     * @param sessao    para saber quem está administrando
     * @param navegador para atualizar o nome no menu se o administrador editar a si mesmo
     */
    public TelaOperadores(CasosDeUso casos, Sessao sessao, Navegador navegador) {
        this.casos = casos;
        this.sessao = sessao;
        this.navegador = navegador;
        montar();
        raiz.addEventFilter(KeyEvent.KEY_PRESSED, this::aoPressionarTecla);
        recarregar();
    }

    /** @return conteúdo da tela */
    public Parent raiz() {
        return raiz;
    }

    private void montar() {
        Label titulo = new Label("Operadores");
        titulo.getStyleClass().add("titulo-secao");
        Button novo = new Button("Novo operador (F2)");
        novo.getStyleClass().add("botao-primario");
        novo.setOnAction(evento -> novoOperador());
        Region espaco = new Region();
        HBox.setHgrow(espaco, Priority.ALWAYS);
        HBox topo = new HBox(12, titulo, espaco, novo);
        topo.setAlignment(Pos.CENTER_LEFT);

        tabela.setPlaceholder(new Label("Nenhum operador."));
        tabela.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tabela.getColumns().add(coluna("Nome", 280, Operador::getNome));
        tabela.getColumns().add(coluna("Login", 160, Operador::getLogin));
        tabela.getColumns().add(coluna("Perfil", 140, o -> DialogoOperador.descrever(o.getPerfil())));
        tabela.getColumns().add(coluna("Situação", 220, this::situacao));
        tabela.setRowFactory(t -> new TableRow<>() {
            @Override
            protected void updateItem(Operador operador, boolean vazio) {
                super.updateItem(operador, vazio);
                getStyleClass().remove("linha-inativa");
                if (!vazio && operador != null && !operador.estaAtivo()) {
                    getStyleClass().add("linha-inativa");
                }
            }
        });
        tabela.setOnMouseClicked(evento -> {
            if (evento.getButton() == MouseButton.PRIMARY && evento.getClickCount() == 2) {
                selecionado().ifPresent(this::editar);
            }
        });
        tabela.setOnKeyPressed(evento -> {
            if (evento.getCode() == KeyCode.ENTER) {
                selecionado().ifPresent(this::editar);
            }
        });

        Button editar = new Button("Editar (Enter)");
        Button senha = new Button("Redefinir senha (F6)");
        for (Button botao : List.of(editar, senha, desativarOuReativar)) {
            botao.getStyleClass().add("botao-secundario");
            botao.disableProperty().bind(tabela.getSelectionModel().selectedItemProperty().isNull());
        }
        editar.setOnAction(evento -> selecionado().ifPresent(this::editar));
        senha.setOnAction(evento -> selecionado().ifPresent(this::redefinirSenha));
        desativarOuReativar.setOnAction(evento -> selecionado().ifPresent(this::desativarOuReativar));
        tabela.getSelectionModel().selectedItemProperty().addListener((obs, antes, operador) ->
                desativarOuReativar.setText(operador != null && !operador.estaAtivo() ? "Reativar (F7)" : "Desativar (F7)"));
        Label legenda = new Label("Cinza: desativado (não entra no sistema, mas continua no histórico).");
        legenda.getStyleClass().add("dica");
        Region espacoAcoes = new Region();
        HBox.setHgrow(espacoAcoes, Priority.ALWAYS);
        HBox acoes = new HBox(8, editar, senha, desativarOuReativar, espacoAcoes, legenda);
        acoes.setAlignment(Pos.CENTER_LEFT);

        mensagem.setWrapText(true);
        raiz.getChildren().addAll(topo, tabela, acoes, mensagem);
        VBox.setVgrow(tabela, Priority.ALWAYS);
        raiz.setPadding(new Insets(16, 24, 16, 24));
    }

    private String situacao(Operador operador) {
        if (!operador.estaAtivo()) {
            return "Desativado";
        }
        if (operador.ehOMesmoQue(sessao.operador())) {
            return "Ativo (você)";
        }
        return operador.deveTrocarSenha() ? "Ativo · trocará a senha" : "Ativo";
    }

    private static TableColumn<Operador, String> coluna(String titulo, double largura, Function<Operador, String> texto) {
        TableColumn<Operador, String> coluna = new TableColumn<>(titulo);
        coluna.setCellValueFactory(dados -> new ReadOnlyStringWrapper(texto.apply(dados.getValue())));
        coluna.setPrefWidth(largura);
        return coluna;
    }

    private void aoPressionarTecla(KeyEvent evento) {
        switch (evento.getCode()) {
            case F2 -> novoOperador();
            case F6 -> selecionado().ifPresent(this::redefinirSenha);
            case F7 -> selecionado().ifPresent(this::desativarOuReativar);
            default -> {
                return;
            }
        }
        evento.consume();
    }

    private void recarregar() {
        Operador quem = sessao.operador();
        Tarefas.emSegundoPlano(tabela, () -> casos.listarOperadores().executar(quem), operadores -> {
            String antes = selecionado().map(Operador::getLogin).orElse(null);
            tabela.getItems().setAll(operadores);
            operadores.stream().filter(o -> o.getLogin().equals(antes)).findFirst()
                    .ifPresentOrElse(o -> tabela.getSelectionModel().select(o),
                            () -> tabela.getSelectionModel().selectFirst());
            tabela.requestFocus();
        }, erro -> Avisos.tratar(erro, mensagem));
    }

    private Optional<Operador> selecionado() {
        return Optional.ofNullable(tabela.getSelectionModel().getSelectedItem());
    }

    private void novoOperador() {
        new DialogoOperador(casos, sessao.operador(), null).mostrar().ifPresent(operador -> {
            Avisos.sucesso(mensagem, "\"" + operador.getNome() + "\" cadastrado. No primeiro acesso, "
                    + "ele entra com a senha provisória e cria a própria.");
            recarregar();
        });
    }

    private void editar(Operador operador) {
        new DialogoOperador(casos, sessao.operador(), operador).mostrar().ifPresent(atualizado -> {
            if (atualizado.ehOMesmoQue(sessao.operador())) {
                navegador.aoAlterarOperadorLogado(atualizado);
            }
            Avisos.sucesso(mensagem, "\"" + atualizado.getNome() + "\" atualizado.");
            recarregar();
        });
    }

    private void redefinirSenha(Operador operador) {
        if (new DialogoSenhaProvisoria(casos, sessao.operador(), operador).mostrar()) {
            Avisos.sucesso(mensagem, "Senha de \"" + operador.getNome() + "\" redefinida. Ela vale só até o "
                    + "próximo acesso, quando ele criará uma nova.");
            recarregar();
        }
    }

    private void desativarOuReativar(Operador operador) {
        Operador quem = sessao.operador();
        long id = operador.getId().orElseThrow();
        if (operador.estaAtivo()) {
            if (!Avisos.confirmar("Desativar operador", "Desativar \"" + operador.getNome() + "\"? Ele não "
                    + "conseguirá mais entrar, mas continua no histórico dos caixas e pode ser reativado.")) {
                return;
            }
            Tarefas.emSegundoPlano(tabela, () -> {
                casos.desativarOperador().executar(quem, id);
                return null;
            }, nada -> {
                Avisos.sucesso(mensagem, "\"" + operador.getNome() + "\" desativado.");
                recarregar();
            }, erro -> Avisos.tratar(erro, mensagem));
        } else {
            Tarefas.emSegundoPlano(tabela, () -> {
                casos.reativarOperador().executar(quem, id);
                return null;
            }, nada -> {
                Avisos.sucesso(mensagem, "\"" + operador.getNome() + "\" pode entrar de novo.");
                recarregar();
            }, erro -> Avisos.tratar(erro, mensagem));
        }
    }
}
