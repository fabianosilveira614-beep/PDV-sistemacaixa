package br.com.ferragem.apresentacao.tela;

import br.com.ferragem.CasosDeUso;
import br.com.ferragem.aplicacao.casodeuso.PagarDebito;
import br.com.ferragem.apresentacao.Navegador;
import br.com.ferragem.apresentacao.comum.Avisos;
import br.com.ferragem.apresentacao.comum.Formatos;
import br.com.ferragem.apresentacao.comum.Formulario;
import br.com.ferragem.apresentacao.comum.Tarefas;
import br.com.ferragem.apresentacao.comum.Tema;
import br.com.ferragem.dominio.modelo.cliente.Cliente;
import br.com.ferragem.dominio.modelo.cliente.Cpf;
import br.com.ferragem.dominio.modelo.fiado.ContaFiado;
import br.com.ferragem.dominio.modelo.fiado.Debito;
import br.com.ferragem.dominio.modelo.fiado.PagamentoAplicado;
import br.com.ferragem.dominio.modelo.fiado.StatusDebito;
import br.com.ferragem.dominio.modelo.venda.FormaPagamento;
import br.com.ferragem.dominio.modelo.venda.Venda;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Perfil de um cliente, em três abas: dados cadastrais, conta de fiado e compras.
 *
 * <p><b>Teclado:</b> Ctrl+1/2/3 trocam de aba · Esc volta para a lista · F2 edita os dados ·
 * na conta de fiado: Espaço marca/desmarca a compra, F9 paga as marcadas, F10 paga um valor livre,
 * F11 quita tudo.</p>
 *
 * <p><b>Quem decide o quê:</b> esta tela só monta o pedido. Quanto o cliente deve, em que ordem o
 * dinheiro abate as compras (da mais antiga para a mais nova) e se o valor cabe no devido são
 * regras da {@link ContaFiado}; exigir caixa aberto e registrar o dinheiro na gaveta é do caso de
 * uso {@link PagarDebito}. Por isso a tela não faz contas de saldo: só soma o que mostra na linha
 * "Selecionadas", para sugerir o valor.</p>
 */
public final class TelaPerfilCliente {

    /** Abas do perfil, para a lista abrir direto na certa (F9 na lista abre a conta de fiado). */
    public enum Aba {
        /** Nome, CPF e telefone. */
        DADOS,
        /** Compras fiado e recebimento. */
        CONTA_FIADO,
        /** Histórico de compras por período. */
        COMPRAS
    }

    private final CasosDeUso casos;
    private final Navegador navegador;
    private Cliente cliente;

    private final VBox raiz = new VBox(12);
    private final Label nomeNoTopo = new Label();
    private final Label devidoNoTopo = new Label();
    private final Label mensagem = new Label();
    private final TabPane abas = new TabPane();
    private final Tab abaDados = new Tab("Dados (Ctrl+1)");
    private final Tab abaConta = new Tab("Conta fiado (Ctrl+2)");
    private final Tab abaCompras = new Tab("Compras (Ctrl+3)");

    // aba Dados
    private final Label nome = new Label();
    private final Label cpf = new Label();
    private final Label telefone = new Label();

    // aba Conta fiado
    private final TableView<LinhaDebito> debitos = new TableView<>();
    private final CheckBox mostrarQuitadas = new CheckBox("Mostrar compras quitadas");
    private final Label totalDevido = new Label();
    private final Label resumoSelecao = new Label();
    private ContaFiado conta;

    // aba Compras
    private final DatePicker inicio = new DatePicker(LocalDate.now().minusDays(30));
    private final DatePicker fim = new DatePicker(LocalDate.now());
    private final TableView<Venda> compras = new TableView<>();
    private boolean comprasCarregadas;

    /**
     * @param casos      casos de uso
     * @param navegador  para voltar à lista e atualizar a faixa do caixa depois de receber
     * @param cliente    cliente do perfil
     * @param abaInicial aba que abre selecionada
     */
    public TelaPerfilCliente(CasosDeUso casos, Navegador navegador, Cliente cliente, Aba abaInicial) {
        this.casos = casos;
        this.navegador = navegador;
        this.cliente = cliente;
        montar();
        exibirDados();
        raiz.addEventFilter(KeyEvent.KEY_PRESSED, this::aoPressionarTecla);
        recarregarConta();
        abas.getSelectionModel().select(switch (abaInicial) {
            case DADOS -> abaDados;
            case CONTA_FIADO -> abaConta;
            case COMPRAS -> abaCompras;
        });
        Platform.runLater(this::focarAbaAtual);
    }

    /** @return conteúdo da tela */
    public Parent raiz() {
        return raiz;
    }

    // ------------------------------------------------------------------ montagem

    private void montar() {
        Button voltar = new Button("← Clientes (Esc)");
        voltar.getStyleClass().add("botao-secundario");
        voltar.setOnAction(evento -> navegador.irParaClientes());
        nomeNoTopo.getStyleClass().add("titulo-secao");
        devidoNoTopo.getStyleClass().add("valor-total");
        Region espaco = new Region();
        HBox.setHgrow(espaco, Priority.ALWAYS);
        HBox topo = new HBox(16, voltar, nomeNoTopo, espaco, devidoNoTopo);
        topo.setAlignment(Pos.CENTER_LEFT);

        abaDados.setContent(montarDados());
        abaConta.setContent(montarConta());
        abaCompras.setContent(montarCompras());
        abas.getTabs().addAll(abaDados, abaConta, abaCompras);
        abas.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        abas.getSelectionModel().selectedItemProperty().addListener((obs, antes, agora) -> {
            Avisos.limpar(mensagem);
            if (agora == abaCompras && !comprasCarregadas) {
                buscarCompras();
            }
            Platform.runLater(this::focarAbaAtual);
        });

        mensagem.setWrapText(true);
        mensagem.setMinHeight(24);
        raiz.getChildren().addAll(topo, abas, mensagem);
        VBox.setVgrow(abas, Priority.ALWAYS);
        raiz.setPadding(new Insets(16, 24, 16, 24));
    }

    private VBox montarDados() {
        Button editar = new Button("Editar dados (F2)");
        editar.getStyleClass().add("botao-primario");
        editar.setOnAction(evento -> editarDados());
        Label aviso = new Label("O CPF só aparece aqui e no cadastro; nas listas e no caixa aparece apenas o nome.");
        aviso.getStyleClass().add("dica");
        aviso.setWrapText(true);
        VBox caixa = new VBox(16, new Formulario().campo("Nome", nome).campo("CPF", cpf).campo("Telefone", telefone),
                editar, aviso);
        caixa.setPadding(new Insets(16));
        return caixa;
    }

    private VBox montarConta() {
        debitos.setPlaceholder(new Label("Nenhuma compra fiado em aberto."));
        debitos.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        debitos.getColumns().add(colunaMarcar());
        debitos.getColumns().add(colunaDebito("Data", 150, d -> Formatos.dataHora(d.getCriadoEm())));
        debitos.getColumns().add(colunaDebito("Venda nº", 90, d -> String.valueOf(d.getVendaId())));
        debitos.getColumns().add(colunaDebito("Situação", 100, d -> descrever(d.getStatus())));
        debitos.getColumns().add(colunaDebito("Valor", 110, d -> Formatos.moeda(d.getValorOriginal())));
        debitos.getColumns().add(colunaDebito("Pago", 110, d -> Formatos.moeda(d.getValorPago())));
        debitos.getColumns().add(colunaDebito("Saldo", 110, d -> Formatos.moeda(d.calcularSaldo())));
        debitos.setPrefHeight(240);
        // duplo clique ou Enter numa compra abre os detalhes da venda
        debitos.setOnMouseClicked(evento -> {
            if (evento.getButton() == MouseButton.PRIMARY && evento.getClickCount() == 2) {
                abrirVendaDoFiado();
            }
        });
        debitos.setOnKeyPressed(evento -> {
            if (evento.getCode() == KeyCode.ENTER) {
                abrirVendaDoFiado();
            }
        });
        // Espaço marca/desmarca: filtro para a tabela não usar o Espaço para outra coisa
        debitos.addEventFilter(KeyEvent.KEY_PRESSED, evento -> {
            LinhaDebito linha = debitos.getSelectionModel().getSelectedItem();
            if (evento.getCode() == KeyCode.SPACE && linha != null) {
                if (linha.debito.estaPendente()) {
                    linha.marcada.set(!linha.marcada.get());
                }
                evento.consume();
            }
        });
        mostrarQuitadas.setOnAction(evento -> exibirConta(conta));

        totalDevido.getStyleClass().add("valor-total");
        resumoSelecao.getStyleClass().add("dica");
        Region espaco = new Region();
        HBox.setHgrow(espaco, Priority.ALWAYS);
        HBox linhaTotais = new HBox(16, totalDevido, resumoSelecao, espaco, mostrarQuitadas);
        linhaTotais.setAlignment(Pos.CENTER_LEFT);

        Button pagarMarcadas = new Button("Pagar selecionadas (F9)");
        pagarMarcadas.getStyleClass().add("botao-primario");
        pagarMarcadas.setOnAction(evento -> pagarSelecionadas());
        Button valorLivre = new Button("Pagar valor livre (F10)");
        valorLivre.getStyleClass().add("botao-secundario");
        valorLivre.setOnAction(evento -> pagarValorLivre());
        Button quitar = new Button("Quitar tudo (F11)");
        quitar.getStyleClass().add("botao-secundario");
        quitar.setOnAction(evento -> quitarTudo());
        Label dica = new Label("Espaço marca a compra · Enter ou duplo clique: detalhes da venda. "
                + "O valor abate da compra mais antiga para a mais nova.");
        dica.getStyleClass().add("dica");
        HBox botoes = new HBox(8, pagarMarcadas, valorLivre, quitar, dica);
        botoes.setAlignment(Pos.CENTER_LEFT);

        VBox caixa = new VBox(10, debitos, linhaTotais, botoes);
        VBox.setVgrow(debitos, Priority.ALWAYS);
        caixa.setPadding(new Insets(12, 0, 0, 0));
        return caixa;
    }

    private TableColumn<LinhaDebito, Boolean> colunaMarcar() {
        TableColumn<LinhaDebito, Boolean> coluna = new TableColumn<>("Pagar");
        coluna.setCellValueFactory(dados -> dados.getValue().marcada);
        coluna.setSortable(false);
        coluna.setPrefWidth(60);
        coluna.setCellFactory(c -> new TableCell<>() {
            private final CheckBox caixa = new CheckBox();

            {
                caixa.setFocusTraversable(false);
                caixa.setOnAction(evento -> linha().ifPresent(l -> l.marcada.set(caixa.isSelected())));
            }

            @Override
            protected void updateItem(Boolean marcada, boolean vazio) {
                super.updateItem(marcada, vazio);
                LinhaDebito l = vazio ? null : linha().orElse(null);
                if (l == null) {
                    setGraphic(null);
                    return;
                }
                caixa.setSelected(l.marcada.get());
                caixa.setDisable(!l.debito.estaPendente()); // compra quitada não se paga de novo
                setGraphic(caixa);
            }

            private java.util.Optional<LinhaDebito> linha() {
                int i = getIndex();
                return i < 0 || i >= getTableView().getItems().size()
                        ? java.util.Optional.empty() : java.util.Optional.of(getTableView().getItems().get(i));
            }
        });
        return coluna;
    }

    private static TableColumn<LinhaDebito, String> colunaDebito(String titulo, double largura,
                                                                 Function<Debito, String> texto) {
        TableColumn<LinhaDebito, String> coluna = new TableColumn<>(titulo);
        coluna.setCellValueFactory(dados -> new ReadOnlyStringWrapper(texto.apply(dados.getValue().debito)));
        coluna.setPrefWidth(largura);
        coluna.setSortable(false);
        return coluna;
    }

    private VBox montarCompras() {
        inicio.setPrefWidth(150);
        fim.setPrefWidth(150);
        // muda a data, busca de novo (sem precisar de botão)
        inicio.valueProperty().addListener((obs, antes, agora) -> buscarCompras());
        fim.valueProperty().addListener((obs, antes, agora) -> buscarCompras());
        Label de = new Label("_De");
        de.setMnemonicParsing(true);
        de.setLabelFor(inicio);
        Label ate = new Label("_Até");
        ate.setMnemonicParsing(true);
        ate.setLabelFor(fim);
        HBox periodo = new HBox(8, de, inicio, ate, fim);
        periodo.setAlignment(Pos.CENTER_LEFT);

        compras.setPlaceholder(new Label("Nenhuma compra neste período."));
        compras.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        compras.getColumns().add(colunaVenda("Data", 160, v -> v.getDataHora().map(Formatos::dataHora).orElse("")));
        compras.getColumns().add(colunaVenda("Venda nº", 90, v -> v.getId().map(String::valueOf).orElse("")));
        compras.getColumns().add(colunaVenda("Forma", 140,
                v -> v.getFormaPagamento().map(FormaPagamento::getDescricao).orElse("")));
        compras.getColumns().add(colunaVenda("Itens", 70, v -> String.valueOf(v.getItens().size())));
        compras.getColumns().add(colunaVenda("Total", 120, v -> Formatos.moeda(v.calcularTotal())));
        compras.setPrefHeight(240);
        // duplo clique ou Enter numa compra abre os detalhes (o histórico já vem com os itens)
        compras.setOnMouseClicked(evento -> {
            if (evento.getButton() == MouseButton.PRIMARY && evento.getClickCount() == 2) {
                abrirCompra();
            }
        });
        compras.setOnKeyPressed(evento -> {
            if (evento.getCode() == KeyCode.ENTER) {
                abrirCompra();
            }
        });
        Label dica = new Label("Enter ou duplo clique numa compra: detalhes da venda.");
        dica.getStyleClass().add("dica");

        VBox caixa = new VBox(10, periodo, compras, dica);
        VBox.setVgrow(compras, Priority.ALWAYS);
        caixa.setPadding(new Insets(12, 0, 0, 0));
        return caixa;
    }

    private static TableColumn<Venda, String> colunaVenda(String titulo, double largura, Function<Venda, String> texto) {
        TableColumn<Venda, String> coluna = new TableColumn<>(titulo);
        coluna.setCellValueFactory(dados -> new ReadOnlyStringWrapper(texto.apply(dados.getValue())));
        coluna.setPrefWidth(largura);
        return coluna;
    }

    // ------------------------------------------------------------------ teclado

    private void aoPressionarTecla(KeyEvent evento) {
        if (evento.isShortcutDown()) {
            Tab destino = switch (evento.getCode()) {
                case DIGIT1, NUMPAD1 -> abaDados;
                case DIGIT2, NUMPAD2 -> abaConta;
                case DIGIT3, NUMPAD3 -> abaCompras;
                default -> null;
            };
            if (destino != null) {
                abas.getSelectionModel().select(destino);
                evento.consume();
            }
            return;
        }
        switch (evento.getCode()) {
            case ESCAPE -> navegador.irParaClientes();
            case F2 -> editarDados();
            case F9 -> naConta(this::pagarSelecionadas);
            case F10 -> naConta(this::pagarValorLivre);
            case F11 -> naConta(this::quitarTudo);
            default -> {
                return;
            }
        }
        evento.consume();
    }

    /** As teclas de pagamento funcionam de qualquer aba, mas mostram a conta antes. */
    private void naConta(Runnable acao) {
        abas.getSelectionModel().select(abaConta);
        acao.run();
    }

    private void focarAbaAtual() {
        Tab atual = abas.getSelectionModel().getSelectedItem();
        if (atual == abaConta) {
            debitos.requestFocus();
            if (debitos.getSelectionModel().isEmpty() && !debitos.getItems().isEmpty()) {
                debitos.getSelectionModel().selectFirst();
            }
        } else if (atual == abaCompras) {
            compras.requestFocus();
        }
    }

    // ------------------------------------------------------------------ dados

    private void exibirDados() {
        nomeNoTopo.setText(cliente.getNome());
        nome.setText(cliente.getNome());
        cpf.setText(cliente.getCpf().map(Cpf::formatado).orElse("não informado"));
        telefone.setText(cliente.getTelefone().orElse("não informado"));
    }

    private void editarDados() {
        new DialogoCliente(casos, cliente).mostrar().ifPresent(atualizado -> {
            cliente = atualizado;
            exibirDados();
            Avisos.sucesso(mensagem, "Dados de \"" + atualizado.getNome() + "\" atualizados.");
        });
    }

    // ------------------------------------------------------------------ conta fiado

    private void recarregarConta() {
        Tarefas.emSegundoPlano(debitos, () -> casos.consultarContaFiado().executar(cliente), this::exibirConta,
                erro -> Avisos.tratar(erro, mensagem));
    }

    private void exibirConta(ContaFiado novaConta) {
        if (novaConta == null) {
            return;
        }
        Set<Long> marcadasAntes = marcadas().stream().map(Debito::getVendaId).collect(Collectors.toSet());
        Long selecionadaAntes = debitos.getSelectionModel().getSelectedItem() == null
                ? null : debitos.getSelectionModel().getSelectedItem().debito.getVendaId();
        conta = novaConta;

        List<Debito> visiveis = new ArrayList<>(conta.getPendentes());
        if (mostrarQuitadas.isSelected()) {
            visiveis.addAll(conta.getQuitadas());
        }
        visiveis.sort(Comparator.comparing(Debito::getCriadoEm)); // mesma ordem em que o dinheiro abate
        List<LinhaDebito> linhas = new ArrayList<>();
        for (Debito debito : visiveis) {
            LinhaDebito linha = new LinhaDebito(debito);
            linha.marcada.set(debito.estaPendente() && marcadasAntes.contains(debito.getVendaId()));
            linha.marcada.addListener((obs, antes, agora) -> atualizarResumo());
            linhas.add(linha);
        }
        debitos.getItems().setAll(linhas);
        linhas.stream().filter(l -> Long.valueOf(l.debito.getVendaId()).equals(selecionadaAntes)).findFirst()
                .ifPresentOrElse(l -> debitos.getSelectionModel().select(l), () -> {
                    if (!linhas.isEmpty()) {
                        debitos.getSelectionModel().selectFirst();
                    }
                });
        atualizarResumo();
        if (abas.getSelectionModel().getSelectedItem() == abaConta) {
            debitos.requestFocus(); // durante a busca a tabela fica desabilitada e não aceita o foco
        }
    }

    private void atualizarResumo() {
        BigDecimal devido = conta == null ? BigDecimal.ZERO : conta.calcularSaldoDevedor();
        totalDevido.setText("Total devido: " + Formatos.moeda(devido));
        devidoNoTopo.setText(devido.signum() > 0 ? "Deve " + Formatos.moeda(devido) : "Nada em aberto");
        devidoNoTopo.getStyleClass().remove("valor-devedor");
        if (devido.signum() > 0) {
            devidoNoTopo.getStyleClass().add("valor-devedor");
        }
        List<Debito> marcadas = marcadas();
        resumoSelecao.setText(marcadas.isEmpty() ? "Nenhuma compra marcada."
                : "Marcadas: " + marcadas.size() + (marcadas.size() == 1 ? " compra" : " compras")
                + " · saldo " + Formatos.moeda(somaDosSaldos(marcadas)));
    }

    private List<Debito> marcadas() {
        return debitos.getItems().stream().filter(l -> l.marcada.get() && l.debito.estaPendente())
                .map(l -> l.debito).toList();
    }

    private static BigDecimal somaDosSaldos(List<Debito> lista) {
        return lista.stream().map(Debito::calcularSaldo).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * A conta de fiado guarda só o número da venda: os itens são buscados no banco na hora.
     * O operador da venda não vem nessa consulta, por isso a janela omite essa linha.
     */
    private void abrirVendaDoFiado() {
        LinhaDebito linha = debitos.getSelectionModel().getSelectedItem();
        if (linha == null) {
            return;
        }
        long vendaId = linha.debito.getVendaId();
        Tarefas.emSegundoPlano(debitos, () -> casos.consultarVenda().executar(vendaId), venda ->
                venda.ifPresentOrElse(v -> {
                    new DialogoDetalheVenda(v, null).mostrar();
                    debitos.requestFocus(); // a tabela ficou desabilitada durante a busca e perdeu o foco
                }, () -> Avisos.erro(mensagem, "Venda nº " + vendaId + " não encontrada.")),
                erro -> Avisos.tratar(erro, mensagem));
    }

    private void abrirCompra() {
        Venda venda = compras.getSelectionModel().getSelectedItem();
        if (venda != null) {
            new DialogoDetalheVenda(venda, null).mostrar();
        }
    }

    private void pagarSelecionadas() {
        List<Debito> marcadas = marcadas();
        if (marcadas.isEmpty()) {
            Avisos.erro(mensagem, "Marque ao menos uma compra com Espaço, ou use Pagar valor livre (F10).");
            return;
        }
        BigDecimal saldo = somaDosSaldos(marcadas);
        Set<Long> vendas = marcadas.stream().map(Debito::getVendaId).collect(Collectors.toCollection(LinkedHashSet::new));
        comCaixaAberto(() -> DialogoPagamento.paraFiado(cliente.getNome() + " — " + marcadas.size()
                        + (marcadas.size() == 1 ? " compra marcada" : " compras marcadas") + ", saldo "
                        + Formatos.moeda(saldo) + ".\nPode pagar menos: abate da mais antiga para a mais nova.",
                saldo, true).mostrar().ifPresent(escolha -> receber(vendas, escolha)));
    }

    private void pagarValorLivre() {
        if (!temDivida()) {
            return;
        }
        BigDecimal devido = conta.calcularSaldoDevedor();
        comCaixaAberto(() -> DialogoPagamento.paraFiado(cliente.getNome() + " deve " + Formatos.moeda(devido)
                        + ".\nO valor abate da compra mais antiga para a mais nova.", devido, true)
                .mostrar().ifPresent(escolha -> receber(Set.of(), escolha)));
    }

    private void quitarTudo() {
        if (!temDivida()) {
            return;
        }
        BigDecimal devido = conta.calcularSaldoDevedor();
        int quantas = conta.getPendentes().size();
        comCaixaAberto(() -> DialogoPagamento.paraFiado("Quitar tudo de " + cliente.getNome() + ": " + quantas
                        + (quantas == 1 ? " compra" : " compras") + ", " + Formatos.moeda(devido) + ".", devido, false)
                .mostrar().ifPresent(escolha -> receber(Set.of(), escolha)));
    }

    private boolean temDivida() {
        if (conta == null || conta.calcularSaldoDevedor().signum() <= 0) {
            Avisos.erro(mensagem, cliente.getNome() + " não tem fiado em aberto.");
            return false;
        }
        return true;
    }

    /**
     * Confere o caixa antes de abrir a janela de pagamento, para o operador não digitar tudo e só
     * então descobrir que o caixa está fechado. O caso de uso confere de novo na hora de gravar.
     */
    private void comCaixaAberto(Runnable abrirPagamento) {
        Avisos.limpar(mensagem);
        Tarefas.emSegundoPlano(debitos, casos.consultarCaixaAberto()::executar, caixa -> {
            if (caixa.isEmpty()) {
                Avisos.erro(mensagem, "O caixa está fechado. Abra o caixa (área Caixa) antes de receber fiado: "
                        + "o dinheiro recebido entra na gaveta.");
            } else {
                abrirPagamento.run();
            }
        }, erro -> Avisos.tratar(erro, mensagem));
    }

    private void receber(Set<Long> vendas, DialogoPagamento.Escolha escolha) {
        PagarDebito.Comando comando = new PagarDebito.Comando(cliente, vendas, escolha.valor(), escolha.forma(),
                escolha.valorRecebido());
        Tarefas.emSegundoPlano(raiz, () -> casos.pagarDebito().executar(comando), comprovante -> {
            Avisos.sucesso(mensagem, "Recebido " + Formatos.moeda(escolha.valor()) + " ("
                    + escolha.forma().getDescricao() + "). Ainda deve: "
                    + Formatos.moeda(comprovante.saldoDevedorFinal()) + ".");
            mostrarComprovante(comprovante, escolha);
            navegador.atualizarSituacaoCaixa();
            recarregarConta();
        }, erro -> Avisos.tratar(erro, mensagem));
    }

    private void mostrarComprovante(PagarDebito.Comprovante comprovante, DialogoPagamento.Escolha escolha) {
        StringBuilder texto = new StringBuilder();
        for (PagamentoAplicado aplicado : comprovante.aplicados()) {
            Debito debito = aplicado.debito();
            texto.append("Venda nº ").append(debito.getVendaId()).append(": ")
                    .append(Formatos.moeda(aplicado.pagamento().valor()))
                    .append(debito.estaPendente() ? "  (falta " + Formatos.moeda(debito.calcularSaldo()) + ")"
                            : "  (quitada)")
                    .append('\n');
        }
        texto.append('\n');
        if (escolha.forma() == FormaPagamento.DINHEIRO) {
            texto.append("Recebido em dinheiro: ").append(Formatos.moeda(comprovante.valorRecebido())).append('\n')
                    .append("Troco: ").append(Formatos.moeda(comprovante.troco())).append('\n');
        }
        texto.append("Saldo devedor restante: ").append(Formatos.moeda(comprovante.saldoDevedorFinal()));

        Alert alerta = new Alert(Alert.AlertType.INFORMATION, texto.toString());
        alerta.setTitle("Recebimento de fiado");
        alerta.setHeaderText(cliente.getNome() + " pagou " + Formatos.moeda(escolha.valor()) + " ("
                + escolha.forma().getDescricao() + ")");
        Tema.aplicar(alerta.getDialogPane());
        alerta.showAndWait();
    }

    // ------------------------------------------------------------------ compras

    private void buscarCompras() {
        LocalDate de = inicio.getValue();
        LocalDate ate = fim.getValue();
        if (de == null || ate == null) {
            Avisos.erro(mensagem, "Informe as duas datas do período.");
            return;
        }
        comprasCarregadas = true;
        Avisos.limpar(mensagem);
        Tarefas.emSegundoPlano(compras, () -> casos.consultarHistoricoCliente().executar(cliente, de, ate),
                vendas -> compras.getItems().setAll(vendas), erro -> {
                    compras.getItems().clear();
                    Avisos.tratar(erro, mensagem);
                });
    }

    private static String descrever(StatusDebito status) {
        return switch (status) {
            case ABERTO -> "Em aberto";
            case PARCIAL -> "Parcial";
            case QUITADO -> "Quitada";
        };
    }

    /** Uma compra fiado na tabela, com a marcação "pagar esta". */
    private static final class LinhaDebito {
        private final Debito debito;
        private final BooleanProperty marcada = new SimpleBooleanProperty(false);

        private LinhaDebito(Debito debito) {
            this.debito = debito;
        }
    }
}
