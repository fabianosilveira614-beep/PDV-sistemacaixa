package br.com.ferragem.apresentacao.tela;

import br.com.ferragem.apresentacao.comum.Formatos;
import br.com.ferragem.dominio.modelo.venda.ItemVenda;
import br.com.ferragem.dominio.modelo.venda.Venda;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.function.Function;

/**
 * Itens de uma venda (produto, quantidade, preço, subtotal), mostrados abaixo da lista de compras
 * no perfil do cliente, tanto na conta de fiado quanto no histórico. Só exibe: os valores vêm
 * prontos da {@link Venda} e do {@link ItemVenda}.
 */
final class PainelItensVenda {

    private final VBox raiz = new VBox(6);
    private final Label titulo = new Label();
    private final TableView<ItemVenda> tabela = new TableView<>();

    PainelItensVenda() {
        titulo.getStyleClass().add("subtitulo");
        tabela.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tabela.setFocusTraversable(false);
        tabela.setPrefHeight(170);
        tabela.getColumns().add(coluna("Produto", 260, i -> i.getProduto().getNome()));
        tabela.getColumns().add(coluna("Qtd.", 100,
                i -> Formatos.quantidade(i.getQuantidade(), i.getProduto().getUnidade())));
        tabela.getColumns().add(coluna("Preço", 100, i -> Formatos.moeda(i.getPrecoUnitario())));
        tabela.getColumns().add(coluna("Subtotal", 100, i -> Formatos.moeda(i.calcularSubtotal())));
        raiz.getChildren().addAll(titulo, tabela);
        VBox.setVgrow(tabela, Priority.ALWAYS);
        limpar("Selecione uma compra para ver os itens.");
    }

    VBox raiz() {
        return raiz;
    }

    void mostrar(Venda venda) {
        titulo.setText("Itens da venda nº " + venda.getId().map(String::valueOf).orElse("?")
                + venda.getDataHora().map(d -> " — " + Formatos.dataHora(d)).orElse("")
                + "  ·  total " + Formatos.moeda(venda.calcularTotal()));
        tabela.getItems().setAll(venda.getItens());
    }

    void limpar(String aviso) {
        titulo.setText("Itens da compra");
        tabela.getItems().clear();
        tabela.setPlaceholder(new Label(aviso));
    }

    private static TableColumn<ItemVenda, String> coluna(String nome, double largura, Function<ItemVenda, String> texto) {
        TableColumn<ItemVenda, String> coluna = new TableColumn<>(nome);
        coluna.setCellValueFactory(dados -> new ReadOnlyStringWrapper(texto.apply(dados.getValue())));
        coluna.setPrefWidth(largura);
        coluna.setSortable(false);
        return coluna;
    }
}
