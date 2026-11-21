package br.com.ferragem.apresentacao.tela;

import br.com.ferragem.apresentacao.comum.Avisos;
import br.com.ferragem.apresentacao.comum.Tarefas;
import br.com.ferragem.apresentacao.comum.Tema;
import javafx.application.Platform;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Janela de busca reaproveitada pelo PDV: produto (F3) e cliente (F4).
 *
 * <p>Uso só pelo teclado: digite e Enter para buscar; o foco desce para a lista com o primeiro
 * resultado marcado; ↑/↓ escolhem; Enter (ou duplo clique) confirma; ↑ no primeiro item volta
 * para o campo, para refinar a busca; Esc fecha sem escolher. A busca roda em segundo plano
 * porque vai ao banco.</p>
 *
 * @param <T> tipo do que é buscado
 */
public final class DialogoBusca<T> {

    /**
     * Busca que pode lançar exceção (banco fora do ar, por exemplo).
     *
     * @param <T> tipo do resultado
     */
    @FunctionalInterface
    public interface Busca<T> {
        List<T> buscar(String termo) throws Exception;
    }

    private final Dialog<T> dialogo = new Dialog<>();
    private final TextField termo = new TextField();
    private final ListView<T> resultados = new ListView<>();
    private final Label mensagem = new Label();

    /**
     * @param titulo    título da janela
     * @param instrucao frase explicando o que pode ser digitado
     * @param busca     consulta (normalmente um caso de uso)
     * @param descrever como cada resultado aparece na lista
     * @param inicial   texto já digitado ao abrir (pode ser vazio)
     */
    public DialogoBusca(String titulo, String instrucao, Busca<T> busca, Function<T, String> descrever,
                        String inicial) {
        dialogo.setTitle(titulo);
        dialogo.setHeaderText(instrucao);
        Tema.aplicar(dialogo.getDialogPane());
        ButtonType escolher = new ButtonType("Escolher", ButtonBar.ButtonData.OK_DONE);
        dialogo.getDialogPane().getButtonTypes().addAll(escolher, ButtonType.CANCEL);
        dialogo.getDialogPane().lookupButton(escolher).disableProperty()
                .bind(resultados.getSelectionModel().selectedItemProperty().isNull());

        resultados.setPrefSize(560, 300);
        resultados.setPlaceholder(new Label("Digite e aperte Enter para buscar."));
        resultados.setCellFactory(lista -> new ListCell<>() {
            @Override
            protected void updateItem(T item, boolean vazio) {
                super.updateItem(item, vazio);
                setText(vazio || item == null ? null : descrever.apply(item));
            }
        });
        mensagem.setWrapText(true);
        // reserva a linha da mensagem: a janela não cresce sozinha quando o texto aparece
        mensagem.setMinHeight(36);
        termo.setText(inicial);

        // filtro (e não handler comum) para o Enter do campo não chegar ao botão "Escolher"
        termo.addEventFilter(KeyEvent.KEY_PRESSED, evento -> {
            if (evento.getCode() == KeyCode.ENTER) {
                buscar(busca);
                evento.consume();
            } else if (evento.getCode() == KeyCode.DOWN && !resultados.getItems().isEmpty()) {
                focarLista();
                evento.consume();
            }
        });
        resultados.addEventFilter(KeyEvent.KEY_PRESSED, evento -> {
            if (evento.getCode() == KeyCode.UP && resultados.getSelectionModel().getSelectedIndex() <= 0) {
                termo.requestFocus();
                evento.consume();
            }
        });
        resultados.setOnMouseClicked(evento -> {
            if (evento.getButton() == MouseButton.PRIMARY && evento.getClickCount() == 2
                    && resultados.getSelectionModel().getSelectedItem() != null) {
                dialogo.setResult(resultados.getSelectionModel().getSelectedItem());
                dialogo.close();
            }
        });

        dialogo.getDialogPane().setContent(new VBox(8, termo, resultados, mensagem));
        dialogo.setResultConverter(botao -> botao == escolher
                ? resultados.getSelectionModel().getSelectedItem() : null);
        Platform.runLater(() -> {
            termo.requestFocus();
            termo.end();
            if (!inicial.isBlank()) {
                buscar(busca);
            }
        });
    }

    /** @return o item escolhido, ou vazio se o usuário cancelou */
    public Optional<T> mostrar() {
        return dialogo.showAndWait();
    }

    private void buscar(Busca<T> busca) {
        String texto = termo.getText();
        Avisos.limpar(mensagem);
        Tarefas.emSegundoPlano(termo, () -> busca.buscar(texto), encontrados -> {
            resultados.getItems().setAll(encontrados);
            if (encontrados.isEmpty()) {
                Avisos.erro(mensagem, "Nada encontrado para \"" + texto.trim() + "\".");
                termo.requestFocus();
            } else {
                focarLista();
            }
        }, erro -> {
            Avisos.tratar(erro, mensagem);
            termo.requestFocus();
        });
    }

    private void focarLista() {
        resultados.requestFocus();
        resultados.getSelectionModel().selectFirst();
    }
}
