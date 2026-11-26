package br.com.ferragem.apresentacao.tela;

import br.com.ferragem.CasosDeUso;
import br.com.ferragem.aplicacao.casodeuso.RegistrarVenda;
import br.com.ferragem.apresentacao.Navegador;
import br.com.ferragem.apresentacao.comum.Avisos;
import br.com.ferragem.apresentacao.comum.Formatos;
import br.com.ferragem.apresentacao.comum.Formulario;
import br.com.ferragem.apresentacao.comum.Sessao;
import br.com.ferragem.apresentacao.comum.Tarefas;
import br.com.ferragem.apresentacao.comum.Tema;
import br.com.ferragem.dominio.excecao.RegraNegocioException;
import br.com.ferragem.dominio.modelo.cliente.Cliente;
import br.com.ferragem.dominio.modelo.produto.Produto;
import br.com.ferragem.dominio.modelo.venda.FormaPagamento;
import br.com.ferragem.dominio.modelo.venda.ItemVenda;
import br.com.ferragem.dominio.modelo.venda.Venda;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Ponto de venda (PDV): o carrinho do caixa, operado pelo teclado.
 *
 * <p><b>Quem faz o quê:</b> o carrinho é um objeto {@link Venda} do domínio, mantido em memória.
 * Incluir, mudar quantidade, dar desconto e remover chamam métodos da {@code Venda}, que aplicam
 * as regras (fracionamento, estoque, preço abaixo do custo); se ela recusar, a mensagem aparece
 * em vermelho e nada muda. Só a busca de produto e a conclusão da venda vão ao banco, pelos
 * casos de uso {@code ConsultarProdutos} e {@code RegistrarVenda}, em segundo plano.</p>
 *
 * <p><b>Teclado:</b> o campo de código fica sempre com o foco (o leitor de código de barras
 * "digita" o código e envia Enter). Os atalhos estão em {@link AtalhoPdv}.</p>
 *
 * <p><b>Caixa fechado:</b> a própria tela pede o troco e abre o caixa; não existe "ir para outra
 * tela abrir o caixa". Suprimento, sangria e fechamento ficam na gaveta (F1).</p>
 */
public final class TelaPdv {

    private final CasosDeUso casos;
    private final Sessao sessao;
    private final Navegador navegador;

    private final StackPane raiz = new StackPane();
    private final BorderPane areaVenda = new BorderPane();
    private final TextField campoCodigo = new TextField();
    private final Label rotuloProximaQuantidade = new Label();
    private final TableView<ItemVenda> tabela = new TableView<>();
    private final Label rotuloCliente = new Label();
    private final Label rotuloItens = new Label();
    private final Label rotuloTotal = new Label();
    private final Label mensagem = new Label();

    private Venda venda = new Venda();
    private BigDecimal quantidadeProxima;

    /**
     * @param casos     casos de uso
     * @param sessao    operador logado
     * @param navegador para atualizar a faixa de situação do caixa
     */
    public TelaPdv(CasosDeUso casos, Sessao sessao, Navegador navegador) {
        this.casos = casos;
        this.sessao = sessao;
        this.navegador = navegador;
        montarAreaVenda();
        areaVenda.addEventFilter(KeyEvent.KEY_PRESSED, this::aoPressionarTecla);
        atualizar();
    }

    /** @return conteúdo da tela */
    public Parent raiz() {
        return raiz;
    }

    /**
     * Chamado sempre que a tela é exibida: confere se o caixa está aberto (alguém pode tê-lo
     * fechado enquanto o operador estava em outra tela).
     */
    public void aoMostrar() {
        Tarefas.emSegundoPlano(raiz, casos.consultarCaixaAberto()::executar, caixa -> {
            if (caixa.isPresent()) {
                raiz.getChildren().setAll(areaVenda);
                focarCampo();
            } else {
                raiz.getChildren().setAll(aberturaDoCaixa());
            }
        }, erro -> Avisos.falhaTecnica(erro));
    }

    // ------------------------------------------------------------------ montagem

    private void montarAreaVenda() {
        campoCodigo.setPromptText("Passe o leitor ou digite o código e Enter");
        campoCodigo.getStyleClass().add("campo-codigo");
        HBox.setHgrow(campoCodigo, Priority.ALWAYS);
        campoCodigo.addEventFilter(KeyEvent.KEY_PRESSED, evento -> {
            if (evento.getCode() == KeyCode.ENTER) {
                incluirPeloCampo();
                evento.consume();
            }
        });
        rotuloProximaQuantidade.getStyleClass().add("rotulo-grupo");
        HBox linhaCodigo = new HBox(12, campoCodigo, rotuloProximaQuantidade);
        linhaCodigo.setAlignment(Pos.CENTER_LEFT);
        // rótulo fixo: o texto de exemplo dentro do campo some quando ele tem o cursor, e no PDV ele sempre tem
        Label rotuloCodigo = new Label("Código de barras, código ou nome do produto  (Enter inclui · F3 busca)");
        rotuloCodigo.setLabelFor(campoCodigo);
        rotuloCodigo.getStyleClass().add("rotulo-grupo");

        montarTabela();
        mensagem.setWrapText(true);
        VBox centro = new VBox(12, new VBox(4, rotuloCodigo, linhaCodigo), tabela, mensagem);
        VBox.setVgrow(tabela, Priority.ALWAYS);
        centro.setPadding(new Insets(16));

        areaVenda.setCenter(centro);
        areaVenda.setRight(painelTotal());
        areaVenda.setBottom(barraAtalhos());
    }

    private void montarTabela() {
        tabela.getStyleClass().add("tabela-pdv");
        tabela.setPlaceholder(new Label("Carrinho vazio. Passe o primeiro produto."));
        tabela.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        TableColumn<ItemVenda, String> numero = new TableColumn<>("#");
        numero.setCellFactory(coluna -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean vazio) {
                super.updateItem(item, vazio);
                setText(vazio ? null : String.valueOf(getIndex() + 1));
            }
        });
        numero.setMaxWidth(50);
        numero.setSortable(false);

        tabela.getColumns().add(numero);
        tabela.getColumns().add(coluna("Código", 110, item -> item.getProduto().getCodigo()));
        tabela.getColumns().add(coluna("Produto", 300, item -> item.getProduto().getNome()));
        tabela.getColumns().add(coluna("Quantidade", 120,
                item -> Formatos.quantidade(item.getQuantidade(), item.getProduto().getUnidade())));
        tabela.getColumns().add(coluna("Preço un.", 110, item -> Formatos.moeda(item.getPrecoUnitario())));
        tabela.getColumns().add(coluna("Subtotal", 120, item -> Formatos.moeda(item.calcularSubtotal())));
        // a tabela não recebe foco pelo Tab: o foco é do campo de código
        tabela.setFocusTraversable(false);
    }

    private static TableColumn<ItemVenda, String> coluna(String titulo, double largura,
                                                         Function<ItemVenda, String> texto) {
        TableColumn<ItemVenda, String> coluna = new TableColumn<>(titulo);
        coluna.setCellValueFactory(dados -> new ReadOnlyStringWrapper(texto.apply(dados.getValue())));
        coluna.setPrefWidth(largura);
        coluna.setSortable(false);
        return coluna;
    }

    private VBox painelTotal() {
        Label tituloCliente = new Label("Cliente");
        tituloCliente.getStyleClass().add("rotulo-grupo");
        rotuloCliente.setWrapText(true);
        Label tituloTotal = new Label("Total");
        tituloTotal.getStyleClass().add("rotulo-grupo");
        rotuloTotal.getStyleClass().add("total-pdv");

        Button pagar = new Button("Pagamento (F9)");
        pagar.getStyleClass().add("botao-primario");
        pagar.setMaxWidth(Double.MAX_VALUE);
        pagar.setFocusTraversable(false);
        pagar.setOnAction(evento -> {
            irParaPagamento();
            focarCampo();
        });

        // a gaveta também tem botão: fechar o caixa não pode depender de decorar uma tecla
        Button gaveta = new Button("Gaveta (F1)");
        gaveta.getStyleClass().add("botao-secundario");
        gaveta.setMaxWidth(Double.MAX_VALUE);
        gaveta.setFocusTraversable(false);
        gaveta.setOnAction(evento -> {
            abrirGaveta();
            focarCampo();
        });
        Label dicaGaveta = new Label("Suprimento, sangria e fechamento do caixa");
        dicaGaveta.setWrapText(true);
        dicaGaveta.getStyleClass().add("dica");

        Region espaco = new Region();
        VBox.setVgrow(espaco, Priority.ALWAYS);
        VBox painel = new VBox(8, tituloCliente, rotuloCliente, rotuloItens, new Label(), tituloTotal, rotuloTotal,
                pagar, espaco, gaveta, dicaGaveta);
        painel.getStyleClass().add("painel-total");
        painel.setPadding(new Insets(16));
        painel.setPrefWidth(260);
        return painel;
    }

    private HBox barraAtalhos() {
        HBox barra = new HBox(14);
        for (AtalhoPdv atalho : AtalhoPdv.values()) {
            Label tecla = new Label(atalho.getRotuloTecla());
            tecla.getStyleClass().add("tecla");
            barra.getChildren().add(new HBox(4, tecla, new Label(atalho.getNomeCurto())));
        }
        barra.getStyleClass().add("barra-atalhos");
        barra.setPadding(new Insets(8, 16, 8, 16));
        barra.setAlignment(Pos.CENTER_LEFT);
        return barra;
    }

    private VBox aberturaDoCaixa() {
        Label titulo = new Label("Caixa fechado");
        titulo.getStyleClass().add("titulo-secao");
        Label texto = new Label("Informe o dinheiro que está na gaveta e abra o caixa para começar a vender.");
        texto.setWrapText(true);
        TextField troco = new TextField();
        troco.setPromptText("0,00");
        troco.setMaxWidth(160);
        Label erro = new Label();
        erro.setWrapText(true);
        Button abrir = new Button("Abrir caixa");
        abrir.getStyleClass().add("botao-primario");
        abrir.setDefaultButton(true);
        VBox cartao = new VBox(16, titulo, texto, new Formulario().campo("_Troco na gaveta (R$)", troco), abrir, erro);
        abrir.setOnAction(evento -> {
            Optional<BigDecimal> valor = Formatos.lerNumero(troco.getText());
            if (valor.isEmpty()) {
                Avisos.erro(erro, "Digite o valor do troco, por exemplo 100,00 (ou 0).");
                return;
            }
            Tarefas.emSegundoPlano(cartao, () -> casos.abrirCaixa().executar(sessao.operador(), valor.get()),
                    aberto -> {
                        navegador.atualizarSituacaoCaixa();
                        raiz.getChildren().setAll(areaVenda);
                        Avisos.sucesso(mensagem, "Caixa aberto. Bom trabalho!");
                        focarCampo();
                    },
                    falha -> Avisos.tratar(falha, erro));
        });
        cartao.getStyleClass().add("cartao");
        cartao.setMaxSize(480, Region.USE_PREF_SIZE);
        Platform.runLater(troco::requestFocus);
        return cartao;
    }

    // ------------------------------------------------------------------ teclado

    private void aoPressionarTecla(KeyEvent evento) {
        KeyCode tecla = evento.getCode();
        if (tecla == KeyCode.UP || tecla == KeyCode.DOWN) {
            moverSelecao(tecla == KeyCode.UP ? -1 : 1);
            evento.consume();
            return;
        }
        Optional<AtalhoPdv> atalho = AtalhoPdv.daTecla(tecla);
        if (atalho.isEmpty()) {
            return;
        }
        // Delete com texto no campo apaga a letra, não o item do carrinho
        if (atalho.get() == AtalhoPdv.REMOVER_ITEM && !campoCodigo.getText().isEmpty()) {
            return;
        }
        evento.consume();
        Avisos.limpar(mensagem);
        switch (atalho.get()) {
            case AJUDA -> mostrarAjuda();
            case QUANTIDADE_PROXIMA -> definirProximaQuantidade();
            case BUSCAR_PRODUTO -> {
                String digitado = campoCodigo.getText();
                campoCodigo.clear();
                buscarProduto(digitado);
            }
            case CLIENTE -> escolherCliente();
            case PRECO_ITEM -> alterarPreco();
            case QUANTIDADE_ITEM -> alterarQuantidade();
            case REMOVER_ITEM -> removerItem();
            case CANCELAR_VENDA -> cancelarVenda();
            case PAGAR -> irParaPagamento();
            case GAVETA -> abrirGaveta();
            case ORCAMENTO -> orcamento();
        }
        focarCampo();
    }

    /** F1: lista gerada a partir de {@link AtalhoPdv}, para nunca ficar desatualizada. */
    private static void mostrarAjuda() {
        StringBuilder texto = new StringBuilder("Enter  ·  inclui o produto do campo de código\n")
                .append("↑ / ↓  ·  seleciona o item do carrinho\n");
        for (AtalhoPdv atalho : AtalhoPdv.values()) {
            texto.append(atalho.getRotuloTecla()).append("  ·  ").append(atalho.getDescricao()).append('\n');
        }
        texto.append("\nNo pagamento: 1 Dinheiro · 2 Pix · 3 Crédito · 4 Débito · 5 Fiado · Enter confirma · Esc volta");
        Alert ajuda = new Alert(Alert.AlertType.INFORMATION);
        Tema.aplicar(ajuda.getDialogPane());
        ajuda.setTitle("Atalhos do PDV");
        ajuda.setHeaderText("Atalhos de teclado");
        ajuda.setContentText(texto.toString());
        ajuda.showAndWait();
    }

    private void moverSelecao(int passo) {
        int total = tabela.getItems().size();
        if (total == 0) {
            return;
        }
        int atual = tabela.getSelectionModel().getSelectedIndex();
        int nova = Math.max(0, Math.min(total - 1, (atual < 0 ? total : atual) + passo));
        tabela.getSelectionModel().select(nova);
        tabela.scrollTo(nova);
    }

    // ------------------------------------------------------------------ incluir produto

    /**
     * Enter no campo de código. O campo é esvaziado na hora e <b>não</b> fica desabilitado durante
     * a busca: o leitor de código de barras manda o próximo código em milissegundos, e o que ele
     * "digitasse" num campo desabilitado seria perdido.
     */
    private void incluirPeloCampo() {
        String termo = campoCodigo.getText().trim();
        if (termo.isEmpty()) {
            return;
        }
        campoCodigo.clear();
        Avisos.limpar(mensagem);
        Tarefas.emSegundoPlano(null, () -> casos.consultarProdutos().executar(termo), encontrados -> {
            if (encontrados.size() == 1) {
                incluir(encontrados.get(0));
            } else if (encontrados.isEmpty()) {
                Avisos.erro(mensagem, "Produto \"" + termo + "\" não encontrado. Use F3 para buscar pelo nome.");
            } else {
                buscarProduto(termo);
            }
            focarCampo();
        }, erro -> {
            Avisos.tratar(erro, mensagem);
            focarCampo();
        });
    }

    private void buscarProduto(String termoInicial) {
        new DialogoBusca<Produto>("Buscar produto",
                "Digite parte do nome ou o código para filtrar. Enter ou ↓ vão para a lista.",
                casos.consultarProdutos()::executar, TelaPdv::descreverProduto, termoInicial)
                .mostrar()
                .ifPresent(this::incluir);
    }

    private static String descreverProduto(Produto produto) {
        return produto.getNome() + "   ·   " + produto.getCodigo() + "   ·   "
                + Formatos.moeda(produto.getPrecoVenda()) + "/" + produto.getUnidade().getSigla()
                + "   ·   estoque " + Formatos.quantidade(produto.getQuantidadeEstoque(), produto.getUnidade());
    }

    private void incluir(Produto produto) {
        BigDecimal quantidade = quantidadeProxima != null ? quantidadeProxima : BigDecimal.ONE;
        try {
            venda.adicionarItem(produto, quantidade);
            quantidadeProxima = null;
            atualizar();
            selecionar(produto.getCodigo());
        } catch (RegraNegocioException e) {
            Avisos.erro(mensagem, e.getMessage());
        }
    }

    private void definirProximaQuantidade() {
        DialogoValor.pedirNumero("Quantidade", "Quantidade do próximo produto (ex.: 2,5 para 2,5 kg ou 3 para 3 unidades).",
                "_Quantidade", "1").ifPresent(quantidade -> {
            if (quantidade.signum() <= 0) {
                Avisos.erro(mensagem, "A quantidade deve ser maior que zero.");
                return;
            }
            quantidadeProxima = quantidade;
            atualizar();
        });
    }

    // ------------------------------------------------------------------ alterar itens

    private Optional<ItemVenda> itemSelecionado() {
        ItemVenda item = tabela.getSelectionModel().getSelectedItem();
        if (item == null) {
            Avisos.erro(mensagem, "Selecione um item do carrinho com as setas ↑ e ↓.");
        }
        return Optional.ofNullable(item);
    }

    private void alterarQuantidade() {
        itemSelecionado().ifPresent(item -> DialogoValor.pedirNumero("Quantidade do item",
                        item.getProduto().getNome() + ": agora "
                                + Formatos.quantidade(item.getQuantidade(), item.getProduto().getUnidade()) + ".",
                        "_Nova quantidade", Formatos.numero(item.getQuantidade()))
                .ifPresent(quantidade -> aplicar(item, () ->
                        venda.alterarQuantidade(item.getProduto().getCodigo(), quantidade))));
    }

    private void alterarPreco() {
        itemSelecionado().ifPresent(item -> DialogoValor.pedirNumero("Preço do item",
                        item.getProduto().getNome() + ": preço atual " + Formatos.moeda(item.getPrecoUnitario())
                                + " por " + item.getProduto().getUnidade().getSigla() + ".",
                        "_Novo preço (R$)", "0,00")
                .ifPresent(preco -> aplicar(item, () -> venda.alterarPrecoUnitario(
                        item.getProduto().getCodigo(), preco, sessao.operador().getPerfil()))));
    }

    private void removerItem() {
        itemSelecionado().ifPresent(item -> {
            if (Avisos.confirmar("Remover item", "Remover \"" + item.getProduto().getNome() + "\" do carrinho?")) {
                venda.removerItem(item.getProduto().getCodigo());
                atualizar();
            }
        });
    }

    /** Alteração no carrinho que pode ser recusada por uma regra de negócio. */
    @FunctionalInterface
    private interface AlteracaoCarrinho {
        void aplicar() throws RegraNegocioException;
    }

    private void aplicar(ItemVenda item, AlteracaoCarrinho alteracao) {
        try {
            alteracao.aplicar();
            atualizar();
            selecionar(item.getProduto().getCodigo());
        } catch (RegraNegocioException e) {
            Avisos.erro(mensagem, e.getMessage());
        }
    }

    // ------------------------------------------------------------------ cliente

    private void escolherCliente() {
        Optional<Cliente> atual = venda.getCliente();
        if (atual.isPresent()) {
            ButtonType trocar = new ButtonType("Trocar cliente", ButtonBar.ButtonData.OK_DONE);
            ButtonType remover = new ButtonType("Remover cliente", ButtonBar.ButtonData.OTHER);
            Alert pergunta = new Alert(Alert.AlertType.CONFIRMATION, "Cliente desta venda: " + atual.get().getNome(),
                    trocar, remover, ButtonType.CANCEL);
            pergunta.setTitle("Cliente");
            pergunta.setHeaderText(null);
            Tema.aplicar(pergunta.getDialogPane());
            Optional<ButtonType> resposta = pergunta.showAndWait();
            if (resposta.filter(remover::equals).isPresent()) {
                venda.removerCliente();
                atualizar();
                return;
            }
            if (resposta.filter(trocar::equals).isEmpty()) {
                return;
            }
        }
        // a lista mostra só o nome: o CPF não aparece no PDV (decisão do grupo, LGPD)
        new DialogoBusca<Cliente>("Cliente",
                "Digite parte do nome ou o CPF para filtrar. Enter ou ↓ vão para a lista.",
                casos.consultarClientes()::executar, Cliente::getNome, "")
                .comAcaoExtra("Cadastrar novo (F2)", KeyCode.F2, () -> new DialogoCliente(casos, null).mostrar())
                .mostrar()
                .ifPresent(cliente -> {
                    venda.identificarCliente(cliente);
                    atualizar();
                });
    }

    // ------------------------------------------------------------------ cancelar e pagar

    private void cancelarVenda() {
        if (venda.estaVazia() && venda.getCliente().isEmpty()) {
            return;
        }
        if (Avisos.confirmar("Cancelar venda", "Cancelar a venda inteira (" + venda.getItens().size() + " itens, "
                + Formatos.moeda(venda.calcularTotal()) + ")?")) {
            novaVenda();
            Avisos.sucesso(mensagem, "Venda cancelada.");
        }
    }

    // ------------------------------------------------------------------ orçamento

    /** F7: com itens, salva e imprime; com o carrinho vazio, abre os orçamentos guardados. */
    private void orcamento() {
        if (venda.estaVazia()) {
            abrirOrcamentos();
            return;
        }
        Tarefas.emSegundoPlano(raiz, () -> casos.salvarOrcamento().executar(venda, sessao.operador()), orcamento -> {
            long numero = orcamento.getId().orElseThrow();
            ImpressaoOrcamento.Resultado impressao = ImpressaoOrcamento.imprimir(orcamento, raiz.getScene().getWindow());
            // o carrinho continua como estava: o cliente pode decidir comprar na hora
            Avisos.sucesso(mensagem, impressao == ImpressaoOrcamento.Resultado.IMPRESSO
                    ? "Orçamento nº " + numero + " salvo e enviado para a impressora."
                    : "Orçamento nº " + numero + " salvo. " + ImpressaoOrcamento.descreverProblema(impressao)
                    + " Para imprimir depois: F7 com o carrinho vazio → Reimprimir.");
            focarCampo();
        }, erro -> {
            Avisos.tratar(erro, mensagem);
            focarCampo();
        });
    }

    private void abrirOrcamentos() {
        new DialogoOrcamentos(casos).mostrar().ifPresent(resultado -> {
            venda = resultado.carrinho();
            quantidadeProxima = null;
            atualizar();
            long numero = resultado.orcamento().getId().orElseThrow();
            if (!resultado.avisos().isEmpty()) {
                Alert alerta = new Alert(Alert.AlertType.WARNING);
                Tema.aplicar(alerta.getDialogPane());
                alerta.setTitle("Orçamento carregado");
                alerta.setHeaderText("Confira com o cliente o que mudou desde o orçamento nº " + numero + ":");
                alerta.setContentText(DialogoOrcamentos.descreverAvisos(resultado).strip());
                alerta.showAndWait();
            }
            Avisos.sucesso(mensagem, "Orçamento nº " + numero + " carregado no carrinho. Confira e aperte F9 para vender.");
        });
    }

    private void abrirGaveta() {
        boolean alterou = new DialogoGaveta(casos, sessao, venda.estaVazia()).mostrar();
        if (alterou) {
            navegador.atualizarSituacaoCaixa();
            aoMostrar(); // se o caixa foi fechado, a tela volta para a abertura
        }
    }

    private void irParaPagamento() {
        if (venda.estaVazia()) {
            Avisos.erro(mensagem, "O carrinho está vazio.");
            return;
        }
        DialogoPagamento.paraVenda(venda.calcularTotal(), venda.getCliente().map(Cliente::getNome).orElse(null))
                .mostrar()
                .ifPresent(this::concluir);
    }

    private void concluir(DialogoPagamento.Escolha escolha) {
        RegistrarVenda.Comando comando = new RegistrarVenda.Comando(venda, escolha.forma(), escolha.valorRecebido(),
                sessao.operador());
        Tarefas.emSegundoPlano(raiz, () -> casos.registrarVenda().executar(comando), comprovante -> {
            mostrarComprovante(comprovante);
            navegador.atualizarSituacaoCaixa();
            novaVenda();
            Avisos.sucesso(mensagem, "Venda nº " + comprovante.vendaId() + " concluída.");
            focarCampo();
        }, erro -> {
            // o carrinho continua intacto (RegistrarVenda grava uma cópia): é só corrigir e tentar de novo
            Avisos.tratar(erro, mensagem);
            focarCampo();
        });
    }

    private static void mostrarComprovante(RegistrarVenda.Comprovante comprovante) {
        StringBuilder texto = new StringBuilder()
                .append("Total: ").append(Formatos.moeda(comprovante.total())).append('\n')
                .append("Forma: ").append(comprovante.forma().getDescricao()).append('\n');
        if (comprovante.forma() == FormaPagamento.DINHEIRO) {
            texto.append("Recebido: ").append(Formatos.moeda(comprovante.valorRecebido())).append('\n');
        }
        if (comprovante.codigoAutorizacao() != null && !comprovante.codigoAutorizacao().isBlank()) {
            texto.append("Autorização: ").append(comprovante.codigoAutorizacao()).append('\n');
        }
        Alert alerta = new Alert(Alert.AlertType.INFORMATION);
        Tema.aplicar(alerta.getDialogPane());
        alerta.setTitle("Venda concluída");
        alerta.setHeaderText("Venda nº " + comprovante.vendaId() + " concluída"
                + (comprovante.troco() != null && comprovante.troco().signum() > 0
                ? "  ·  TROCO " + Formatos.moeda(comprovante.troco()) : ""));
        alerta.setContentText(texto.toString());
        alerta.showAndWait();
    }

    private void novaVenda() {
        venda = new Venda();
        quantidadeProxima = null;
        atualizar();
    }

    // ------------------------------------------------------------------ exibição

    private void atualizar() {
        List<ItemVenda> itens = venda.getItens();
        tabela.getItems().setAll(itens);
        tabela.refresh();
        rotuloCliente.setText(venda.getCliente().map(Cliente::getNome).orElse("Sem cliente (F4 para identificar)"));
        rotuloItens.setText(itens.size() == 1 ? "1 item" : itens.size() + " itens");
        rotuloTotal.setText(Formatos.moeda(venda.calcularTotal()));
        rotuloProximaQuantidade.setText(quantidadeProxima == null
                ? "Qtd.: 1" : "Qtd.: " + Formatos.numero(quantidadeProxima) + " ×");
    }

    private void selecionar(String codigo) {
        for (int i = 0; i < tabela.getItems().size(); i++) {
            if (tabela.getItems().get(i).ehDoProduto(codigo)) {
                tabela.getSelectionModel().select(i);
                tabela.scrollTo(i);
                return;
            }
        }
    }

    private void focarCampo() {
        Platform.runLater(campoCodigo::requestFocus);
    }
}
