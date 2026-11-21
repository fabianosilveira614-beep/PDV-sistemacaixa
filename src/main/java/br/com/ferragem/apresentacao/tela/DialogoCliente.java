package br.com.ferragem.apresentacao.tela;

import br.com.ferragem.CasosDeUso;
import br.com.ferragem.aplicacao.casodeuso.AtualizarCliente;
import br.com.ferragem.aplicacao.casodeuso.CadastrarCliente;
import br.com.ferragem.apresentacao.comum.Avisos;
import br.com.ferragem.apresentacao.comum.Formulario;
import br.com.ferragem.apresentacao.comum.Tarefas;
import br.com.ferragem.apresentacao.comum.Tema;
import br.com.ferragem.dominio.modelo.cliente.Cliente;
import br.com.ferragem.dominio.modelo.cliente.Cpf;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.util.Optional;

/**
 * Formulário de cliente: cadastro novo ou edição dos dados. Usado na lista de clientes, no perfil
 * e no PDV (F4 → "Cadastrar novo"), sempre com o mesmo comportamento.
 *
 * <p>É um dos dois únicos lugares em que o CPF aparece (o outro é a aba "Dados" do perfil),
 * conforme a decisão do grupo baseada no princípio da necessidade da LGPD. Nome obrigatório, CPF
 * válido e CPF repetido são regras dos casos de uso: se recusarem, a mensagem aparece aqui dentro
 * e a janela continua aberta com o que foi digitado.</p>
 */
public final class DialogoCliente {

    private final Dialog<Cliente> dialogo = new Dialog<>();
    private final TextField nome = new TextField();
    private final TextField cpf = new TextField();
    private final TextField telefone = new TextField();
    private final Label mensagem = new Label();

    /**
     * @param casos     casos de uso
     * @param existente cliente a editar, ou {@code null} para um cadastro novo
     */
    public DialogoCliente(CasosDeUso casos, Cliente existente) {
        boolean novo = existente == null;
        dialogo.setTitle(novo ? "Novo cliente" : "Editar cliente");
        dialogo.setHeaderText(novo ? "Cadastrar cliente" : existente.getNome());
        Tema.aplicar(dialogo.getDialogPane());
        ButtonType salvar = new ButtonType("Salvar", ButtonBar.ButtonData.OK_DONE);
        dialogo.getDialogPane().getButtonTypes().addAll(salvar, ButtonType.CANCEL);

        cpf.setPromptText("opcional (só números ou com pontos)");
        telefone.setPromptText("opcional");
        mensagem.setWrapText(true);
        mensagem.setMinHeight(36);
        mensagem.setMaxWidth(420);
        if (!novo) {
            nome.setText(existente.getNome());
            cpf.setText(existente.getCpf().map(Cpf::formatado).orElse(""));
            telefone.setText(existente.getTelefone().orElse(""));
        }
        dialogo.getDialogPane().setContent(new VBox(8,
                new Formulario().campo("_Nome", nome).campo("_CPF", cpf).campo("_Telefone", telefone), mensagem));

        dialogo.getDialogPane().lookupButton(salvar).addEventFilter(ActionEvent.ACTION, evento -> {
            evento.consume(); // a janela só fecha quando o caso de uso aceitar
            Avisos.limpar(mensagem);
            Tarefas.emSegundoPlano(dialogo.getDialogPane(), () -> novo
                            ? casos.cadastrarCliente().executar(
                                    new CadastrarCliente.Comando(nome.getText(), cpf.getText(), telefone.getText()))
                            : casos.atualizarCliente().executar(new AtualizarCliente.Comando(
                                    existente.getId().orElseThrow(), nome.getText(), cpf.getText(), telefone.getText())),
                    cliente -> {
                        dialogo.setResult(cliente);
                        dialogo.close();
                    },
                    erro -> Avisos.tratar(erro, mensagem));
        });
        Platform.runLater(nome::requestFocus);
    }

    /** @return o cliente gravado, ou vazio se o usuário cancelou */
    public Optional<Cliente> mostrar() {
        return dialogo.showAndWait();
    }
}
