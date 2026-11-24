package br.com.ferragem.apresentacao.tela;

import br.com.ferragem.aplicacao.casodeuso.TrocarSenha;
import br.com.ferragem.apresentacao.Navegador;
import br.com.ferragem.apresentacao.comum.Avisos;
import br.com.ferragem.apresentacao.comum.Formulario;
import br.com.ferragem.apresentacao.comum.Sessao;
import br.com.ferragem.apresentacao.comum.Tarefas;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Region;

/**
 * Troca da própria senha. Quando é obrigatória (primeiro acesso), não há botão de cancelar nem
 * menu: o operador só usa o sistema depois de definir uma senha que só ele conhece.
 *
 * <p>A tela confere apenas se "nova" e "confirmação" são iguais, que é erro de digitação, não
 * regra de negócio. Tamanho mínimo e "diferente da atual" são regras do caso de uso
 * {@link TrocarSenha}: se mudarem, a tela não muda.</p>
 */
public class TelaTrocaSenha {

    private final TrocarSenha trocarSenha;
    private final Sessao sessao;
    private final Navegador navegador;
    private final PasswordField atual = new PasswordField();
    private final PasswordField nova = new PasswordField();
    private final PasswordField confirmacao = new PasswordField();
    private final Label mensagem = new Label();
    private final VBox cartao;

    /**
     * @param trocarSenha caso de uso
     * @param sessao      operador logado
     * @param navegador   para seguir depois da troca
     * @param obrigatoria {@code true} no primeiro acesso
     */
    public TelaTrocaSenha(TrocarSenha trocarSenha, Sessao sessao, Navegador navegador, boolean obrigatoria) {
        this.trocarSenha = trocarSenha;
        this.sessao = sessao;
        this.navegador = navegador;

        Label titulo = new Label(obrigatoria ? "Defina a sua senha" : "Trocar senha");
        titulo.getStyleClass().add("titulo-secao");
        Label explicacao = new Label(obrigatoria
                ? "Esta senha foi criada por outra pessoa. Por segurança, escolha uma que só você conheça."
                : "Informe a senha atual e a nova.");
        explicacao.setWrapText(true);

        Button salvar = new Button("Salvar senha");
        salvar.getStyleClass().add("botao-primario");
        salvar.setDefaultButton(true);
        salvar.setOnAction(evento -> salvar());
        HBox botoes = new HBox(8, salvar);
        if (!obrigatoria) {
            Button cancelar = new Button("Cancelar");
            cancelar.getStyleClass().add("botao-secundario");
            cancelar.setCancelButton(true);
            cancelar.setOnAction(evento -> navegador.irParaCaixa());
            botoes.getChildren().add(cancelar);
        }
        mensagem.setWrapText(true);

        Formulario formulario = new Formulario()
                .campo("Senha _atual", atual)
                .campo("_Nova senha", nova)
                .campo("_Repita a nova", confirmacao);
        cartao = new VBox(16, titulo, explicacao, formulario, botoes, mensagem);
        cartao.getStyleClass().add("cartao");
        cartao.setMaxSize(440, Region.USE_PREF_SIZE);
        Platform.runLater(atual::requestFocus);
    }

    /** @return conteúdo da tela */
    public Parent raiz() {
        StackPane centro = new StackPane(cartao);
        StackPane.setAlignment(cartao, Pos.CENTER);
        return centro;
    }

    private void salvar() {
        Avisos.limpar(mensagem);
        if (!nova.getText().equals(confirmacao.getText())) {
            Avisos.erro(mensagem, "A nova senha e a repetição não são iguais.");
            confirmacao.clear();
            confirmacao.requestFocus();
            return;
        }
        String senhaAtual = atual.getText();
        String senhaNova = nova.getText();
        Tarefas.emSegundoPlano(cartao,
                () -> {
                    trocarSenha.executar(sessao.operador(), senhaAtual, senhaNova);
                    return null;
                },
                nada -> navegador.irParaCaixa(),
                erro -> Avisos.tratar(erro, mensagem));
    }
}
