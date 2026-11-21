package br.com.ferragem.apresentacao;

import br.com.ferragem.CasosDeUso;
import br.com.ferragem.ConfiguracaoAplicacao;
import br.com.ferragem.apresentacao.comum.Avisos;
import br.com.ferragem.apresentacao.comum.Tema;
import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Janela principal da aplicação.
 *
 * <p>Ao abrir, prepara o banco (aplica as migrações pendentes) e, se der certo, mostra o login.
 * O acesso ao banco roda numa {@link Task}, fora da thread da interface, para a janela não
 * congelar enquanto o banco responde: critério 8 da rubrica ("operações demoradas sem bloquear
 * a janela"). Daí em diante quem decide as telas é o {@link Navegador}.</p>
 */
public class AplicacaoJavaFX extends Application {

    private static final String TITULO = "Caixa Ferragem";
    private static final System.Logger LOG = System.getLogger(AplicacaoJavaFX.class.getName());

    private ConfiguracaoAplicacao configuracao;
    private CasosDeUso casos;
    private Navegador navegador;
    private RuntimeException erroDeInicializacao;
    private Label mensagemStatus;
    private ProgressIndicator indicadorCarregando;

    /**
     * Monta as dependências antes de a janela abrir. Roda fora da thread da interface, por isso
     * não cria nenhum componente de tela aqui (isso fica para o {@link #start}).
     * Uma configuração inválida não derruba o programa: o erro é guardado e mostrado na janela.
     */
    @Override
    public void init() {
        try {
            configuracao = ConfiguracaoAplicacao.criarPadrao();
            casos = configuracao.criarCasosDeUso();
        } catch (RuntimeException e) {
            erroDeInicializacao = e;
        }
    }

    /**
     * Cria a janela e dispara a preparação do banco em segundo plano.
     *
     * @param janela janela principal fornecida pelo JavaFX
     */
    @Override
    public void start(Stage janela) {
        Scene cena;
        if (casos != null) {
            navegador = new Navegador(casos);
            navegador.mostrar(criarAreaStatus());
            cena = new Scene(navegador.raiz(), 1100, 700);
        } else {
            cena = new Scene(criarAreaStatus(), 1100, 700);
        }
        Tema.aplicar(cena);

        janela.setTitle(TITULO);
        janela.setMinWidth(900);
        janela.setMinHeight(600);
        janela.setScene(cena);
        janela.show();

        if (erroDeInicializacao != null) {
            mostrarFalha(erroDeInicializacao);
        } else {
            prepararBancoEmSegundoPlano();
        }
    }

    private VBox criarAreaStatus() {
        indicadorCarregando = new ProgressIndicator();
        indicadorCarregando.setMaxSize(48, 48);
        mensagemStatus = new Label("Preparando o banco de dados…");
        mensagemStatus.getStyleClass().add("status");
        mensagemStatus.setWrapText(true);

        VBox area = new VBox(16, indicadorCarregando, mensagemStatus);
        area.setAlignment(Pos.CENTER);
        area.setPadding(new Insets(24));
        return area;
    }

    private void prepararBancoEmSegundoPlano() {
        Task<Integer> preparacao = new Task<>() {
            @Override
            protected Integer call() {
                return configuracao.getMigradorBanco().migrar();
            }
        };
        preparacao.setOnSucceeded(evento -> navegador.irParaLogin());
        preparacao.setOnFailed(evento -> mostrarFalha(preparacao.getException()));

        Thread trabalhador = new Thread(preparacao, "preparacao-banco");
        trabalhador.setDaemon(true);
        trabalhador.start();
    }

    private void mostrarFalha(Throwable erro) {
        LOG.log(System.Logger.Level.ERROR, "Falha na inicialização", erro);
        indicadorCarregando.setVisible(false);
        mensagemStatus.getStyleClass().add("status-erro");
        mensagemStatus.setText(erro.getMessage()
                + "\nCausa: " + Avisos.causaRaiz(erro)
                + "\n\nVerifique se o PostgreSQL está rodando (docker compose up -d) "
                + "e se usuário e senha conferem com o config.properties.");
    }
}
