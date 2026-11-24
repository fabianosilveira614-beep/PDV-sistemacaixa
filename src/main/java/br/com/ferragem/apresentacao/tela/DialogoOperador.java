package br.com.ferragem.apresentacao.tela;

import br.com.ferragem.CasosDeUso;
import br.com.ferragem.aplicacao.casodeuso.AtualizarOperador;
import br.com.ferragem.aplicacao.casodeuso.CadastrarOperador;
import br.com.ferragem.apresentacao.comum.Avisos;
import br.com.ferragem.apresentacao.comum.Formulario;
import br.com.ferragem.apresentacao.comum.Tarefas;
import br.com.ferragem.apresentacao.comum.Tema;
import br.com.ferragem.dominio.modelo.caixa.Operador;
import br.com.ferragem.dominio.modelo.caixa.PerfilOperador;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.util.Optional;

/**
 * Formulário de operador. No cadastro pede nome, login, senha provisória e perfil; na edição,
 * só nome e perfil (login não muda, e senha se redefine por {@link DialogoSenhaProvisoria}).
 * Todas as regras (só administrador, senha mínima, login repetido, não tirar o próprio perfil)
 * são dos casos de uso: se recusarem, a mensagem aparece aqui e a janela continua aberta.
 */
public final class DialogoOperador {

    private final Dialog<Operador> dialogo = new Dialog<>();
    private final TextField nome = new TextField();
    private final TextField login = new TextField();
    private final TextField senha = new TextField();
    private final ComboBox<PerfilOperador> perfil = new ComboBox<>();
    private final Label mensagem = new Label();

    /**
     * @param casos     casos de uso
     * @param quem      administrador logado
     * @param existente operador a editar, ou {@code null} para cadastrar
     */
    public DialogoOperador(CasosDeUso casos, Operador quem, Operador existente) {
        boolean novo = existente == null;
        dialogo.setTitle(novo ? "Novo operador" : "Editar operador");
        dialogo.setHeaderText(novo ? "Cadastrar operador" : existente.getNome() + "  (login: " + existente.getLogin() + ")");
        Tema.aplicar(dialogo.getDialogPane());
        ButtonType salvar = new ButtonType("Salvar", ButtonBar.ButtonData.OK_DONE);
        dialogo.getDialogPane().getButtonTypes().addAll(salvar, ButtonType.CANCEL);

        perfil.getItems().setAll(PerfilOperador.values());
        perfil.setConverter(new StringConverter<>() {
            @Override
            public String toString(PerfilOperador p) {
                return p == null ? "" : DialogoOperador.descrever(p);
            }

            @Override
            public PerfilOperador fromString(String texto) {
                return null;
            }
        });
        perfil.setValue(novo ? PerfilOperador.OPERADOR : existente.getPerfil());
        mensagem.setWrapText(true);
        mensagem.setMinHeight(36);
        mensagem.setMaxWidth(440);

        Formulario formulario = new Formulario().campo("_Nome", nome);
        if (novo) {
            login.setPromptText("usado para entrar; não muda depois");
            senha.setPromptText("mínimo " + CadastrarOperador.TAMANHO_MINIMO_SENHA + " caracteres");
            formulario.campo("_Login", login).campo("_Senha provisória", senha);
        } else {
            nome.setText(existente.getNome());
        }
        formulario.campo("_Perfil", perfil);
        VBox conteudo = new VBox(8, formulario);
        if (novo) {
            Label dica = new Label("Entregue a senha provisória ao operador: ele será obrigado a trocá-la no primeiro acesso.");
            dica.getStyleClass().add("dica");
            dica.setWrapText(true);
            dica.setMaxWidth(440);
            conteudo.getChildren().add(dica);
        }
        conteudo.getChildren().add(mensagem);
        dialogo.getDialogPane().setContent(conteudo);

        dialogo.getDialogPane().lookupButton(salvar).addEventFilter(ActionEvent.ACTION, evento -> {
            evento.consume(); // só fecha quando o caso de uso aceitar
            Avisos.limpar(mensagem);
            Tarefas.emSegundoPlano(dialogo.getDialogPane(), () -> novo
                            ? casos.cadastrarOperador().executar(quem, new CadastrarOperador.Comando(
                                    nome.getText(), login.getText(), senha.getText(), perfil.getValue()))
                            : casos.atualizarOperador().executar(quem, new AtualizarOperador.Comando(
                                    existente.getId().orElseThrow(), nome.getText(), perfil.getValue())),
                    operador -> {
                        dialogo.setResult(operador);
                        dialogo.close();
                    },
                    erro -> Avisos.tratar(erro, mensagem));
        });
        Platform.runLater(nome::requestFocus);
    }

    /** @return o operador gravado, ou vazio se cancelou */
    public Optional<Operador> mostrar() {
        return dialogo.showAndWait();
    }

    /**
     * @param perfil perfil
     * @return nome do perfil para a tela
     */
    static String descrever(PerfilOperador perfil) {
        return perfil == PerfilOperador.ADMINISTRADOR ? "Administrador" : "Operador";
    }
}
