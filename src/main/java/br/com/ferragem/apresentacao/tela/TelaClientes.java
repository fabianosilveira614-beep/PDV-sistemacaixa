package br.com.ferragem.apresentacao.tela;

import br.com.ferragem.CasosDeUso;
import br.com.ferragem.aplicacao.casodeuso.ExcluirCliente;
import br.com.ferragem.aplicacao.casodeuso.ListarClientes.ResumoCliente;
import br.com.ferragem.apresentacao.Navegador;
import br.com.ferragem.apresentacao.comum.Avisos;
import br.com.ferragem.apresentacao.comum.Formatos;
import br.com.ferragem.apresentacao.comum.Tarefas;
import javafx.animation.PauseTransition;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Lista de clientes: busca, cadastro, exclusão e entrada para o perfil (dados, conta de fiado e
 * compras).
 *
 * <p><b>Teclado:</b> F2 novo · F3 busca · Enter abre o perfil · F9 receber fiado · Delete exclui ·
 * ↑/↓ navegam (também a partir do campo de busca).</p>
 *
 * <p>O CPF não aparece nesta lista (só no cadastro e no perfil), mas a busca aceita CPF. O "total
 * devido" vem do {@link br.com.ferragem.aplicacao.casodeuso.ListarClientes}, que usa a mesma
 * {@code ContaFiado} do pagamento: a tela não soma nada sozinha.</p>
 */
public final class TelaClientes {

    private final CasosDeUso casos;
    private final Navegador navegador;
    private final VBox raiz = new VBox(12);
    private final TextField busca = new TextField();
    private final TableView<ResumoCliente> tabela = new TableView<>();
    private final Label mensagem = new Label();
    private final PauseTransition esperaDigitacao = new PauseTransition(Duration.millis(300));

    /**
     * @param casos     casos de uso
     * @param navegador para abrir o perfil
     */
    public TelaClientes(CasosDeUso casos, Navegador navegador) {
        this.casos = casos;
        this.navegador = navegador;
        montar();
        raiz.addEventFilter(KeyEvent.KEY_PRESSED, this::aoPressionarTecla);
        recarregar();
    }

    /** @return conteúdo da tela */
    public Parent raiz() {
        return raiz;
    }

    // ------------------------------------------------------------------ montagem

    private void montar() {
        Label titulo = new Label("Clientes");
        titulo.getStyleClass().add("titulo-secao");
        busca.setPromptText("Buscar por nome ou CPF (F3)");
        busca.setPrefWidth(320);
        esperaDigitacao.setOnFinished(evento -> recarregar());
        busca.textProperty().addListener((obs, antes, agora) -> esperaDigitacao.playFromStart());

        Button novo = new Button("Novo cliente (F2)");
        novo.getStyleClass().add("botao-primario");
        novo.setOnAction(evento -> novoCliente());
        Region espaco = new Region();
        HBox.setHgrow(espaco, Priority.ALWAYS);
        HBox topo = new HBox(12, titulo, espaco, busca, novo);
        topo.setAlignment(Pos.CENTER_LEFT);

        montarTabela();

        Button perfil = new Button("Abrir perfil (Enter)");
        Button receber = new Button("Receber fiado (F9)");
        Button excluir = new Button("Excluir (Del)");
        for (Button botao : List.of(perfil, receber, excluir)) {
            botao.getStyleClass().add("botao-secundario");
            botao.disableProperty().bind(tabela.getSelectionModel().selectedItemProperty().isNull());
        }
        perfil.setOnAction(evento -> abrirPerfil(TelaPerfilCliente.Aba.DADOS));
        receber.setOnAction(evento -> abrirPerfil(TelaPerfilCliente.Aba.CONTA_FIADO));
        excluir.setOnAction(evento -> selecionado().ifPresent(this::excluirCliente));
        Label legenda = new Label("Vermelho: cliente com fiado em aberto.");
        legenda.getStyleClass().add("dica");
        Region espacoAcoes = new Region();
        HBox.setHgrow(espacoAcoes, Priority.ALWAYS);
        HBox acoes = new HBox(8, perfil, receber, excluir, espacoAcoes, legenda);
        acoes.setAlignment(Pos.CENTER_LEFT);

        mensagem.setWrapText(true);
        raiz.getChildren().addAll(topo, tabela, acoes, mensagem);
        VBox.setVgrow(tabela, Priority.ALWAYS);
        raiz.setPadding(new Insets(16, 24, 16, 24));
    }

    private void montarTabela() {
        tabela.setPlaceholder(new Label("Nenhum cliente. Use \"Novo cliente\" para cadastrar."));
        tabela.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tabela.getColumns().add(coluna("Nome", 320, r -> r.cliente().getNome()));
        tabela.getColumns().add(coluna("Telefone", 160, r -> r.cliente().getTelefone().orElse("")));
        TableColumn<ResumoCliente, String> devido = coluna("Total devido", 140, r -> Formatos.moeda(r.totalDevido()));
        devido.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(String texto, boolean vazio) {
                super.updateItem(texto, vazio);
                setText(vazio ? null : texto);
                getStyleClass().remove("valor-devedor");
                ResumoCliente linha = vazio || getIndex() < 0 || getIndex() >= getTableView().getItems().size()
                        ? null : getTableView().getItems().get(getIndex());
                if (linha != null && linha.totalDevido().compareTo(BigDecimal.ZERO) > 0) {
                    getStyleClass().add("valor-devedor");
                }
            }
        });
        tabela.getColumns().add(devido);
        tabela.getColumns().add(coluna("Compras em aberto", 150, r -> String.valueOf(r.comprasEmAberto())));

        tabela.setOnMouseClicked(evento -> {
            if (evento.getButton() == MouseButton.PRIMARY && evento.getClickCount() == 2) {
                abrirPerfil(TelaPerfilCliente.Aba.DADOS);
            }
        });
        tabela.setOnKeyPressed(evento -> {
            if (evento.getCode() == KeyCode.ENTER) {
                abrirPerfil(TelaPerfilCliente.Aba.DADOS);
            }
        });
    }

    private static TableColumn<ResumoCliente, String> coluna(String titulo, double largura,
                                                             Function<ResumoCliente, String> texto) {
        TableColumn<ResumoCliente, String> coluna = new TableColumn<>(titulo);
        coluna.setCellValueFactory(dados -> new ReadOnlyStringWrapper(texto.apply(dados.getValue())));
        coluna.setPrefWidth(largura);
        return coluna;
    }

    // ------------------------------------------------------------------ teclado

    private void aoPressionarTecla(KeyEvent evento) {
        boolean digitando = evento.getTarget() instanceof TextInputControl;
        switch (evento.getCode()) {
            case F2 -> novoCliente();
            case F3 -> {
                busca.requestFocus();
                busca.selectAll();
            }
            case F9 -> abrirPerfil(TelaPerfilCliente.Aba.CONTA_FIADO);
            case DELETE -> {
                if (digitando) {
                    return; // no campo de busca, Delete apaga a letra
                }
                selecionado().ifPresent(this::excluirCliente);
            }
            case UP, DOWN -> {
                if (!digitando) {
                    return;
                }
                moverSelecao(evento.getCode() == KeyCode.UP ? -1 : 1);
                tabela.requestFocus();
            }
            case ENTER -> {
                if (!digitando) {
                    return; // na tabela, Enter abre o perfil (tratado na própria tabela)
                }
                moverSelecao(0);
                tabela.requestFocus();
            }
            default -> {
                return;
            }
        }
        evento.consume();
    }

    private void moverSelecao(int passo) {
        int total = tabela.getItems().size();
        if (total == 0) {
            return;
        }
        int atual = tabela.getSelectionModel().getSelectedIndex();
        int nova = atual < 0 ? 0 : Math.max(0, Math.min(total - 1, atual + passo));
        tabela.getSelectionModel().select(nova);
        tabela.scrollTo(nova);
    }

    // ------------------------------------------------------------------ ações

    private void recarregar() {
        String termo = busca.getText();
        Tarefas.emSegundoPlano(tabela, () -> casos.listarClientes().executar(termo), this::exibir,
                erro -> Avisos.tratar(erro, mensagem));
    }

    private void exibir(List<ResumoCliente> clientes) {
        Long antes = selecionado().flatMap(r -> r.cliente().getId()).orElse(null);
        tabela.getItems().setAll(clientes);
        if (antes != null) {
            clientes.stream().filter(r -> r.cliente().getId().orElse(-1L).equals(antes)).findFirst()
                    .ifPresent(r -> tabela.getSelectionModel().select(r));
        }
    }

    private Optional<ResumoCliente> selecionado() {
        return Optional.ofNullable(tabela.getSelectionModel().getSelectedItem());
    }

    private void abrirPerfil(TelaPerfilCliente.Aba aba) {
        selecionado().ifPresent(r -> navegador.abrirPerfilCliente(r.cliente(), aba));
    }

    private void novoCliente() {
        new DialogoCliente(casos, null).mostrar().ifPresent(cliente -> {
            Avisos.sucesso(mensagem, "Cliente \"" + cliente.getNome() + "\" cadastrado.");
            recarregar();
        });
    }

    private void excluirCliente(ResumoCliente resumo) {
        String nome = resumo.cliente().getNome();
        if (!Avisos.confirmar("Excluir cliente", "Excluir \"" + nome + "\"?\n\n"
                + "Quem ainda deve fiado não pode ser excluído. Quem já comprou é retirado da lista, mas "
                + "fica guardado para não apagar o histórico de vendas.")) {
            return;
        }
        long id = resumo.cliente().getId().orElseThrow();
        Tarefas.emSegundoPlano(tabela, () -> casos.excluirCliente().executar(id), resultado -> {
            Avisos.sucesso(mensagem, resultado == ExcluirCliente.Resultado.EXCLUIDO
                    ? "\"" + nome + "\" excluído."
                    : "\"" + nome + "\" já tinha compras: foi retirado da lista, mas o histórico continua guardado.");
            recarregar();
        }, erro -> Avisos.tratar(erro, mensagem));
    }
}
