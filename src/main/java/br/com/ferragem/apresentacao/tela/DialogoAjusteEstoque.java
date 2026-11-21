package br.com.ferragem.apresentacao.tela;

import br.com.ferragem.CasosDeUso;
import br.com.ferragem.aplicacao.casodeuso.AjustarEstoque;
import br.com.ferragem.apresentacao.comum.Avisos;
import br.com.ferragem.apresentacao.comum.Formatos;
import br.com.ferragem.apresentacao.comum.Formulario;
import br.com.ferragem.apresentacao.comum.Tarefas;
import br.com.ferragem.apresentacao.comum.Tema;
import br.com.ferragem.dominio.modelo.produto.Produto;
import br.com.ferragem.dominio.modelo.produto.TipoMovimentoEstoque;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Ajuste manual de estoque: entrada de mercadoria, perda ou correção de contagem. Toda mudança
 * vai para a trilha de movimentos; quem decide se é válida é o caso de uso {@code AjustarEstoque}
 * (quantidade conforme a unidade, perda maior que o estoque).
 *
 * <p>Teclado: <b>Alt+E</b> entrada, <b>Alt+P</b> perda, <b>Alt+C</b> correção (atalhos de letra,
 * porque o cursor fica no campo de quantidade, onde números são digitados); Enter confirma; Esc
 * cancela.</p>
 */
public final class DialogoAjusteEstoque {

    private final Dialog<BigDecimal> dialogo = new Dialog<>();
    private final ToggleGroup tipos = new ToggleGroup();
    private final TextField quantidade = new TextField();
    private final TextField motivo = new TextField();
    private final Label mensagem = new Label();

    /**
     * @param casos   casos de uso
     * @param produto produto a ajustar
     */
    public DialogoAjusteEstoque(CasosDeUso casos, Produto produto) {
        dialogo.setTitle("Ajustar estoque");
        dialogo.setHeaderText(produto.getNome() + "   ·   estoque atual: "
                + Formatos.quantidade(produto.getQuantidadeEstoque(), produto.getUnidade()));
        Tema.aplicar(dialogo.getDialogPane());
        ButtonType confirmar = new ButtonType("Confirmar", ButtonBar.ButtonData.OK_DONE);
        dialogo.getDialogPane().getButtonTypes().addAll(confirmar, ButtonType.CANCEL);

        VBox opcoes = new VBox(6,
                opcao("_Entrada de mercadoria (chegou do fornecedor)   Alt+E", TipoMovimentoEstoque.ENTRADA_COMPRA),
                opcao("_Perda ou avaria (quebrou, venceu, sumiu)   Alt+P", TipoMovimentoEstoque.PERDA),
                opcao("_Correção de contagem (+ soma, − tira; ex.: -3)   Alt+C", TipoMovimentoEstoque.AJUSTE));
        tipos.selectToggle(tipos.getToggles().get(0));
        quantidade.setPromptText("em " + produto.getUnidade().getSigla());
        motivo.setPromptText("opcional (ex.: NF 1234 do fornecedor)");
        mensagem.setWrapText(true);
        mensagem.setMinHeight(36);
        mensagem.setMaxWidth(440);

        dialogo.getDialogPane().setContent(new VBox(12, opcoes,
                new Formulario().campo("_Quantidade", quantidade).campo("Moti_vo", motivo), mensagem));

        dialogo.getDialogPane().lookupButton(confirmar).addEventFilter(ActionEvent.ACTION, evento -> {
            evento.consume(); // só fecha quando o caso de uso aceitar
            Avisos.limpar(mensagem);
            Optional<BigDecimal> numero = Formatos.lerNumero(quantidade.getText());
            if (numero.isEmpty()) {
                Avisos.erro(mensagem, "Digite a quantidade, por exemplo 10 ou 2,5.");
                quantidade.requestFocus();
                return;
            }
            AjustarEstoque.Comando comando = new AjustarEstoque.Comando(produto.getId().orElseThrow(),
                    (TipoMovimentoEstoque) tipos.getSelectedToggle().getUserData(), numero.get(), motivo.getText());
            Tarefas.emSegundoPlano(dialogo.getDialogPane(), () -> casos.ajustarEstoque().executar(comando),
                    novoEstoque -> {
                        dialogo.setResult(novoEstoque);
                        dialogo.close();
                    },
                    erro -> Avisos.tratar(erro, mensagem));
        });
        Platform.runLater(quantidade::requestFocus);
    }

    /** @return o novo estoque, ou vazio se o usuário cancelou */
    public Optional<BigDecimal> mostrar() {
        return dialogo.showAndWait();
    }

    private RadioButton opcao(String texto, TipoMovimentoEstoque tipo) {
        RadioButton botao = new RadioButton(texto);
        botao.setMnemonicParsing(true);
        // ao escolher o tipo pelo teclado, o cursor volta para a quantidade
        botao.setOnAction(evento -> quantidade.requestFocus());
        botao.setToggleGroup(tipos);
        botao.setUserData(tipo);
        return botao;
    }
}
