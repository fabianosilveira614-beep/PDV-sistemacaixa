package br.com.ferragem.apresentacao.tela;

import br.com.ferragem.CasosDeUso;
import br.com.ferragem.aplicacao.casodeuso.ExcluirProduto;
import br.com.ferragem.apresentacao.comum.Avisos;
import br.com.ferragem.apresentacao.comum.Formatos;
import br.com.ferragem.apresentacao.comum.Tarefas;
import br.com.ferragem.dominio.modelo.produto.Produto;
import javafx.animation.PauseTransition;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
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

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Cadastro de produtos: lista com busca, novo, edição, ajuste de estoque, inativação e reativação.
 *
 * <p><b>Teclado:</b> F2 novo · F3 busca · Enter edita · F6 ajusta estoque · F7 inativa/reativa ·
 * Delete exclui · ↑/↓ navegam (também a partir do campo de busca).</p>
 *
 * <p>A lista destaca em vermelho o que está no estoque mínimo ou abaixo dele
 * ({@link Produto#precisaRepor()}: a regra é do produto, a tela só pinta) e em cinza o que está
 * fora de linha.</p>
 */
public final class TelaProdutos {

    private final CasosDeUso casos;
    private final VBox raiz = new VBox(12);
    private final TextField busca = new TextField();
    private final CheckBox mostrarInativos = new CheckBox("Mostrar inativos");
    private final TableView<Produto> tabela = new TableView<>();
    private final Button editar = new Button("Editar (Enter)");
    private final Button ajustar = new Button("Ajustar estoque (F6)");
    private final Button inativarOuReativar = new Button("Inativar (F7)");
    private final Button excluir = new Button("Excluir (Del)");
    private final Label mensagem = new Label();
    private final PauseTransition esperaDigitacao = new PauseTransition(Duration.millis(300));

    /**
     * @param casos casos de uso
     */
    public TelaProdutos(CasosDeUso casos) {
        this.casos = casos;
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
        Label titulo = new Label("Produtos");
        titulo.getStyleClass().add("titulo-secao");
        busca.setPromptText("Buscar por nome ou código (F3)");
        busca.setPrefWidth(320);
        // busca enquanto digita, mas só depois de uma pausa: não vai ao banco a cada letra
        esperaDigitacao.setOnFinished(evento -> recarregar());
        busca.textProperty().addListener((obs, antes, agora) -> esperaDigitacao.playFromStart());
        mostrarInativos.setOnAction(evento -> recarregar());

        Button novo = new Button("Novo produto (F2)");
        novo.getStyleClass().add("botao-primario");
        novo.setOnAction(evento -> novoProduto());
        Region espaco = new Region();
        HBox.setHgrow(espaco, Priority.ALWAYS);
        HBox topo = new HBox(12, titulo, espaco, busca, mostrarInativos, novo);
        topo.setAlignment(Pos.CENTER_LEFT);

        montarTabela();

        editar.getStyleClass().add("botao-secundario");
        editar.setOnAction(evento -> selecionado().ifPresent(this::editarProduto));
        ajustar.getStyleClass().add("botao-secundario");
        ajustar.setOnAction(evento -> selecionado().ifPresent(this::ajustarEstoque));
        inativarOuReativar.getStyleClass().add("botao-secundario");
        inativarOuReativar.setOnAction(evento -> selecionado().ifPresent(this::inativarOuReativar));
        editar.disableProperty().bind(tabela.getSelectionModel().selectedItemProperty().isNull());
        ajustar.disableProperty().bind(tabela.getSelectionModel().selectedItemProperty().isNull());
        inativarOuReativar.disableProperty().bind(tabela.getSelectionModel().selectedItemProperty().isNull());
        excluir.getStyleClass().add("botao-secundario");
        excluir.setOnAction(evento -> selecionado().ifPresent(this::excluirProduto));
        excluir.disableProperty().bind(tabela.getSelectionModel().selectedItemProperty().isNull());
        tabela.getSelectionModel().selectedItemProperty().addListener((obs, antes, produto) ->
                inativarOuReativar.setText(produto != null && !produto.estaAtivo() ? "Reativar (F7)" : "Inativar (F7)"));
        Label legenda = new Label("Vermelho: no estoque mínimo ou abaixo.   Cinza: fora de linha.");
        legenda.getStyleClass().add("dica");
        Region espacoAcoes = new Region();
        HBox.setHgrow(espacoAcoes, Priority.ALWAYS);
        HBox acoes = new HBox(8, editar, ajustar, inativarOuReativar, excluir, espacoAcoes, legenda);
        acoes.setAlignment(Pos.CENTER_LEFT);

        mensagem.setWrapText(true);
        raiz.getChildren().addAll(topo, tabela, acoes, mensagem);
        VBox.setVgrow(tabela, Priority.ALWAYS);
        raiz.setPadding(new Insets(16, 24, 16, 24));
    }

    private void montarTabela() {
        tabela.setPlaceholder(new Label("Nenhum produto. Use \"Novo produto\" para cadastrar o primeiro."));
        tabela.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tabela.getColumns().add(coluna("Código", 100, Produto::getCodigo));
        tabela.getColumns().add(coluna("Cód. barras", 130, p -> p.getCodigoBarras().orElse("")));
        tabela.getColumns().add(coluna("Nome", 260, Produto::getNome));
        tabela.getColumns().add(coluna("Unid.", 60, p -> p.getUnidade().getSigla()));
        tabela.getColumns().add(coluna("Custo", 90, p -> Formatos.moeda(p.getPrecoCusto())));
        tabela.getColumns().add(coluna("Venda", 90, p -> Formatos.moeda(p.getPrecoVenda())));
        tabela.getColumns().add(coluna("Margem", 80, p -> Formatos.numero(p.calcularMargem()) + "%"));
        tabela.getColumns().add(coluna("Estoque", 100,
                p -> Formatos.quantidade(p.getQuantidadeEstoque(), p.getUnidade())));
        tabela.getColumns().add(coluna("Mínimo", 100,
                p -> p.getEstoqueMinimo().map(m -> Formatos.quantidade(m, p.getUnidade())).orElse("—")));

        tabela.setRowFactory(t -> new TableRow<>() {
            @Override
            protected void updateItem(Produto produto, boolean vazio) {
                super.updateItem(produto, vazio);
                getStyleClass().removeAll("linha-repor", "linha-inativa");
                if (!vazio && produto != null) {
                    if (!produto.estaAtivo()) {
                        getStyleClass().add("linha-inativa");
                    } else if (produto.precisaRepor()) {
                        getStyleClass().add("linha-repor");
                    }
                }
            }
        });
        tabela.setOnMouseClicked(evento -> {
            if (evento.getButton() == MouseButton.PRIMARY && evento.getClickCount() == 2) {
                selecionado().ifPresent(this::editarProduto);
            }
        });
        tabela.setOnKeyPressed(evento -> {
            if (evento.getCode() == KeyCode.ENTER) {
                selecionado().ifPresent(this::editarProduto);
            }
        });
    }

    private static TableColumn<Produto, String> coluna(String titulo, double largura, Function<Produto, String> texto) {
        TableColumn<Produto, String> coluna = new TableColumn<>(titulo);
        coluna.setCellValueFactory(dados -> new ReadOnlyStringWrapper(texto.apply(dados.getValue())));
        coluna.setPrefWidth(largura);
        return coluna;
    }

    // ------------------------------------------------------------------ teclado

    private void aoPressionarTecla(KeyEvent evento) {
        boolean digitando = evento.getTarget() instanceof TextInputControl;
        switch (evento.getCode()) {
            case F2 -> novoProduto();
            case F3 -> {
                busca.requestFocus();
                busca.selectAll();
            }
            case F6 -> selecionado().ifPresent(this::ajustarEstoque);
            case F7 -> selecionado().ifPresent(this::inativarOuReativar);
            // Delete no campo de busca apaga a letra, não o produto
            case DELETE -> {
                if (digitando) {
                    return;
                }
                selecionado().ifPresent(this::excluirProduto);
            }
            case UP, DOWN -> {
                if (!digitando) {
                    return; // na tabela, as setas já funcionam sozinhas
                }
                moverSelecao(evento.getCode() == KeyCode.UP ? -1 : 1);
                tabela.requestFocus();
            }
            case ENTER -> {
                if (!digitando) {
                    return; // na tabela, Enter edita (tratado na própria tabela)
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
        boolean inativos = mostrarInativos.isSelected();
        Tarefas.emSegundoPlano(tabela, () -> casos.listarProdutos().executar(termo, inativos),
                this::exibir, erro -> Avisos.tratar(erro, mensagem));
    }

    private void exibir(List<Produto> produtos) {
        Long selecionadoAntes = selecionado().flatMap(Produto::getId).orElse(null);
        tabela.getItems().setAll(produtos);
        if (selecionadoAntes != null) {
            produtos.stream().filter(p -> p.getId().orElse(-1L).equals(selecionadoAntes)).findFirst()
                    .ifPresent(p -> tabela.getSelectionModel().select(p));
        }
    }

    private Optional<Produto> selecionado() {
        return Optional.ofNullable(tabela.getSelectionModel().getSelectedItem());
    }

    private void novoProduto() {
        new DialogoProduto(casos, null).mostrar().ifPresent(produto -> {
            Avisos.sucesso(mensagem, "Produto \"" + produto.getNome() + "\" cadastrado.");
            recarregar();
        });
    }

    private void editarProduto(Produto produto) {
        new DialogoProduto(casos, produto).mostrar().ifPresent(atualizado -> {
            Avisos.sucesso(mensagem, "Produto \"" + atualizado.getNome() + "\" atualizado.");
            recarregar();
        });
    }

    private void ajustarEstoque(Produto produto) {
        new DialogoAjusteEstoque(casos, produto).mostrar().ifPresent(novoEstoque -> {
            Avisos.sucesso(mensagem, "Estoque de \"" + produto.getNome() + "\" agora: "
                    + Formatos.quantidade(novoEstoque, produto.getUnidade()) + ".");
            recarregar();
        });
    }

    private void excluirProduto(Produto produto) {
        if (!Avisos.confirmar("Excluir produto", "Excluir \"" + produto.getNome() + "\"?\n\n"
                + "Se ele já foi vendido, será inativado em vez de excluído, para não apagar o histórico de vendas.")) {
            return;
        }
        long id = produto.getId().orElseThrow();
        Tarefas.emSegundoPlano(tabela, () -> casos.excluirProduto().executar(id), resultado -> {
            Avisos.sucesso(mensagem, resultado == ExcluirProduto.Resultado.EXCLUIDO
                    ? "\"" + produto.getNome() + "\" excluído."
                    : "\"" + produto.getNome() + "\" já tinha vendas: foi inativado em vez de excluído.");
            recarregar();
        }, erro -> Avisos.tratar(erro, mensagem));
    }

    private void inativarOuReativar(Produto produto) {
        long id = produto.getId().orElseThrow();
        if (produto.estaAtivo()) {
            if (!Avisos.confirmar("Inativar produto", "Inativar \"" + produto.getNome() + "\"? Ele sai do caixa, "
                    + "mas continua no histórico de vendas e pode ser reativado depois.")) {
                return;
            }
            Tarefas.emSegundoPlano(tabela, () -> {
                casos.inativarProduto().executar(id);
                return null;
            }, nada -> {
                Avisos.sucesso(mensagem, "\"" + produto.getNome() + "\" inativado.");
                recarregar();
            }, erro -> Avisos.tratar(erro, mensagem));
        } else {
            Tarefas.emSegundoPlano(tabela, () -> {
                casos.reativarProduto().executar(id);
                return null;
            }, nada -> {
                Avisos.sucesso(mensagem, "\"" + produto.getNome() + "\" voltou a ser vendido.");
                recarregar();
            }, erro -> Avisos.tratar(erro, mensagem));
        }
    }
}
