package br.com.ferragem.apresentacao.comum;

import javafx.scene.Scene;
import javafx.scene.control.DialogPane;

import java.net.URL;
import java.util.Objects;

/**
 * Aplica a folha de estilos única ({@code /ui/tema.css}). Janelas de diálogo têm cena própria
 * e não herdam o estilo da janela principal, por isso também passam por aqui.
 */
public final class Tema {

    private static final String ARQUIVO = "/ui/tema.css";

    private Tema() {
    }

    /** @param cena cena que recebe o tema */
    public static void aplicar(Scene cena) {
        cena.getStylesheets().add(endereco());
    }

    /** @param painel painel de diálogo que recebe o tema */
    public static void aplicar(DialogPane painel) {
        painel.getStylesheets().add(endereco());
    }

    private static String endereco() {
        URL css = Objects.requireNonNull(Tema.class.getResource(ARQUIVO), "tema.css não encontrado");
        return css.toExternalForm();
    }
}
