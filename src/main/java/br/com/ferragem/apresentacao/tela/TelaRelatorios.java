package br.com.ferragem.apresentacao.tela;

import br.com.ferragem.CasosDeUso;
import br.com.ferragem.aplicacao.casodeuso.RelatorioEstoqueBaixo;
import br.com.ferragem.aplicacao.casodeuso.RelatorioFiadoEmAberto;
import br.com.ferragem.aplicacao.casodeuso.ResumoVendas;
import br.com.ferragem.aplicacao.porta.HistoricoVendas.VendaDoPeriodo;
import br.com.ferragem.apresentacao.Navegador;
import br.com.ferragem.apresentacao.comum.Avisos;
import br.com.ferragem.apresentacao.comum.Formatos;
import br.com.ferragem.apresentacao.comum.Tarefas;
import br.com.ferragem.dominio.modelo.venda.FormaPagamento;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.function.Function;

/**
 * Relatórios: vendas do período, fiado em aberto e estoque baixo.
 *
 * <p><b>Teclado:</b> Ctrl+1/2/3 trocam de aba · F5 atualiza · no fiado, Enter abre a conta do
 * cliente.</p>
 *
 * <p>A tela não calcula nada: os números vêm prontos de {@link ResumoVendas},
 * {@link RelatorioFiadoEmAberto} e {@link RelatorioEstoqueBaixo}, que têm testes próprios.</p>
 */
public final class TelaRelatorios {

    private final CasosDeUso casos;
    private final Navegador navegador;
    private final VBox raiz = new VBox(12);
    private final TabPane abas = new TabPane();
    private final Tab abaVendas = new Tab("Vendas do período (Ctrl+1)");
    private final Tab abaFiado = new Tab("Fiado em aberto (Ctrl+2)");
    private final Tab abaEstoque = new Tab("Estoque baixo (Ctrl+3)");
    private final Label mensagem = new Label();

    // vendas do período
    private final SeletorPeriodo periodoVendas;
    private final Label qtdVendas = numero();
    private final Label totalVendido = numero();
    private final Label ticketMedio = numero();
    private final Label entrouNoCaixa = numero();
    private final Label lucroBruto = numero();
    private final Label notaLucro = new Label();
    private final GridPane porForma = new GridPane();
    private final TableView<ResumoVendas.ProdutoVendido> ranking = new TableView<>();
    private final TableView<VendaDoPeriodo> vendas = new TableView<>();

    // fiado em aberto
    private final SeletorPeriodo periodoFiado;
    private final TableView<RelatorioFiadoEmAberto.Linha> fiado = new TableView<>();
    private final Label rodapeFiado = new Label();

    // estoque baixo
    private final TableView<RelatorioEstoqueBaixo.Linha> estoque = new TableView<>();
    private final Label rodapeEstoque = new Label();

    private boolean fiadoCarregado;
    private boolean estoqueCarregado;

    /**
     * @param casos     casos de uso
     * @param navegador para abrir a conta de um cliente a partir do fiado em aberto
     */
    public TelaRelatorios(CasosDeUso casos, Navegador navegador) {
        this.casos = casos;
        this.navegador = navegador;
        periodoVendas = new SeletorPeriodo(LocalDate.now().withDayOfMonth(1), this::carregarVendas);
        periodoFiado = new SeletorPeriodo(LocalDate.now().minusYears(1), this::carregarFiado);
        montar();
        raiz.addEventFilter(KeyEvent.KEY_PRESSED, this::aoPressionarTecla);
        carregarVendas();
    }

    /** @return conteúdo da tela */
    public Parent raiz() {
        return raiz;
    }

    // ------------------------------------------------------------------ montagem

    private void montar() {
        Label titulo = new Label("Relatórios");
        titulo.getStyleClass().add("titulo-secao");
        Label dica = new Label("F5 atualiza");
        dica.getStyleClass().add("dica");
        HBox topo = new HBox(16, titulo, dica);
        topo.setAlignment(Pos.CENTER_LEFT);

        abaVendas.setContent(montarVendas());
        abaFiado.setContent(montarFiado());
        abaEstoque.setContent(montarEstoque());
        abas.getTabs().addAll(abaVendas, abaFiado, abaEstoque);
        abas.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        abas.getSelectionModel().selectedItemProperty().addListener((obs, antes, agora) -> {
            Avisos.limpar(mensagem);
            if (agora == abaFiado && !fiadoCarregado) {
                carregarFiado();
            } else if (agora == abaEstoque && !estoqueCarregado) {
                carregarEstoque();
            }
        });

        mensagem.setWrapText(true);
        mensagem.setMinHeight(24);
        raiz.getChildren().addAll(topo, abas, mensagem);
        VBox.setVgrow(abas, Priority.ALWAYS);
        raiz.setPadding(new Insets(16, 24, 16, 24));
    }

    private VBox montarVendas() {
        HBox cartoes = new HBox(10, cartao("Vendas", qtdVendas), cartao("Total vendido", totalVendido),
                cartao("Valor médio por venda", ticketMedio), cartao("Entrou no caixa", entrouNoCaixa),
                cartao("Lucro bruto", lucroBruto));
        notaLucro.getStyleClass().add("dica");
        notaLucro.setWrapText(true);

        porForma.setHgap(16);
        porForma.setVgap(4);
        ranking.setPlaceholder(new Label("Nenhuma venda no período."));
        ranking.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        ranking.getColumns().add(coluna("Produto", 170, p -> p.produto().getNome()));
        ranking.getColumns().add(coluna("Qtd.", 80, p -> Formatos.quantidade(p.quantidade(), p.produto().getUnidade())));
        ranking.getColumns().add(coluna("Valor", 90, p -> Formatos.moeda(p.valor())));
        Label tituloForma = subtitulo("Por forma de pagamento");
        Label tituloRanking = subtitulo("Mais vendidos (até " + ResumoVendas.TAMANHO_RANKING + ")");
        VBox esquerda = new VBox(8, tituloForma, porForma, tituloRanking, ranking);
        esquerda.setPrefWidth(380);
        esquerda.setMinWidth(320);
        VBox.setVgrow(ranking, Priority.ALWAYS);

        vendas.setPlaceholder(new Label("Nenhuma venda no período."));
        vendas.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        vendas.getColumns().add(coluna("Data", 140, v -> v.venda().getDataHora().map(Formatos::dataHora).orElse("")));
        vendas.getColumns().add(coluna("Nº", 60, v -> v.venda().getId().map(String::valueOf).orElse("")));
        vendas.getColumns().add(coluna("Cliente", 150, v -> v.venda().getCliente().map(c -> c.getNome()).orElse("—")));
        vendas.getColumns().add(coluna("Operador", 110, VendaDoPeriodo::operador));
        vendas.getColumns().add(coluna("Forma", 90,
                v -> v.venda().getFormaPagamento().map(FormaPagamento::getDescricao).orElse("")));
        vendas.getColumns().add(coluna("Total", 90, v -> Formatos.moeda(v.venda().calcularTotal())));
        // duplo clique ou Enter numa venda abre os detalhes
        vendas.setOnMouseClicked(evento -> {
            if (evento.getButton() == MouseButton.PRIMARY && evento.getClickCount() == 2) {
                abrirDetalheDaVenda();
            }
        });
        vendas.setOnKeyPressed(evento -> {
            if (evento.getCode() == KeyCode.ENTER) {
                abrirDetalheDaVenda();
            }
        });
        VBox direita = new VBox(8, subtitulo("Vendas  (duplo clique ou Enter: detalhes)"), vendas);
        VBox.setVgrow(vendas, Priority.ALWAYS);
        HBox.setHgrow(direita, Priority.ALWAYS);

        HBox corpo = new HBox(16, esquerda, direita);
        VBox.setVgrow(corpo, Priority.ALWAYS);
        VBox caixa = new VBox(10, periodoVendas.raiz(), cartoes, notaLucro, corpo);
        caixa.setPadding(new Insets(12, 0, 0, 0));
        return caixa;
    }

    private VBox montarFiado() {
        fiado.setPlaceholder(new Label("Nenhuma compra fiado em aberto feita neste período."));
        fiado.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        fiado.getColumns().add(coluna("Cliente", 220, l -> l.debito().getCliente().getNome()));
        fiado.getColumns().add(coluna("Venda nº", 80, l -> String.valueOf(l.debito().getVendaId())));
        fiado.getColumns().add(coluna("Data da compra", 150, l -> Formatos.dataHora(l.debito().getCriadoEm())));
        fiado.getColumns().add(coluna("Dias em aberto", 110, l -> String.valueOf(l.diasEmAberto())));
        fiado.getColumns().add(coluna("Valor", 100, l -> Formatos.moeda(l.debito().getValorOriginal())));
        fiado.getColumns().add(coluna("Pago", 100, l -> Formatos.moeda(l.debito().getValorPago())));
        fiado.getColumns().add(coluna("Saldo", 100, l -> Formatos.moeda(l.debito().calcularSaldo())));
        fiado.setOnMouseClicked(evento -> {
            if (evento.getButton() == MouseButton.PRIMARY && evento.getClickCount() == 2) {
                abrirContaDoCliente();
            }
        });
        fiado.setOnKeyPressed(evento -> {
            if (evento.getCode() == KeyCode.ENTER) {
                abrirContaDoCliente();
            }
        });
        Label explicacao = new Label("Compras feitas no período que ainda não foram quitadas, da mais antiga "
                + "para a mais nova. Enter abre a conta do cliente para receber.");
        explicacao.getStyleClass().add("dica");
        explicacao.setWrapText(true);
        rodapeFiado.getStyleClass().add("valor-total");
        VBox caixa = new VBox(10, periodoFiado.raiz(), explicacao, fiado, rodapeFiado);
        VBox.setVgrow(fiado, Priority.ALWAYS);
        caixa.setPadding(new Insets(12, 0, 0, 0));
        return caixa;
    }

    private VBox montarEstoque() {
        estoque.setPlaceholder(new Label("Nenhum produto no estoque mínimo ou abaixo."));
        estoque.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        estoque.getColumns().add(coluna("Código", 110, l -> l.produto().getCodigo()));
        estoque.getColumns().add(coluna("Nome", 280, l -> l.produto().getNome()));
        estoque.getColumns().add(coluna("Estoque", 120,
                l -> Formatos.quantidade(l.produto().getQuantidadeEstoque(), l.produto().getUnidade())));
        estoque.getColumns().add(coluna("Mínimo", 120, l -> l.produto().getEstoqueMinimo()
                .map(m -> Formatos.quantidade(m, l.produto().getUnidade())).orElse("")));
        estoque.getColumns().add(coluna("Falta para o mínimo", 160, l -> l.falta().signum() == 0 ? "no mínimo"
                : Formatos.quantidade(l.falta(), l.produto().getUnidade())));
        Label explicacao = new Label("Produtos ativos com estoque mínimo definido que chegaram nele ou abaixo dele. "
                + "O mínimo de cada produto é definido no cadastro de Produtos.");
        explicacao.getStyleClass().add("dica");
        explicacao.setWrapText(true);
        rodapeEstoque.getStyleClass().add("valor-total");
        VBox caixa = new VBox(10, explicacao, estoque, rodapeEstoque);
        VBox.setVgrow(estoque, Priority.ALWAYS);
        caixa.setPadding(new Insets(12, 0, 0, 0));
        return caixa;
    }

    private static Label numero() {
        Label rotulo = new Label("—");
        rotulo.getStyleClass().add("valor-total");
        return rotulo;
    }

    private static Label subtitulo(String texto) {
        Label rotulo = new Label(texto);
        rotulo.getStyleClass().add("subtitulo");
        return rotulo;
    }

    private static Node cartao(String titulo, Label valor) {
        Label rotulo = new Label(titulo);
        rotulo.getStyleClass().add("rotulo-grupo");
        VBox cartao = new VBox(2, rotulo, valor);
        cartao.getStyleClass().add("cartao-numero");
        HBox.setHgrow(cartao, Priority.ALWAYS);
        cartao.setMaxWidth(Double.MAX_VALUE);
        return cartao;
    }

    private static <T> TableColumn<T, String> coluna(String titulo, double largura, Function<T, String> texto) {
        TableColumn<T, String> coluna = new TableColumn<>(titulo);
        coluna.setCellValueFactory(dados -> new ReadOnlyStringWrapper(texto.apply(dados.getValue())));
        coluna.setPrefWidth(largura);
        return coluna;
    }

    // ------------------------------------------------------------------ teclado

    private void aoPressionarTecla(KeyEvent evento) {
        if (evento.isShortcutDown()) {
            Tab destino = switch (evento.getCode()) {
                case DIGIT1, NUMPAD1 -> abaVendas;
                case DIGIT2, NUMPAD2 -> abaFiado;
                case DIGIT3, NUMPAD3 -> abaEstoque;
                default -> null;
            };
            if (destino != null) {
                abas.getSelectionModel().select(destino);
                evento.consume();
            }
            return;
        }
        if (evento.getCode() == KeyCode.F5) {
            Tab atual = abas.getSelectionModel().getSelectedItem();
            if (atual == abaVendas) {
                carregarVendas();
            } else if (atual == abaFiado) {
                carregarFiado();
            } else {
                carregarEstoque();
            }
            evento.consume();
        }
    }

    // ------------------------------------------------------------------ carga

    private void carregarVendas() {
        LocalDate de = periodoVendas.inicio();
        LocalDate ate = periodoVendas.fim();
        if (de == null || ate == null) {
            Avisos.erro(mensagem, "Informe as duas datas do período.");
            return;
        }
        Avisos.limpar(mensagem);
        Tarefas.emSegundoPlano(vendas, () -> casos.relatorioVendas().executar(de, ate), this::exibirVendas,
                erro -> Avisos.tratar(erro, mensagem));
    }

    private void exibirVendas(ResumoVendas resumo) {
        qtdVendas.setText(String.valueOf(resumo.getQuantidadeVendas()));
        totalVendido.setText(Formatos.moeda(resumo.getTotalVendido()));
        ticketMedio.setText(Formatos.moeda(resumo.getTicketMedio()));
        entrouNoCaixa.setText(Formatos.moeda(resumo.getEntrouNoCaixa()));
        lucroBruto.setText(Formatos.moeda(resumo.getLucroBruto())
                + resumo.getMargemBruta().map(m -> "  (" + Formatos.numero(m) + "%)").orElse(""));
        StringBuilder nota = new StringBuilder("\"Entrou no caixa\" = vendas à vista + fiado recebido no período ("
                + Formatos.moeda(resumo.getRecebidoDeFiado()) + "), sem as vendas no fiado ainda não pagas.");
        if (resumo.getVendidoSemCusto().signum() > 0) {
            nota.append(" Lucro calculado sobre ").append(Formatos.moeda(resumo.getVendidoComCusto()))
                    .append(" em vendas; ").append(Formatos.moeda(resumo.getVendidoSemCusto()))
                    .append(" são de vendas antigas, sem custo registrado.");
        }
        notaLucro.setText(nota.toString());

        porForma.getChildren().clear();
        int linha = 0;
        for (FormaPagamento forma : FormaPagamento.values()) {
            String nome = forma == FormaPagamento.FIADO ? "Fiado (vendido)" : forma.getDescricao();
            porForma.addRow(linha++, new Label(nome), new Label(Formatos.moeda(resumo.getTotalPorForma(forma))));
        }
        porForma.addRow(linha, new Label("Fiado recebido"), new Label(Formatos.moeda(resumo.getRecebidoDeFiado())));

        ranking.getItems().setAll(resumo.getMaisVendidos());
        vendas.getItems().setAll(resumo.getVendas());
    }

    private void carregarFiado() {
        LocalDate de = periodoFiado.inicio();
        LocalDate ate = periodoFiado.fim();
        if (de == null || ate == null) {
            Avisos.erro(mensagem, "Informe as duas datas do período.");
            return;
        }
        fiadoCarregado = true;
        Avisos.limpar(mensagem);
        Tarefas.emSegundoPlano(fiado, () -> casos.relatorioFiadoEmAberto().executar(de, ate), resultado -> {
            fiado.getItems().setAll(resultado.linhas());
            int quantas = resultado.linhas().size();
            rodapeFiado.setText(quantas + (quantas == 1 ? " compra em aberto" : " compras em aberto")
                    + "  ·  total devido " + Formatos.moeda(resultado.totalDevido()));
            if (!fiado.getItems().isEmpty()) {
                fiado.getSelectionModel().selectFirst();
            }
            Platform.runLater(fiado::requestFocus);
        }, erro -> Avisos.tratar(erro, mensagem));
    }

    private void carregarEstoque() {
        estoqueCarregado = true;
        Avisos.limpar(mensagem);
        Tarefas.emSegundoPlano(estoque, () -> casos.relatorioEstoqueBaixo().executar(), linhas -> {
            estoque.getItems().setAll(linhas);
            rodapeEstoque.setText(linhas.isEmpty() ? "Nada a repor."
                    : linhas.size() + (linhas.size() == 1 ? " produto a repor" : " produtos a repor"));
        }, erro -> Avisos.tratar(erro, mensagem));
    }

    private void abrirDetalheDaVenda() {
        VendaDoPeriodo selecionada = vendas.getSelectionModel().getSelectedItem();
        if (selecionada != null) {
            new DialogoDetalheVenda(selecionada.venda(), selecionada.operador()).mostrar();
        }
    }

    private void abrirContaDoCliente() {
        RelatorioFiadoEmAberto.Linha linha = fiado.getSelectionModel().getSelectedItem();
        if (linha != null) {
            navegador.abrirPerfilCliente(linha.debito().getCliente(), TelaPerfilCliente.Aba.CONTA_FIADO);
        }
    }
}
