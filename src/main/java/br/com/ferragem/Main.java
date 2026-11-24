package br.com.ferragem;

import br.com.ferragem.apresentacao.AplicacaoJavaFX;
import javafx.application.Application;

/**
 * Ponto de entrada do programa.
 *
 * <p>Fica separado de {@link AplicacaoJavaFX} de propósito: quando a classe com {@code main}
 * estende {@code Application} e o projeto não usa módulos ({@code module-info.java}), a JVM
 * recusa iniciar com o erro "JavaFX runtime components are missing". Um lançador simples
 * evita o problema tanto no IntelliJ quanto no {@code mvn javafx:run}.</p>
 */
public final class Main {
    private Main() {
    }

    /**
     * Inicia a interface gráfica.
     *
     * @param args argumentos de linha de comando (não utilizados)
     */
    public static void main(String[] args) {
        Application.launch(AplicacaoJavaFX.class, args);
    }
}
