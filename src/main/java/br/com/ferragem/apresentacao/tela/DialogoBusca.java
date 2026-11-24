package br.com.ferragem.apresentacao.tela;

import br.com.ferragem.apresentacao.comum.Avisos;
import br.com.ferragem.apresentacao.comum.Tarefas;
import br.com.ferragem.apresentacao.comum.Tema;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
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
import javafx.util.Duration;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Janela de busca reaproveitada pelo PDV: produto (F3) e cliente (F4).
 *
 * <p>Pode ter uma ação extra à esquerda (ex.: "Cadastrar novo (F2)" na busca de cliente do PDV),
 * que cria o item na hora e o devolve como se tivesse sido escolhido na lista.</p>
 *
 * <p>Abre já mostrando tudo (a busca com o campo vazio lista todos os ativos) e filtra enquanto
 * se digita, depois de uma pausa curta, para não ir ao banco a cada letra. Uso só pelo teclado:
 * Enter ou ↓ no campo descem para a lista com o primeiro resultado marcado; ↑/↓ escolhem; Enter
 * (ou duplo clique) confirma; ↑ no primeiro item volta para o campo; Esc fecha sem escolher.
 * A busca roda em segundo plano porque vai ao banco.</p>
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
    private final PauseTransition esperaDigitacao = new PauseTransition(Duration.millis(300));
    private final Busca<T> busca;
    /** Texto da última busca que já voltou do banco; {@code null} enquanto nenhuma voltou. */
    private String textoExibido;
    /** Conta as buscas para descartar a resposta de uma busca antiga que chegue depois da nova. */
    private int numeroDaBusca;

    /**
     * @param titulo    título da janela
     * @param instrucao frase explicando o que pode ser digitado
     * @param busca     consulta (normalmente um caso de uso)
     * @param descrever como cada resultado aparece na lista
     * @param inicial   texto já digitado ao abrir (pode ser vazio)
     */
    public DialogoBusca(String titulo, String instrucao, Busca<T> busca, Function<T, String> descrever,
                        String inicial) {
        this.busca = busca;
        dialogo.setTitle(titulo);
        dialogo.setHeaderText(instrucao);
        Tema.aplicar(dialogo.getDialogPane());
        ButtonType escolher = new ButtonType("Escolher (ENTER)", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelar = new ButtonType("Cancelar (ESC)", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialogo.getDialogPane().getButtonTypes().addAll(escolher, cancelar);
        dialogo.getDialogPane().lookupButton(escolher).disableProperty()
                .bind(resultados.getSelectionModel().selectedItemProperty().isNull());

        resultados.setPrefSize(560, 300);
        resultados.setPlaceholder(new Label("Carregando…"));
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
        // filtra enquanto digita, mas só depois de uma pausa
        esperaDigitacao.setOnFinished(evento -> buscar(false));
        termo.textProperty().addListener((obs, antes, agora) -> esperaDigitacao.playFromStart());

        // filtro (e não handler comum) para o Enter do campo não chegar ao botão "Escolher"
        termo.addEventFilter(KeyEvent.KEY_PRESSED, evento -> {
            if (evento.getCode() == KeyCode.ENTER || evento.getCode() == KeyCode.DOWN) {
                irParaLista();
                evento.consume();
            }
        });
        resultados.addEventFilter(KeyEvent.KEY_PRESSED, evento -> {
            if (evento.getCode() == KeyCode.UP && resultados.getSelectionModel().getSelectedIndex() <= 0) {
                termo.requestFocus();
                evento.consume();
            }
        });
        // Enter e Esc valem na janela toda: a lista consome essas teclas para editar itens
        dialogo.getDialogPane().addEventFilter(KeyEvent.KEY_PRESSED, evento -> {
            if (evento.getCode() == KeyCode.ESCAPE) {
                dialogo.setResult(null);
                dialogo.close();
                evento.consume();
            } else if (evento.getCode() == KeyCode.ENTER && resultados.isFocused()
                    && resultados.getSelectionModel().getSelectedItem() != null) {
                dialogo.setResult(resultados.getSelectionModel().getSelectedItem());
                dialogo.close();
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
            // com texto já digitado (ex.: código não encontrado no PDV), desce para a lista
            buscar(!inicial.isBlank());
        });
    }

    /**
     * Acrescenta um botão à esquerda, com tecla de atalho, que produz o item sem buscar
     * (cadastro rápido). Se a ação devolver algo, a busca fecha com esse item escolhido;
     * se devolver vazio (cadastro cancelado), a busca continua aberta.
     *
     * @param rotulo texto do botão, já com a tecla (ex.: "Cadastrar novo (F2)")
     * @param tecla  tecla que aciona o botão dentro desta janela
     * @param acao   ação que cria o item
     * @return esta mesma busca, para encadear com {@link #mostrar()}
     */
    public DialogoBusca<T> comAcaoExtra(String rotulo, KeyCode tecla, Supplier<Optional<T>> acao) {
        ButtonType tipo = new ButtonType(rotulo, ButtonBar.ButtonData.LEFT);
        dialogo.getDialogPane().getButtonTypes().add(tipo);
        Button botao = (Button) dialogo.getDialogPane().lookupButton(tipo);
        botao.addEventFilter(ActionEvent.ACTION, evento -> {
            evento.consume(); // não fecha a busca por conta própria
            acao.get().ifPresent(item -> {
                dialogo.setResult(item);
                dialogo.close();
            });
        });
        dialogo.getDialogPane().addEventFilter(KeyEvent.KEY_PRESSED, evento -> {
            if (evento.getCode() == tecla) {
                evento.consume();
                botao.fire();
            }
        });
        return this;
    }

    /** @return o item escolhido, ou vazio se o usuário cancelou */
    public Optional<T> mostrar() {
        return dialogo.showAndWait();
    }

    /** Enter ou ↓ no campo: se a lista já mostra o que está digitado, desce; senão busca antes. */
    private void irParaLista() {
        if (termo.getText().equals(textoExibido) && esperaDigitacao.getStatus() != PauseTransition.Status.RUNNING) {
            if (!resultados.getItems().isEmpty()) {
                focarLista();
            }
        } else {
            buscar(true);
        }
    }

    /**
     * @param descerParaLista se, ao chegar o resultado, o foco deve ir para a lista (Enter) ou
     *                        ficar no campo (filtro enquanto digita)
     */
    private void buscar(boolean descerParaLista) {
        esperaDigitacao.stop();
        String texto = termo.getText();
        int estaBusca = ++numeroDaBusca;
        // nada fica desabilitado: desabilitar o campo tiraria o foco de quem está digitando
        Tarefas.emSegundoPlano(null, () -> busca.buscar(texto), encontrados -> {
            if (estaBusca != numeroDaBusca) {
                return; // chegou depois de uma busca mais nova: descarta
            }
            textoExibido = texto;
            resultados.getItems().setAll(encontrados);
            resultados.setPlaceholder(new Label(texto.isBlank() ? "Nada cadastrado ainda." : "Nada encontrado."));
            if (encontrados.isEmpty() && !texto.isBlank()) {
                Avisos.erro(mensagem, "Nada encontrado para \"" + texto.trim() + "\".");
            } else {
                Avisos.limpar(mensagem);
            }
            if (descerParaLista && !encontrados.isEmpty()) {
                focarLista();
            }
        }, erro -> {
            if (estaBusca == numeroDaBusca) {
                Avisos.tratar(erro, mensagem);
                termo.requestFocus();
            }
        });
    }

    private void focarLista() {
        resultados.requestFocus();
        resultados.getSelectionModel().selectFirst();
    }
}
