package br.com.ferragem.apresentacao.tela;

import br.com.ferragem.CasosDeUso;
import br.com.ferragem.apresentacao.comum.Avisos;
import br.com.ferragem.apresentacao.comum.Formatos;
import br.com.ferragem.apresentacao.comum.Sessao;
import br.com.ferragem.apresentacao.comum.Tarefas;
import br.com.ferragem.apresentacao.comum.Tema;
import br.com.ferragem.dominio.modelo.caixa.ResultadoFechamento;
import br.com.ferragem.dominio.modelo.caixa.SessaoCaixa;
import br.com.ferragem.dominio.modelo.caixa.SituacaoFechamento;
import br.com.ferragem.dominio.modelo.venda.FormaPagamento;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

/**
 * Gaveta do caixa (F1 no PDV): quanto deve haver em dinheiro, o recebido por forma de pagamento
 * e as operações de suprimento, sangria e fechamento.
 *
 * <p>Nenhum cálculo é feito aqui: o saldo vem de {@link SessaoCaixa#calcularSaldoEsperado()} e a
 * conferência do fechamento vem de {@link ResultadoFechamento}. A janela só formata e exibe.</p>
 */
public final class DialogoGaveta {

    private final CasosDeUso casos;
    private final Sessao sessao;
    private final boolean podeFechar;
    private final Dialog<Void> dialogo = new Dialog<>();
    private final VBox conteudo = new VBox(16);
    private final Label mensagem = new Label();
    private boolean alterou;

    /**
     * @param casos      casos de uso
     * @param sessao     operador logado
     * @param podeFechar {@code false} quando há venda em andamento no PDV (fechar o caixa a deixaria órfã)
     */
    public DialogoGaveta(CasosDeUso casos, Sessao sessao, boolean podeFechar) {
        this.casos = casos;
        this.sessao = sessao;
        this.podeFechar = podeFechar;
        dialogo.setTitle("Gaveta do caixa");
        dialogo.setHeaderText(null);
        Tema.aplicar(dialogo.getDialogPane());
        dialogo.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        mensagem.setWrapText(true);
        conteudo.setMinWidth(520);
        // enquanto o banco responde, reserva espaço para a janela não abrir espremida
        conteudo.setMinHeight(320);
        dialogo.getDialogPane().setContent(conteudo);
        // atalhos da gaveta: F1 suprimento, F2 sangria
        dialogo.getDialogPane().addEventFilter(KeyEvent.KEY_PRESSED, evento -> {
            if (evento.getCode() == KeyCode.F1) {
                suprimento();
                evento.consume();
            } else if (evento.getCode() == KeyCode.F2) {
                sangria();
                evento.consume();
            }
        });
        recarregar();
    }

    /**
     * Mostra a gaveta e espera o operador fechar a janela.
     *
     * @return {@code true} se algo mudou (suprimento, sangria ou fechamento), para o PDV se atualizar
     */
    public boolean mostrar() {
        dialogo.showAndWait();
        return alterou;
    }

    private void recarregar() {
        Tarefas.emSegundoPlano(conteudo, casos.consultarCaixaAberto()::executar, this::exibir,
                erro -> Avisos.tratar(erro, mensagem));
    }

    private void exibir(Optional<SessaoCaixa> caixa) {
        if (caixa.isEmpty()) {
            dialogo.close();
            return;
        }
        SessaoCaixa sessaoCaixa = caixa.get();
        Label abertura = new Label("Aberto por " + sessaoCaixa.getOperador().getNome() + " em "
                + Formatos.dataHora(sessaoCaixa.getAbertoEm()) + ", com "
                + Formatos.moeda(sessaoCaixa.getValorAbertura()) + " de troco.");
        abertura.setWrapText(true);
        Label rotuloGaveta = new Label("Dinheiro que deve estar na gaveta");
        rotuloGaveta.getStyleClass().add("rotulo-grupo");
        Label gaveta = new Label(Formatos.moeda(sessaoCaixa.calcularSaldoEsperado()));
        gaveta.getStyleClass().add("valor-destaque");

        Button suprimento = new Button("Suprimento (F1)");
        suprimento.getStyleClass().add("botao-secundario");
        suprimento.setOnAction(evento -> suprimento());
        Button sangria = new Button("Sangria (F2)");
        sangria.getStyleClass().add("botao-secundario");
        sangria.setOnAction(evento -> sangria());
        Button fechar = new Button("Fechar caixa");
        fechar.getStyleClass().add("botao-perigo");
        fechar.setOnAction(evento -> fechar());

        conteudo.getChildren().setAll(abertura, new VBox(4, rotuloGaveta, gaveta),
                recebidoPorForma(sessaoCaixa.calcularRecebidoPorForma()),
                new HBox(8, suprimento, sangria, fechar), mensagem);
        ajustarTamanho();
    }

    /**
     * A janela abre antes de os dados chegarem do banco; sem isto ela fica com o tamanho do
     * conteúdo vazio e corta o saldo e os botões.
     */
    private void ajustarTamanho() {
        if (dialogo.getDialogPane().getScene() != null && dialogo.getDialogPane().getScene().getWindow() != null) {
            dialogo.getDialogPane().getScene().getWindow().sizeToScene();
        }
    }

    private static GridPane recebidoPorForma(Map<FormaPagamento, BigDecimal> totais) {
        GridPane grade = new GridPane();
        grade.setHgap(24);
        grade.setVgap(4);
        Label cabecalho = new Label("Recebido nesta sessão");
        cabecalho.getStyleClass().add("rotulo-grupo");
        grade.add(cabecalho, 0, 0, 2, 1);
        int linha = 1;
        for (FormaPagamento forma : FormaPagamento.values()) {
            if (forma == FormaPagamento.FIADO) {
                continue; // fiado não é dinheiro recebido: vira dívida do cliente
            }
            grade.add(new Label(forma.getDescricao()), 0, linha);
            Label valor = new Label(Formatos.moeda(totais.getOrDefault(forma, BigDecimal.ZERO)));
            valor.getStyleClass().add("numero");
            grade.add(valor, 1, linha);
            linha++;
        }
        return grade;
    }

    private void suprimento() {
        DialogoValor.pedir("Suprimento", "Dinheiro colocado na gaveta (ex.: mais troco).", true)
                .ifPresent(lancamento -> Tarefas.emSegundoPlano(conteudo,
                        () -> {
                            casos.registrarSuprimento().executar(lancamento.valor(), lancamento.motivo());
                            return null;
                        },
                        nada -> concluido("Suprimento de " + Formatos.moeda(lancamento.valor()) + " registrado."),
                        erro -> Avisos.tratar(erro, mensagem)));
    }

    private void sangria() {
        DialogoValor.pedir("Sangria", "Dinheiro retirado da gaveta (ex.: depósito no banco).", true)
                .ifPresent(lancamento -> Tarefas.emSegundoPlano(conteudo,
                        () -> {
                            casos.registrarSangria().executar(lancamento.valor(), lancamento.motivo());
                            return null;
                        },
                        nada -> concluido("Sangria de " + Formatos.moeda(lancamento.valor()) + " registrada."),
                        erro -> Avisos.tratar(erro, mensagem)));
    }

    private void fechar() {
        if (!podeFechar) {
            Avisos.erro(mensagem, "Há uma venda em andamento. Conclua (F9) ou cancele (F8) antes de fechar o caixa.");
            return;
        }
        DialogoValor.pedir("Fechar caixa", "Conte o dinheiro da gaveta e digite o total encontrado.", false)
                .ifPresent(lancamento -> Tarefas.emSegundoPlano(conteudo,
                        () -> casos.fecharCaixa().executar(sessao.operador(), lancamento.valor()),
                        resultado -> {
                            alterou = true;
                            mostrarConferencia(resultado);
                            dialogo.close();
                        },
                        erro -> Avisos.tratar(erro, mensagem)));
    }

    private void concluido(String texto) {
        alterou = true;
        recarregar();
        Avisos.sucesso(mensagem, texto);
        ajustarTamanho();
    }

    private static void mostrarConferencia(ResultadoFechamento resultado) {
        String situacao = switch (resultado.situacao()) {
            case CONFERE -> "O dinheiro confere.";
            case QUEBRA -> "Faltou " + Formatos.moeda(resultado.diferenca().negate()) + " na gaveta.";
            case SOBRA -> "Sobrou " + Formatos.moeda(resultado.diferenca()) + " na gaveta.";
        };
        StringBuilder texto = new StringBuilder()
                .append("Esperado na gaveta: ").append(Formatos.moeda(resultado.esperado())).append('\n')
                .append("Contado: ").append(Formatos.moeda(resultado.contado())).append("\n\n")
                .append("Recebido na sessão:\n");
        for (FormaPagamento forma : FormaPagamento.values()) {
            BigDecimal valor = resultado.recebidoPorForma().get(forma);
            if (valor != null) {
                texto.append("  ").append(forma.getDescricao()).append(": ").append(Formatos.moeda(valor)).append('\n');
            }
        }
        Alert alerta = new Alert(resultado.situacao() == SituacaoFechamento.CONFERE
                ? Alert.AlertType.INFORMATION : Alert.AlertType.WARNING);
        Tema.aplicar(alerta.getDialogPane());
        alerta.setTitle("Fechamento do caixa");
        alerta.setHeaderText(situacao);
        alerta.setContentText(texto.toString());
        alerta.showAndWait();
    }
}
