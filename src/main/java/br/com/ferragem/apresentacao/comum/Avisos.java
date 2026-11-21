package br.com.ferragem.apresentacao.comum;

import br.com.ferragem.dominio.excecao.RegraNegocioException;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;

import java.util.Optional;

/**
 * Mostra mensagens ao usuário seguindo uma regra fixa:
 *
 * <ul>
 *   <li><b>Regra de negócio violada</b> ({@link RegraNegocioException}): a mensagem já foi
 *       escrita para o operador ("Estoque insuficiente...") e aparece na própria tela, em
 *       vermelho, sem janela extra. O operador corrige e continua.</li>
 *   <li><b>Falha técnica</b> (banco fora do ar, bug): janela de erro com a causa, e o erro
 *       completo vai para o log, onde o grupo pode investigar.</li>
 * </ul>
 */
public final class Avisos {

    private static final System.Logger LOG = System.getLogger(Avisos.class.getName());
    private static final String CLASSE_ERRO = "mensagem-erro";
    private static final String CLASSE_SUCESSO = "mensagem-sucesso";

    private Avisos() {
    }

    /**
     * Trata um erro vindo de um caso de uso.
     *
     * @param erro     exceção lançada
     * @param mensagem rótulo da tela onde as regras de negócio são exibidas
     */
    public static void tratar(Throwable erro, Label mensagem) {
        if (erro instanceof RegraNegocioException regra) {
            erro(mensagem, regra.getMessage());
        } else {
            limpar(mensagem);
            falhaTecnica(erro);
        }
    }

    /** Mostra um texto de erro no rótulo da tela. */
    public static void erro(Label mensagem, String texto) {
        mensagem.getStyleClass().removeAll(CLASSE_ERRO, CLASSE_SUCESSO);
        mensagem.getStyleClass().add(CLASSE_ERRO);
        mensagem.setText(texto);
    }

    /** Mostra um texto de confirmação no rótulo da tela. */
    public static void sucesso(Label mensagem, String texto) {
        mensagem.getStyleClass().removeAll(CLASSE_ERRO, CLASSE_SUCESSO);
        mensagem.getStyleClass().add(CLASSE_SUCESSO);
        mensagem.setText(texto);
    }

    /** Apaga a mensagem da tela. */
    public static void limpar(Label mensagem) {
        mensagem.getStyleClass().removeAll(CLASSE_ERRO, CLASSE_SUCESSO);
        mensagem.setText("");
    }

    /**
     * Janela de erro inesperado. O detalhe técnico vai para o log.
     *
     * @param erro exceção não prevista
     */
    public static void falhaTecnica(Throwable erro) {
        LOG.log(System.Logger.Level.ERROR, "Erro inesperado na tela", erro);
        Alert alerta = new Alert(Alert.AlertType.ERROR);
        alerta.setTitle("Erro");
        alerta.setHeaderText("Não foi possível concluir a operação.");
        Tema.aplicar(alerta.getDialogPane());
        alerta.setContentText(causaRaiz(erro)
                + "\n\nSe o problema continuar, verifique se o PostgreSQL está rodando.");
        alerta.showAndWait();
    }

    /**
     * Pede confirmação para uma ação que não pode ser desfeita.
     *
     * @param titulo  título da janela
     * @param pergunta texto da pergunta
     * @return {@code true} se o usuário confirmou
     */
    public static boolean confirmar(String titulo, String pergunta) {
        Alert alerta = new Alert(Alert.AlertType.CONFIRMATION, pergunta, ButtonType.YES, ButtonType.NO);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        Tema.aplicar(alerta.getDialogPane());
        Optional<ButtonType> resposta = alerta.showAndWait();
        return resposta.filter(ButtonType.YES::equals).isPresent();
    }

    /**
     * Desce pela cadeia de causas até a exceção original, que costuma dizer o problema real
     * (por exemplo, "Connection to localhost:5432 refused").
     *
     * @param erro exceção recebida
     * @return mensagem da causa original
     */
    public static String causaRaiz(Throwable erro) {
        Throwable atual = erro;
        while (atual.getCause() != null && atual.getCause() != atual) {
            atual = atual.getCause();
        }
        return atual.getMessage() != null ? atual.getMessage() : atual.getClass().getSimpleName();
    }
}
