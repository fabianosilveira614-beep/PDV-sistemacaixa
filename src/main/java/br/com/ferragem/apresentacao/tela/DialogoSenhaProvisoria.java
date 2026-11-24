package br.com.ferragem.apresentacao.tela;

import br.com.ferragem.CasosDeUso;
import br.com.ferragem.aplicacao.casodeuso.CadastrarOperador;
import br.com.ferragem.apresentacao.comum.Avisos;
import br.com.ferragem.apresentacao.comum.Formulario;
import br.com.ferragem.apresentacao.comum.Tarefas;
import br.com.ferragem.apresentacao.comum.Tema;
import br.com.ferragem.dominio.modelo.caixa.Operador;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

/**
 * Redefinição de senha pelo administrador (operador esqueceu a sua). A senha aparece em texto
 * aberto de propósito: o administrador precisa conferi-la para entregá-la, e ela só vale até o
 * operador entrar e trocá-la (troca obrigatória).
 */
public final class DialogoSenhaProvisoria {

    private final Dialog<Boolean> dialogo = new Dialog<>();
    private final TextField senha = new TextField();
    private final Label mensagem = new Label();

    /**
     * @param casos casos de uso
     * @param quem  administrador logado
     * @param alvo  quem esqueceu a senha
     */
    public DialogoSenhaProvisoria(CasosDeUso casos, Operador quem, Operador alvo) {
        dialogo.setTitle("Redefinir senha");
        dialogo.setHeaderText("Nova senha provisória para " + alvo.getNome());
        Tema.aplicar(dialogo.getDialogPane());
        ButtonType salvar = new ButtonType("Redefinir", ButtonBar.ButtonData.OK_DONE);
        dialogo.getDialogPane().getButtonTypes().addAll(salvar, ButtonType.CANCEL);
        senha.setPromptText("mínimo " + CadastrarOperador.TAMANHO_MINIMO_SENHA + " caracteres");
        Label dica = new Label("No próximo acesso, " + alvo.getNome() + " entra com esta senha e é obrigado a criar uma nova.");
        dica.getStyleClass().add("dica");
        dica.setWrapText(true);
        dica.setMaxWidth(420);
        mensagem.setWrapText(true);
        mensagem.setMinHeight(36);
        mensagem.setMaxWidth(420);
        dialogo.getDialogPane().setContent(new VBox(8, new Formulario().campo("_Senha provisória", senha), dica, mensagem));

        long id = alvo.getId().orElseThrow();
        dialogo.getDialogPane().lookupButton(salvar).addEventFilter(ActionEvent.ACTION, evento -> {
            evento.consume();
            Avisos.limpar(mensagem);
            String texto = senha.getText();
            Tarefas.emSegundoPlano(dialogo.getDialogPane(), () -> {
                casos.redefinirSenha().executar(quem, id, texto);
                return Boolean.TRUE;
            }, ok -> {
                dialogo.setResult(ok);
                dialogo.close();
            }, erro -> Avisos.tratar(erro, mensagem));
        });
        Platform.runLater(senha::requestFocus);
    }

    /** @return {@code true} se a senha foi redefinida */
    public boolean mostrar() {
        return dialogo.showAndWait().orElse(false);
    }
}
