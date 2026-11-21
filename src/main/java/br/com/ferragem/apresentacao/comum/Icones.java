package br.com.ferragem.apresentacao.comum;

import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignA;
import org.kordamp.ikonli.materialdesign2.MaterialDesignB;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;
import org.kordamp.ikonli.materialdesign2.MaterialDesignP;
import javafx.scene.Node;

/**
 * Único lugar do sistema que conhece a biblioteca de ícones (Ikonli, pacote Material Design 2).
 *
 * <p>As telas pedem "o ícone de produtos", nunca um ícone específico da biblioteca. <b>Se amanhã
 * trocarmos de biblioteca ou de desenho</b>, muda só esta classe. Cor e tamanho vêm do
 * {@code tema.css} (classe {@code .ikonli-font-icon}), não do código.</p>
 *
 * <p>Os ícones são referenciados pelas constantes do {@code enum} da biblioteca, e não por texto
 * como {@code "mdi2c-cash-register"}: um nome errado vira erro de compilação, não um ícone em
 * branco descoberto durante a apresentação.</p>
 */
public final class Icones {

    private Icones() {
    }

    /** @return ícone da área de caixa (PDV) */
    public static Node caixa() {
        return criar(MaterialDesignC.CASH_REGISTER);
    }

    /** @return ícone da área de produtos */
    public static Node produtos() {
        return criar(MaterialDesignP.PACKAGE_VARIANT_CLOSED);
    }

    /** @return ícone da área de clientes */
    public static Node clientes() {
        return criar(MaterialDesignA.ACCOUNT_GROUP_OUTLINE);
    }

    /** @return ícone da área de relatórios */
    public static Node relatorios() {
        return criar(MaterialDesignC.CHART_BOX_OUTLINE);
    }

    /** @return ícone da área de operadores */
    public static Node operadores() {
        return criar(MaterialDesignB.BADGE_ACCOUNT_HORIZONTAL_OUTLINE);
    }

    /** @return ícone do menu do usuário logado */
    public static Node usuario() {
        return criar(MaterialDesignA.ACCOUNT_CIRCLE_OUTLINE);
    }

    private static Node criar(Ikon icone) {
        return new FontIcon(icone);
    }
}
