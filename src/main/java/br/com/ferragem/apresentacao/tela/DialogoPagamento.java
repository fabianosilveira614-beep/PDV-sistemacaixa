package br.com.ferragem.apresentacao.tela;

import br.com.ferragem.apresentacao.comum.Formatos;
import br.com.ferragem.apresentacao.comum.Formulario;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Escolha da forma de pagamento, usada em dois lugares:
 *
 * <ul>
 *   <li><b>Venda no PDV</b> ({@link #paraVenda}): teclas 1 a 5 (inclui fiado); o valor é o total.</li>
 *   <li><b>Recebimento de fiado</b> ({@link #paraFiado}): teclas 1 a 4 (não se paga fiado com fiado);
 *       aparece o campo "Valor a pagar", editável ou não conforme o botão escolhido.</li>
 * </ul>
 *
 * <p>Teclado: números escolhem a forma; <b>Enter</b> confirma; <b>Esc</b> volta. Com o cursor num
 * campo de texto os números são digitados no campo. O troco mostrado é só uma <b>prévia</b> para o
 * operador separar as notas: o valor oficial vem do comprovante, calculado por
 * {@code PagamentoDinheiro} (a regra mora lá).</p>
 */
public final class DialogoPagamento {

    /**
     * O que o operador escolheu.
     *
     * @param forma         forma de pagamento
     * @param valor         valor a pagar (na venda, o total)
     * @param valorRecebido dinheiro entregue; nulo nas outras formas ou quando é o valor exato
     */
    public record Escolha(FormaPagamento forma, BigDecimal valor, BigDecimal valorRecebido) {
    }

    private static final List<KeyCode> TECLAS = List.of(KeyCode.DIGIT1, KeyCode.DIGIT2, KeyCode.DIGIT3,
            KeyCode.DIGIT4, KeyCode.DIGIT5);

    private final Dialog<Escolha> dialogo = new Dialog<>();
    private final ToggleGroup formas = new ToggleGroup();
    private final Map<KeyCode, ToggleButton> botoesPorTecla = new LinkedHashMap<>();
    private final TextField valor = new TextField();
    private final TextField recebido = new TextField();
    private final Label previaTroco = new Label();
    private final Label aviso = new Label();
    private final VBox areaDinheiro;
    private final BigDecimal valorFixo;
    private final boolean temCliente;

    /**
     * Pagamento de uma venda no PDV.
     *
     * @param total       total da venda
     * @param nomeCliente nome do cliente identificado, ou {@code null}
     * @return o diálogo pronto para {@link #mostrar()}
     */
    public static DialogoPagamento paraVenda(BigDecimal total, String nomeCliente) {
        return new DialogoPagamento("Pagamento",
                "Total: " + Formatos.moeda(total) + (nomeCliente != null ? "    Cliente: " + nomeCliente : ""),
                List.of(FormaPagamento.values()), total, false, false, nomeCliente != null);
    }

    /**
     * Recebimento de compras fiado.
     *
     * @param descricao     o que está sendo pago (ex.: "Pagar selecionadas: 2 compras")
     * @param valorSugerido valor já preenchido
     * @param valorEditavel {@code false} em "Quitar tudo", em que o valor é sempre o total devido
     * @return o diálogo pronto para {@link #mostrar()}
     */
    public static DialogoPagamento paraFiado(String descricao, BigDecimal valorSugerido, boolean valorEditavel) {
        return new DialogoPagamento("Receber fiado", descricao,
                List.of(FormaPagamento.DINHEIRO, FormaPagamento.PIX, FormaPagamento.CREDITO, FormaPagamento.DEBITO),
                valorSugerido, true, valorEditavel, true);
    }

    private DialogoPagamento(String titulo, String cabecalho, List<FormaPagamento> formasAceitas,
                             BigDecimal valorSugerido, boolean mostrarValor, boolean valorEditavel,
                             boolean temCliente) {
        this.temCliente = temCliente;
        this.valorFixo = mostrarValor ? null : valorSugerido;
        dialogo.setTitle(titulo);
        dialogo.setHeaderText(cabecalho);
        Tema.aplicar(dialogo.getDialogPane());

        ButtonType confirmar = new ButtonType("Confirmar (Enter)", ButtonBar.ButtonData.OK_DONE);
        dialogo.getDialogPane().getButtonTypes().addAll(confirmar, ButtonType.CANCEL);
        Node botaoConfirmar = dialogo.getDialogPane().lookupButton(confirmar);
        botaoConfirmar.disableProperty().bind(formas.selectedToggleProperty().isNull());

        HBox linhaFormas = new HBox(8);
        for (int i = 0; i < formasAceitas.size(); i++) {
            FormaPagamento forma = formasAceitas.get(i);
            ToggleButton botao = new ToggleButton((i + 1) + "  " + forma.getDescricao());
            botao.setToggleGroup(formas);
            botao.setUserData(forma);
            botao.getStyleClass().add("botao-forma");
            botao.setFocusTraversable(false);
            botoesPorTecla.put(TECLAS.get(i), botao);
            linhaFormas.getChildren().add(botao);
        }

        valor.setText(Formatos.numero(valorSugerido));
        valor.setMaxWidth(200);
        recebido.setPromptText("vazio = valor exato");
        recebido.setMaxWidth(200);
        previaTroco.getStyleClass().add("valor-destaque");
        areaDinheiro = new VBox(6, new Label("Valor recebido em dinheiro (R$)"), recebido, previaTroco);
        areaDinheiro.setVisible(false);
        areaDinheiro.setManaged(false);
        aviso.setWrapText(true);
        aviso.setMinHeight(36);
        aviso.setMaxWidth(560);

        recebido.textProperty().addListener((obs, antes, agora) -> atualizarPrevia());
        valor.textProperty().addListener((obs, antes, agora) -> atualizarPrevia());
        formas.selectedToggleProperty().addListener((obs, antes, agora) -> aoEscolher());

        dialogo.getDialogPane().addEventFilter(KeyEvent.KEY_PRESSED, evento -> {
            boolean digitandoNoCampo = dialogo.getDialogPane().getScene().getFocusOwner() instanceof TextInputControl;
            ToggleButton botao = botoesPorTecla.get(normalizar(evento.getCode()));
            if (botao != null && !digitandoNoCampo) {
                botao.setSelected(true);
                evento.consume();
            }
        });
        botaoConfirmar.addEventFilter(ActionEvent.ACTION, evento -> {
            if (!entradaValida()) {
                evento.consume();
            }
        });

        VBox conteudo = new VBox(12);
        if (mostrarValor) {
            valor.setEditable(valorEditavel);
            if (!valorEditavel) {
                valor.getStyleClass().add("campo-somente-leitura");
            }
            conteudo.getChildren().add(new Formulario().campo("_Valor a pagar (R$)", valor));
        }
        conteudo.getChildren().addAll(new Label("Escolha a forma (teclas 1 a " + formasAceitas.size() + "):"),
                linhaFormas, areaDinheiro, aviso);
        conteudo.setPadding(new Insets(8, 0, 0, 0));
        dialogo.getDialogPane().setContent(conteudo);
        dialogo.setResultConverter(botao -> botao == confirmar ? escolha() : null);
        Platform.runLater(() -> dialogo.getDialogPane().requestFocus());
    }

    /** @return a escolha, ou vazio se o operador voltou (Esc) */
    public Optional<Escolha> mostrar() {
        return dialogo.showAndWait();
    }

    private FormaPagamento selecionada() {
        return formas.getSelectedToggle() == null ? null : (FormaPagamento) formas.getSelectedToggle().getUserData();
    }

    /** Valor a pagar: o fixo (venda) ou o digitado (fiado). */
    private Optional<BigDecimal> valorAPagar() {
        return valorFixo != null ? Optional.of(valorFixo) : Formatos.lerNumero(valor.getText());
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
        Optional<BigDecimal> aPagar = valorAPagar();
        if (recebido.getText().isBlank() || aPagar.isEmpty()) {
            previaTroco.setText(recebido.getText().isBlank() ? "Sem troco" : "");
            return;
        }
        Optional<BigDecimal> entregue = Formatos.lerNumero(recebido.getText());
        if (entregue.isEmpty()) {
            previaTroco.setText("");
        } else if (entregue.get().compareTo(aPagar.get()) >= 0) {
            previaTroco.setText("Troco: " + Formatos.moeda(entregue.get().subtract(aPagar.get())));
        } else {
            previaTroco.setText("Faltam " + Formatos.moeda(aPagar.get().subtract(entregue.get())));
        }
    }

    private boolean entradaValida() {
        if (valorAPagar().isEmpty()) {
            erro("Valor a pagar inválido. Digite, por exemplo, 50,00.");
            valor.requestFocus();
            return false;
        }
        if (selecionada() == FormaPagamento.DINHEIRO && !recebido.getText().isBlank()
                && Formatos.lerNumero(recebido.getText()).isEmpty()) {
            erro("Valor recebido inválido. Digite, por exemplo, 50,00 ou deixe vazio.");
            recebido.requestFocus();
            return false;
        }
        return true;
    }

    private void erro(String texto) {
        aviso.getStyleClass().remove("mensagem-erro");
        aviso.getStyleClass().add("mensagem-erro");
        aviso.setText(texto);
    }

    private Escolha escolha() {
        FormaPagamento forma = selecionada();
        BigDecimal entregue = forma == FormaPagamento.DINHEIRO
                ? Formatos.lerNumero(recebido.getText()).orElse(null) : null;
        return new Escolha(forma, valorAPagar().orElseThrow(), entregue);
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
