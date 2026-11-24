package br.com.ferragem.apresentacao.tela;

import br.com.ferragem.aplicacao.casodeuso.AutenticarOperador;
import br.com.ferragem.apresentacao.Navegador;
import br.com.ferragem.apresentacao.comum.Avisos;
import br.com.ferragem.apresentacao.comum.Formulario;
import br.com.ferragem.apresentacao.comum.Tarefas;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Region;

/**
 * Tela de entrada. Enter no campo de senha entra (botão padrão).
 *
 * <p>A conferência da senha (PBKDF2, ~0,2 s de propósito) roda em segundo plano para a janela
 * não travar. A mensagem de erro é a mesma para login inexistente e senha errada: quem decide
 * isso é o caso de uso, a tela só exibe.</p>
 */
public class TelaLogin {

    private final AutenticarOperador autenticar;
    private final Navegador navegador;
    private final TextField login = new TextField();
    private final PasswordField senha = new PasswordField();
    private final Label mensagem = new Label();
    private final VBox cartao;

    /**
     * @param autenticar caso de uso de login
     * @param navegador  para seguir para a próxima tela
     */
    public TelaLogin(AutenticarOperador autenticar, Navegador navegador) {
        this.autenticar = autenticar;
        this.navegador = navegador;

        Label titulo = new Label("Entrar no caixa");
        titulo.getStyleClass().add("titulo-secao");
        login.setPromptText("seu login");
        Button entrar = new Button("Entrar");
        entrar.getStyleClass().add("botao-primario");
        entrar.setDefaultButton(true);
        entrar.setMaxWidth(Double.MAX_VALUE);
        entrar.setOnAction(evento -> entrar());
        mensagem.setWrapText(true);

        Formulario formulario = new Formulario()
                .campo("_Login", login)
                .campo("_Senha", senha);
        cartao = new VBox(16, titulo, formulario, entrar, mensagem);
        cartao.getStyleClass().add("cartao");
        cartao.setMaxSize(380, Region.USE_PREF_SIZE);
        Platform.runLater(login::requestFocus);
    }

    /** @return conteúdo da tela */
    public Parent raiz() {
        StackPane centro = new StackPane(cartao);
        StackPane.setAlignment(cartao, Pos.CENTER);
        return centro;
    }

    private void entrar() {
        Avisos.limpar(mensagem);
        String loginDigitado = login.getText();
        String senhaDigitada = senha.getText();
        Tarefas.emSegundoPlano(cartao,
                () -> autenticar.executar(loginDigitado, senhaDigitada),
                navegador::aoEntrar,
                erro -> {
                    Avisos.tratar(erro, mensagem);
                    senha.clear();
                    senha.requestFocus();
                });
    }
}
