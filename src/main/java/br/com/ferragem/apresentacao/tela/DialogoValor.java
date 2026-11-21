package br.com.ferragem.apresentacao.tela;

import br.com.ferragem.apresentacao.comum.Formatos;
import br.com.ferragem.apresentacao.comum.Formulario;
import br.com.ferragem.apresentacao.comum.Tema;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Janela pequena que pede um número (dinheiro ou quantidade) e, opcionalmente, um motivo. Usada
 * no caixa (suprimento, sangria, fechamento) e no PDV (quantidade, preço). Enter confirma, Esc
 * cancela.
 *
 * <p>Só confere se o texto é um número. Se o valor é aceitável (negativo, maior que a gaveta,
 * mais de 2 casas) quem decide é a regra de negócio, chamada depois.</p>
 */
public final class DialogoValor {

    /**
     * Valor digitado e motivo (vazio quando não foi pedido).
     *
     * @param valor  quantia
     * @param motivo justificativa
     */
    public record Lancamento(BigDecimal valor, String motivo) {
    }

    private DialogoValor() {
    }

    /**
     * @param titulo      título da janela
     * @param instrucao   frase explicando o que digitar
     * @param pedirMotivo se mostra o campo de motivo
     * @return o lançamento, ou vazio se o usuário cancelou
     */
    public static Optional<Lancamento> pedir(String titulo, String instrucao, boolean pedirMotivo) {
        return pedir(titulo, instrucao, "_Valor (R$)", "0,00", pedirMotivo);
    }

    /**
     * Pede só um número, com o rótulo escolhido (ex.: "Quantidade").
     *
     * @param titulo    título da janela
     * @param instrucao frase explicando o que digitar
     * @param rotulo    rótulo do campo
     * @param exemplo   texto de exemplo mostrado no campo vazio
     * @return o número digitado, ou vazio se o usuário cancelou
     */
    public static Optional<BigDecimal> pedirNumero(String titulo, String instrucao, String rotulo, String exemplo) {
        return pedir(titulo, instrucao, rotulo, exemplo, false).map(Lancamento::valor);
    }

    private static Optional<Lancamento> pedir(String titulo, String instrucao, String rotulo, String exemplo,
                                              boolean pedirMotivo) {
        Dialog<Lancamento> dialogo = new Dialog<>();
        dialogo.setTitle(titulo);
        dialogo.setHeaderText(instrucao);
        Tema.aplicar(dialogo.getDialogPane());

        ButtonType confirmar = new ButtonType("Confirmar", ButtonBar.ButtonData.OK_DONE);
        dialogo.getDialogPane().getButtonTypes().addAll(confirmar, ButtonType.CANCEL);

        TextField valor = new TextField();
        valor.setPromptText(exemplo);
        TextField motivo = new TextField();
        Label erro = new Label();
        erro.getStyleClass().add("mensagem-erro");
        erro.setWrapText(true);
        // reserva a linha da mensagem: a janela não cresce sozinha quando o texto aparece
        erro.setMinHeight(36);

        Formulario formulario = new Formulario().campo(rotulo, valor);
        if (pedirMotivo) {
            formulario.campo("_Motivo", motivo);
        }
        dialogo.getDialogPane().setContent(new VBox(8, formulario, erro));

        Node botaoConfirmar = dialogo.getDialogPane().lookupButton(confirmar);
        botaoConfirmar.addEventFilter(ActionEvent.ACTION, evento -> {
            if (Formatos.lerNumero(valor.getText()).isEmpty()) {
                erro.setText("Digite um número, por exemplo " + exemplo + ".");
                valor.requestFocus();
                evento.consume();
            }
        });
        dialogo.setResultConverter(botao -> botao == confirmar
                ? new Lancamento(Formatos.lerNumero(valor.getText()).orElseThrow(), motivo.getText().trim())
                : null);

        Platform.runLater(valor::requestFocus);
        return dialogo.showAndWait();
    }
}
