package br.com.ferragem.apresentacao.tela;

import br.com.ferragem.apresentacao.comum.Formatos;
import br.com.ferragem.apresentacao.comum.Tema;
import br.com.ferragem.dominio.modelo.venda.FormaPagamento;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Escolha da forma de pagamento (F9 no PDV).
 *
 * <p>Teclado: <b>1</b> Dinheiro · <b>2</b> Pix · <b>3</b> Crédito · <b>4</b> Débito ·
 * <b>5</b> Fiado; <b>Enter</b> confirma; <b>Esc</b> volta ao carrinho. Em dinheiro, o foco vai
 * para o valor recebido (vazio = valor exato, sem troco).</p>
 *
 * <p>O troco mostrado aqui é só uma <b>prévia</b> para o operador separar as notas. O valor
 * oficial vem do comprovante, calculado por {@code PagamentoDinheiro} (a regra mora lá).</p>
 */
public final class DialogoPagamento {

    /**
     * O que o operador escolheu.
     *
     * @param forma         forma de pagamento
     * @param valorRecebido dinheiro entregue; nulo nas outras formas ou quando é o valor exato
     */
    public record Escolha(FormaPagamento forma, BigDecimal valorRecebido) {
    }

    private static final Map<KeyCode, FormaPagamento> TECLAS = new LinkedHashMap<>();

    static {
        TECLAS.put(KeyCode.DIGIT1, FormaPagamento.DINHEIRO);
        TECLAS.put(KeyCode.DIGIT2, FormaPagamento.PIX);
        TECLAS.put(KeyCode.DIGIT3, FormaPagamento.CREDITO);
        TECLAS.put(KeyCode.DIGIT4, FormaPagamento.DEBITO);
        TECLAS.put(KeyCode.DIGIT5, FormaPagamento.FIADO);
    }

    private final Dialog<Escolha> dialogo = new Dialog<>();
    private final ToggleGroup formas = new ToggleGroup();
    private final Map<FormaPagamento, ToggleButton> botoes = new LinkedHashMap<>();
    private final TextField recebido = new TextField();
    private final Label previaTroco = new Label();
    private final Label aviso = new Label();
    private final VBox areaDinheiro;
    private final BigDecimal total;
    private final boolean temCliente;

    /**
     * @param total       total da venda
     * @param nomeCliente nome do cliente identificado, ou {@code null}
     */
    public DialogoPagamento(BigDecimal total, String nomeCliente) {
        this.total = total;
        this.temCliente = nomeCliente != null;
        dialogo.setTitle("Pagamento");
        dialogo.setHeaderText("Total: " + Formatos.moeda(total)
                + (temCliente ? "    Cliente: " + nomeCliente : ""));
        Tema.aplicar(dialogo.getDialogPane());

        ButtonType confirmar = new ButtonType("Confirmar (Enter)", ButtonBar.ButtonData.OK_DONE);
        dialogo.getDialogPane().getButtonTypes().addAll(confirmar, ButtonType.CANCEL);
        Node botaoConfirmar = dialogo.getDialogPane().lookupButton(confirmar);
        botaoConfirmar.disableProperty().bind(formas.selectedToggleProperty().isNull());

        HBox linhaFormas = new HBox(8);
        int numero = 1;
        for (FormaPagamento forma : TECLAS.values()) {
            ToggleButton botao = new ToggleButton(numero++ + "  " + forma.getDescricao());
            botao.setToggleGroup(formas);
            botao.setUserData(forma);
            botao.getStyleClass().add("botao-forma");
            botao.setFocusTraversable(false);
            botoes.put(forma, botao);
            linhaFormas.getChildren().add(botao);
        }

        recebido.setPromptText("vazio = valor exato");
        recebido.setMaxWidth(200);
        previaTroco.getStyleClass().add("valor-destaque");
        areaDinheiro = new VBox(6, new Label("Valor recebido (R$)"), recebido, previaTroco);
        areaDinheiro.setVisible(false);
        areaDinheiro.setManaged(false);
        aviso.setWrapText(true);

        recebido.textProperty().addListener((obs, antes, agora) -> atualizarPrevia());
        formas.selectedToggleProperty().addListener((obs, antes, agora) -> aoEscolher());

        dialogo.getDialogPane().addEventFilter(KeyEvent.KEY_PRESSED, evento -> {
            boolean digitandoNoCampo = dialogo.getDialogPane().getScene().getFocusOwner() instanceof TextInputControl;
            FormaPagamento forma = TECLAS.get(normalizar(evento.getCode()));
            if (forma != null && !digitandoNoCampo) {
                botoes.get(forma).setSelected(true);
                evento.consume();
            }
        });
        botaoConfirmar.addEventFilter(ActionEvent.ACTION, evento -> {
            if (!entradaValida()) {
                evento.consume();
            }
        });

        VBox conteudo = new VBox(12, new Label("Escolha a forma (teclas 1 a 5):"), linhaFormas, areaDinheiro, aviso);
        conteudo.setPadding(new Insets(8, 0, 0, 0));
        dialogo.getDialogPane().setContent(conteudo);
        dialogo.setResultConverter(botao -> botao == confirmar ? escolha() : null);
        Platform.runLater(() -> dialogo.getDialogPane().requestFocus());
    }

    /** @return a escolha, ou vazio se o operador voltou ao carrinho (Esc) */
    public Optional<Escolha> mostrar() {
        return dialogo.showAndWait();
    }

    private FormaPagamento selecionada() {
        return formas.getSelectedToggle() == null ? null : (FormaPagamento) formas.getSelectedToggle().getUserData();
    }

    private void aoEscolher() {
        FormaPagamento forma = selecionada();
        boolean dinheiro = forma == FormaPagamento.DINHEIRO;
        areaDinheiro.setVisible(dinheiro);
        areaDinheiro.setManaged(dinheiro);
        aviso.getStyleClass().remove("mensagem-erro");
        aviso.setText("");
        if (forma != null && forma.exigeCliente() && !temCliente) {
            aviso.getStyleClass().add("mensagem-erro");
            aviso.setText("Para vender fiado, volte (Esc) e identifique o cliente com F4.");
        }
        if (dinheiro) {
            Platform.runLater(recebido::requestFocus);
            atualizarPrevia();
        }
        dialogo.getDialogPane().getScene().getWindow().sizeToScene();
    }

    private void atualizarPrevia() {
        if (recebido.getText().isBlank()) {
            previaTroco.setText("Sem troco");
            return;
        }
        Optional<BigDecimal> valor = Formatos.lerNumero(recebido.getText());
        if (valor.isEmpty()) {
            previaTroco.setText("");
        } else if (valor.get().compareTo(total) >= 0) {
            previaTroco.setText("Troco: " + Formatos.moeda(valor.get().subtract(total)));
        } else {
            previaTroco.setText("Faltam " + Formatos.moeda(total.subtract(valor.get())));
        }
    }

    private boolean entradaValida() {
        if (selecionada() == FormaPagamento.DINHEIRO && !recebido.getText().isBlank()
                && Formatos.lerNumero(recebido.getText()).isEmpty()) {
            aviso.getStyleClass().add("mensagem-erro");
            aviso.setText("Valor recebido inválido. Digite, por exemplo, 50,00 ou deixe vazio.");
            recebido.requestFocus();
            return false;
        }
        return true;
    }

    private Escolha escolha() {
        FormaPagamento forma = selecionada();
        BigDecimal valor = forma == FormaPagamento.DINHEIRO
                ? Formatos.lerNumero(recebido.getText()).orElse(null) : null;
        return new Escolha(forma, valor);
    }

    /** Teclado numérico (NUMPAD1…5) vale o mesmo que a fileira de cima. */
    private static KeyCode normalizar(KeyCode tecla) {
        return switch (tecla) {
            case NUMPAD1 -> KeyCode.DIGIT1;
            case NUMPAD2 -> KeyCode.DIGIT2;
            case NUMPAD3 -> KeyCode.DIGIT3;
            case NUMPAD4 -> KeyCode.DIGIT4;
            case NUMPAD5 -> KeyCode.DIGIT5;
            default -> tecla;
        };
    }
}
