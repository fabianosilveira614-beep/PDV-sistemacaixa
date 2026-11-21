package br.com.ferragem.apresentacao.comum;

import javafx.geometry.HPos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

/**
 * Grade de duas colunas (rótulo | campo) usada por todos os formulários, para que as telas
 * fiquem com o mesmo alinhamento e espaçamento (grade de 8 px do documento de projeto).
 */
public final class Formulario extends GridPane {

    private int linha;

    /** Cria a grade vazia; as linhas entram por {@link #campo}. */
    public Formulario() {
        setHgap(12);
        setVgap(8);
        ColumnConstraints rotulos = new ColumnConstraints();
        rotulos.setHalignment(HPos.RIGHT);
        ColumnConstraints campos = new ColumnConstraints();
        campos.setHgrow(Priority.ALWAYS);
        campos.setFillWidth(true);
        getColumnConstraints().addAll(rotulos, campos);
    }

    /**
     * Acrescenta uma linha. O rótulo fica ligado ao campo ({@code labelFor}), o que permite
     * leitores de tela anunciarem o nome do campo e o atalho Alt+letra do mnemônico.
     *
     * @param rotulo texto do rótulo
     * @param campo  componente de entrada
     * @return este formulário, para encadear chamadas
     */
    public Formulario campo(String rotulo, Node campo) {
        Label label = new Label(rotulo);
        label.setLabelFor(campo);
        label.setMnemonicParsing(true);
        add(label, 0, linha);
        add(campo, 1, linha);
        linha++;
        return this;
    }
}
