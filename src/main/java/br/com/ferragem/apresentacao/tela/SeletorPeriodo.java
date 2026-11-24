package br.com.ferragem.apresentacao.tela;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

import java.time.LocalDate;

/**
 * Campos "De" e "Até" com atalhos (Hoje, 7 dias, Este mês). Avisa quem usa a cada mudança, para a
 * tela refazer a consulta sem precisar de botão. Validar o período (início depois do fim) é do
 * caso de uso, não deste componente.
 */
final class SeletorPeriodo {

    private final HBox raiz = new HBox(8);
    private final DatePicker inicio = new DatePicker();
    private final DatePicker fim = new DatePicker();
    private boolean ajustando;

    /**
     * @param inicioPadrao primeiro dia ao abrir
     * @param aoMudar      chamado quando qualquer data muda
     */
    SeletorPeriodo(LocalDate inicioPadrao, Runnable aoMudar) {
        inicio.setValue(inicioPadrao);
        fim.setValue(LocalDate.now());
        inicio.setPrefWidth(140);
        fim.setPrefWidth(140);
        inicio.valueProperty().addListener((obs, antes, agora) -> avisar(aoMudar));
        fim.valueProperty().addListener((obs, antes, agora) -> avisar(aoMudar));

        Label de = new Label("_De");
        de.setMnemonicParsing(true);
        de.setLabelFor(inicio);
        Label ate = new Label("_Até");
        ate.setMnemonicParsing(true);
        ate.setLabelFor(fim);
        LocalDate hoje = LocalDate.now();
        Button botaoHoje = atalho("Hoje", hoje, hoje, aoMudar);
        Button seteDias = atalho("7 dias", hoje.minusDays(6), hoje, aoMudar);
        Button mes = atalho("Este mês", hoje.withDayOfMonth(1), hoje, aoMudar);
        raiz.getChildren().addAll(de, inicio, ate, fim, botaoHoje, seteDias, mes);
        raiz.setAlignment(Pos.CENTER_LEFT);
    }

    private Button atalho(String texto, LocalDate de, LocalDate ate, Runnable aoMudar) {
        Button botao = new Button(texto);
        botao.getStyleClass().add("botao-secundario");
        botao.setFocusTraversable(false);
        botao.setOnAction(evento -> {
            ajustando = true; // muda as duas datas e consulta uma vez só
            inicio.setValue(de);
            fim.setValue(ate);
            ajustando = false;
            aoMudar.run();
        });
        return botao;
    }

    private void avisar(Runnable aoMudar) {
        if (!ajustando) {
            aoMudar.run();
        }
    }

    HBox raiz() {
        return raiz;
    }

    LocalDate inicio() {
        return inicio.getValue();
    }

    LocalDate fim() {
        return fim.getValue();
    }
}
