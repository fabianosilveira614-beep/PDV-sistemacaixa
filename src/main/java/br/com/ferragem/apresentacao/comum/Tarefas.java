package br.com.ferragem.apresentacao.comum;

import javafx.concurrent.Task;
import javafx.scene.Node;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Executa trabalho demorado (banco de dados, hash de senha) fora da thread da interface.
 *
 * <p><b>Por que isso importa:</b> o JavaFX desenha a janela numa única thread. Se uma consulta
 * ao banco rodar nela, a janela congela até o banco responder. Aqui o trabalho roda numa
 * thread separada, e os retornos ({@code aoTerminar}, {@code aoFalhar}) voltam para a thread
 * da interface, onde é permitido mexer na tela.</p>
 *
 * <p>Enquanto o trabalho roda, o componente informado fica desabilitado, o que impede, por
 * exemplo, clicar duas vezes em "Finalizar venda" e registrar a venda em dobro.</p>
 */
public final class Tarefas {

    private Tarefas() {
    }

    /**
     * Trabalho que pode lançar exceções (regra de negócio ou falha técnica).
     *
     * @param <T> tipo do resultado
     */
    @FunctionalInterface
    public interface Trabalho<T> {
        T executar() throws Exception;
    }

    /**
     * @param bloqueado  componente desabilitado enquanto o trabalho roda; pode ser nulo
     * @param trabalho   o que fazer em segundo plano (não pode mexer na tela)
     * @param aoTerminar o que fazer com o resultado, já na thread da interface
     * @param aoFalhar   o que fazer com o erro, já na thread da interface
     * @param <T>        tipo do resultado
     */
    public static <T> void emSegundoPlano(Node bloqueado, Trabalho<T> trabalho, Consumer<T> aoTerminar,
                                          Consumer<Throwable> aoFalhar) {
        Objects.requireNonNull(trabalho);
        Task<T> tarefa = new Task<>() {
            @Override
            protected T call() throws Exception {
                return trabalho.executar();
            }
        };
        if (bloqueado != null) {
            bloqueado.setDisable(true);
        }
        tarefa.setOnSucceeded(evento -> {
            liberar(bloqueado);
            aoTerminar.accept(tarefa.getValue());
        });
        tarefa.setOnFailed(evento -> {
            liberar(bloqueado);
            aoFalhar.accept(tarefa.getException());
        });
        Thread thread = new Thread(tarefa, "tarefa-tela");
        thread.setDaemon(true);
        thread.start();
    }

    private static void liberar(Node bloqueado) {
        if (bloqueado != null) {
            bloqueado.setDisable(false);
        }
    }
}
