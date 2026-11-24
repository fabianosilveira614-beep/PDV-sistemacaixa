package br.com.ferragem.apresentacao.tela;

import br.com.ferragem.apresentacao.comum.Formatos;
import br.com.ferragem.apresentacao.comum.Formulario;
import br.com.ferragem.apresentacao.comum.Tema;
import br.com.ferragem.dominio.modelo.cliente.Cliente;
import br.com.ferragem.dominio.modelo.venda.FormaPagamento;
import br.com.ferragem.dominio.modelo.venda.Venda;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * Detalhes de uma venda já feita: data e hora, cliente, operador (quando conhecido), forma de
 * pagamento, itens com o preço cobrado na época e total. Só mostra: nada aqui altera a venda.
 *
 * <p>Usada no relatório de vendas e no perfil do cliente (conta fiado e compras), sempre por
 * duplo clique ou Enter numa venda. <b>Esc</b> fecha.</p>
 */
final class DialogoDetalheVenda {

    private final Dialog<Void> dialogo = new Dialog<>();

    /**
     * @param venda    venda finalizada, com os itens
     * @param operador quem registrou a venda, ou {@code null} se a tela não sabe (a linha é omitida)
     */
    DialogoDetalheVenda(Venda venda, String operador) {
        String numero = venda.getId().map(String::valueOf).orElse("?");
        dialogo.setTitle("Venda nº " + numero);
        dialogo.setHeaderText("Venda nº " + numero);
        Tema.aplicar(dialogo.getDialogPane());
        dialogo.getDialogPane().getButtonTypes()
                .add(new ButtonType("Fechar (Esc)", ButtonBar.ButtonData.CANCEL_CLOSE));

        Formulario dados = new Formulario()
                .campo("Data e hora", new Label(venda.getDataHora().map(Formatos::dataHora).orElse("—")))
                .campo("Cliente", new Label(venda.getCliente().map(Cliente::getNome).orElse("—")));
        if (operador != null) {
            dados.campo("Operador", new Label(operador));
        }
        dados.campo("Forma de pagamento", new Label(
                venda.getFormaPagamento().map(FormaPagamento::getDescricao).orElse("—")));

        PainelItensVenda itens = new PainelItensVenda();
        itens.mostrar(venda);

        Label total = new Label("Total: " + Formatos.moeda(venda.calcularTotal()));
        total.getStyleClass().add("valor-total");

        VBox conteudo = new VBox(12, dados, itens.raiz(), total);
        conteudo.setPrefWidth(640);
        dialogo.getDialogPane().setContent(conteudo);
    }

    /** Abre a janela e espera o operador fechar. */
    void mostrar() {
        dialogo.showAndWait();
    }
}
