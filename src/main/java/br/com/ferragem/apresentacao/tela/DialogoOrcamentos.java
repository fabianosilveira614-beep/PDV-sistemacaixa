package br.com.ferragem.apresentacao.tela;

import br.com.ferragem.CasosDeUso;
import br.com.ferragem.aplicacao.casodeuso.CarregarOrcamento;
import br.com.ferragem.apresentacao.comum.Avisos;
import br.com.ferragem.apresentacao.comum.Formatos;
import br.com.ferragem.apresentacao.comum.Tarefas;
import br.com.ferragem.apresentacao.comum.Tema;
import br.com.ferragem.dominio.modelo.cliente.Cliente;
import br.com.ferragem.dominio.modelo.orcamento.Orcamento;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Orçamentos guardados (F7 com o carrinho vazio): carregar no carrinho, reimprimir ou excluir.
 *
 * <p><b>Teclado:</b> ↑/↓ escolhem · Enter carrega no carrinho · F6 reimprime · Delete exclui ·
 * Esc fecha.</p>
 */
final class DialogoOrcamentos {

    private final CasosDeUso casos;
    private final Dialog<CarregarOrcamento.Resultado> dialogo = new Dialog<>();
    private final TableView<Orcamento> lista = new TableView<>();
    private final TableView<Orcamento.Item> itens = new TableView<>();
    private final Label mensagem = new Label();

    /**
     * @param casos casos de uso
     */
    DialogoOrcamentos(CasosDeUso casos) {
        this.casos = casos;
        dialogo.setTitle("Orçamentos");
        dialogo.setHeaderText("Orçamentos guardados. Carregar no carrinho usa os preços de hoje.");
        Tema.aplicar(dialogo.getDialogPane());
        ButtonType carregar = new ButtonType("Carregar no carrinho (Enter)", ButtonBar.ButtonData.OK_DONE);
        ButtonType reimprimir = new ButtonType("Reimprimir (F6)", ButtonBar.ButtonData.LEFT);
        ButtonType excluir = new ButtonType("Excluir (Del)", ButtonBar.ButtonData.LEFT);
        ButtonType fechar = new ButtonType("Fechar (Esc)", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialogo.getDialogPane().getButtonTypes().addAll(reimprimir, excluir, carregar, fechar);
        Button botaoCarregar = (Button) dialogo.getDialogPane().lookupButton(carregar);
        Button botaoReimprimir = (Button) dialogo.getDialogPane().lookupButton(reimprimir);
        Button botaoExcluir = (Button) dialogo.getDialogPane().lookupButton(excluir);
        for (Button botao : new Button[]{botaoCarregar, botaoReimprimir, botaoExcluir}) {
            botao.disableProperty().bind(lista.getSelectionModel().selectedItemProperty().isNull());
        }

        lista.setPlaceholder(new Label("Nenhum orçamento guardado. Com itens no carrinho, F7 salva um."));
        lista.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        lista.getColumns().add(coluna("Nº", 60, o -> o.getId().map(String::valueOf).orElse("")));
        lista.getColumns().add(coluna("Data", 140, o -> Formatos.dataHora(o.getCriadoEm())));
        lista.getColumns().add(coluna("Cliente", 170, o -> o.getCliente().map(Cliente::getNome).orElse("—")));
        lista.getColumns().add(coluna("Atendente", 110, Orcamento::getAtendente));
        lista.getColumns().add(coluna("Itens", 60, o -> String.valueOf(o.getItens().size())));
        lista.getColumns().add(coluna("Total no dia", 110, o -> Formatos.moeda(o.calcularTotal())));
        lista.setPrefSize(720, 260);
        lista.getSelectionModel().selectedItemProperty().addListener((obs, antes, o) ->
                itens.getItems().setAll(o == null ? List.of() : o.getItens()));

        itens.setPlaceholder(new Label("Selecione um orçamento."));
        itens.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        itens.getColumns().add(coluna("Produto", 300, i -> i.produto().getNome()));
        itens.getColumns().add(coluna("Qtd.", 110, i -> Formatos.quantidade(i.quantidade(), i.produto().getUnidade())));
        itens.getColumns().add(coluna("Preço no dia", 110, i -> Formatos.moeda(i.precoUnitario())));
        itens.getColumns().add(coluna("Subtotal", 110, i -> Formatos.moeda(i.calcularSubtotal())));
        itens.setPrefHeight(170);
        itens.setFocusTraversable(false);

        mensagem.setWrapText(true);
        mensagem.setMinHeight(36);
        mensagem.setMaxWidth(720);
        dialogo.getDialogPane().setContent(new VBox(8, lista, itens, mensagem));

        botaoCarregar.addEventFilter(ActionEvent.ACTION, evento -> {
            evento.consume(); // fecha só quando o carrinho estiver montado
            selecionado().ifPresent(this::carregar);
        });
        botaoReimprimir.addEventFilter(ActionEvent.ACTION, evento -> {
            evento.consume();
            selecionado().ifPresent(this::reimprimir);
        });
        botaoExcluir.addEventFilter(ActionEvent.ACTION, evento -> {
            evento.consume();
            selecionado().ifPresent(this::excluir);
        });
        dialogo.getDialogPane().addEventFilter(KeyEvent.KEY_PRESSED, evento -> {
            if (evento.getCode() == KeyCode.F6) {
                botaoReimprimir.fire();
                evento.consume();
            } else if (evento.getCode() == KeyCode.DELETE) {
                botaoExcluir.fire();
                evento.consume();
            }
        });
        recarregar();
    }

    /** @return o carrinho montado a partir do orçamento escolhido, ou vazio se fechou */
    Optional<CarregarOrcamento.Resultado> mostrar() {
        return dialogo.showAndWait();
    }

    private Optional<Orcamento> selecionado() {
        return Optional.ofNullable(lista.getSelectionModel().getSelectedItem());
    }

    private void recarregar() {
        Tarefas.emSegundoPlano(lista, casos.listarOrcamentos()::executar, orcamentos -> {
            lista.getItems().setAll(orcamentos);
            if (!orcamentos.isEmpty()) {
                lista.getSelectionModel().selectFirst();
            }
            Platform.runLater(lista::requestFocus);
        }, erro -> Avisos.tratar(erro, mensagem));
    }

    private void carregar(Orcamento orcamento) {
        long id = orcamento.getId().orElseThrow();
        Avisos.limpar(mensagem);
        Tarefas.emSegundoPlano(dialogo.getDialogPane(), () -> casos.carregarOrcamento().executar(id), resultado -> {
            if (resultado.carrinho().estaVazia()) {
                Avisos.erro(mensagem, "Nenhum item deste orçamento pode ser vendido agora: " + descreverAvisos(resultado));
                return;
            }
            dialogo.setResult(resultado);
            dialogo.close();
        }, erro -> Avisos.tratar(erro, mensagem));
    }

    private void reimprimir(Orcamento orcamento) {
        ImpressaoOrcamento.Resultado resultado = ImpressaoOrcamento.imprimir(orcamento,
                dialogo.getDialogPane().getScene().getWindow());
        if (resultado == ImpressaoOrcamento.Resultado.IMPRESSO) {
            Avisos.sucesso(mensagem, "Orçamento nº " + orcamento.getId().orElseThrow() + " enviado para a impressora.");
        } else {
            Avisos.erro(mensagem, ImpressaoOrcamento.descreverProblema(resultado));
        }
    }

    private void excluir(Orcamento orcamento) {
        long id = orcamento.getId().orElseThrow();
        if (!Avisos.confirmar("Excluir orçamento", "Excluir o orçamento nº " + id + "? Isso não afeta estoque nem caixa.")) {
            return;
        }
        Tarefas.emSegundoPlano(lista, () -> {
            casos.excluirOrcamento().executar(id);
            return null;
        }, nada -> {
            Avisos.sucesso(mensagem, "Orçamento nº " + id + " excluído.");
            recarregar();
        }, erro -> Avisos.tratar(erro, mensagem));
    }

    /**
     * @param resultado orçamento carregado
     * @return os avisos em texto, um por linha
     */
    static String descreverAvisos(CarregarOrcamento.Resultado resultado) {
        StringBuilder texto = new StringBuilder();
        for (CarregarOrcamento.Aviso aviso : resultado.avisos()) {
            texto.append("\n• ").append(switch (aviso.tipo()) {
                case PRECO_MUDOU -> aviso.nome() + ": era " + Formatos.moeda(aviso.antes()) + ", hoje é "
                        + Formatos.moeda(aviso.agora()) + " (o carrinho usa o preço de hoje).";
                case SAIU_DE_LINHA -> aviso.nome() + " saiu de linha e ficou de fora.";
                case SEM_ESTOQUE -> aviso.nome() + ": o orçamento tem " + Formatos.numero(aviso.antes())
                        + ", mas há só " + Formatos.numero(aviso.agora()) + " em estoque. Ficou de fora.";
                case CLIENTE_EXCLUIDO -> "O cliente " + aviso.nome() + " foi excluído do cadastro: o carrinho ficou sem cliente.";
            });
        }
        return texto.toString();
    }

    private static <T> TableColumn<T, String> coluna(String titulo, double largura, Function<T, String> texto) {
        TableColumn<T, String> coluna = new TableColumn<>(titulo);
        coluna.setCellValueFactory(dados -> new ReadOnlyStringWrapper(texto.apply(dados.getValue())));
        coluna.setPrefWidth(largura);
        return coluna;
    }
}
