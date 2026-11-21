package br.com.ferragem.apresentacao.tela;

import br.com.ferragem.CasosDeUso;
import br.com.ferragem.aplicacao.casodeuso.AtualizarProduto;
import br.com.ferragem.aplicacao.casodeuso.CadastrarProduto;
import br.com.ferragem.apresentacao.comum.Avisos;
import br.com.ferragem.apresentacao.comum.Formatos;
import br.com.ferragem.apresentacao.comum.Formulario;
import br.com.ferragem.apresentacao.comum.Tarefas;
import br.com.ferragem.apresentacao.comum.Tema;
import br.com.ferragem.dominio.modelo.produto.Produto;
import br.com.ferragem.dominio.modelo.produto.UnidadeMedida;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Formulário de produto: cadastro novo ou edição.
 *
 * <p>A janela <b>só confere se os números são números</b>. Tudo o mais (nome obrigatório, preço de
 * venda maior que zero, código repetido, fracionamento do estoque mínimo) é regra de negócio e
 * vem dos casos de uso {@code CadastrarProduto} e {@code AtualizarProduto}. Se a regra recusar, a
 * mensagem aparece aqui dentro e a janela continua aberta com o que foi digitado.</p>
 *
 * <p>A unidade não muda depois do cadastro: um produto vendido por unidade que virasse "por kg"
 * tornaria incoerentes as vendas e o estoque já registrados.</p>
 */
public final class DialogoProduto {

    private final CasosDeUso casos;
    private final Produto existente;
    private final Dialog<Produto> dialogo = new Dialog<>();
    private final ComboBox<UnidadeMedida> unidade = new ComboBox<>();
    private final TextField codigo = new TextField();
    private final TextField codigoBarras = new TextField();
    private final TextField nome = new TextField();
    private final TextField custo = new TextField();
    private final TextField venda = new TextField();
    private final TextField estoqueInicial = new TextField();
    private final TextField estoqueMinimo = new TextField();
    private final Label mensagem = new Label();

    /**
     * @param casos     casos de uso
     * @param existente produto a editar, ou {@code null} para um cadastro novo
     */
    public DialogoProduto(CasosDeUso casos, Produto existente) {
        this.casos = casos;
        this.existente = existente;
        boolean novo = existente == null;
        dialogo.setTitle(novo ? "Novo produto" : "Editar produto");
        dialogo.setHeaderText(novo ? "Cadastrar produto" : existente.getNome());
        Tema.aplicar(dialogo.getDialogPane());

        ButtonType salvar = new ButtonType("Salvar", ButtonBar.ButtonData.OK_DONE);
        dialogo.getDialogPane().getButtonTypes().addAll(salvar, ButtonType.CANCEL);

        unidade.getItems().setAll(UnidadeMedida.values());
        unidade.setConverter(new StringConverter<>() {
            @Override
            public String toString(UnidadeMedida valor) {
                return valor == null ? "" : descreverUnidade(valor);
            }

            @Override
            public UnidadeMedida fromString(String texto) {
                return null; // a lista não é editável
            }
        });
        custo.setPromptText("0,00");
        venda.setPromptText("0,00");
        estoqueInicial.setPromptText("0");
        estoqueMinimo.setPromptText("vazio = sem controle de reposição");
        mensagem.setWrapText(true);
        mensagem.setMinHeight(36);
        mensagem.setMaxWidth(460);

        Formulario formulario = new Formulario()
                .campo("_Unidade de venda", unidade)
                .campo("_Código", codigo)
                .campo("Código de _barras", codigoBarras)
                .campo("_Nome", nome)
                .campo("Preço de _custo (R$)", custo)
                .campo("Preço de _venda (R$)", venda);
        if (novo) {
            unidade.getSelectionModel().select(UnidadeMedida.UNIDADE);
            formulario.campo("_Estoque inicial", estoqueInicial);
        } else {
            preencher(existente);
        }
        formulario.campo("Estoque _mínimo", estoqueMinimo);
        dialogo.getDialogPane().setContent(new VBox(8, formulario, mensagem));

        Node botaoSalvar = dialogo.getDialogPane().lookupButton(salvar);
        botaoSalvar.addEventFilter(ActionEvent.ACTION, evento -> {
            evento.consume(); // a janela só fecha quando o caso de uso aceitar
            salvar();
        });
        Platform.runLater(novo ? codigo::requestFocus : nome::requestFocus);
    }

    /** @return o produto gravado, ou vazio se o usuário cancelou */
    public Optional<Produto> mostrar() {
        return dialogo.showAndWait();
    }

    /**
     * @param unidade unidade de venda
     * @return texto para o operador, como "Por quilo (kg)"
     */
    static String descreverUnidade(UnidadeMedida unidade) {
        return switch (unidade) {
            case UNIDADE -> "Por unidade (un)";
            case KG -> "Por quilo (kg)";
            case METRO -> "Por metro (m)";
        };
    }

    private void preencher(Produto produto) {
        unidade.getSelectionModel().select(produto.getUnidade());
        unidade.setDisable(true);
        codigo.setText(produto.getCodigo());
        codigoBarras.setText(produto.getCodigoBarras().orElse(""));
        nome.setText(produto.getNome());
        custo.setText(Formatos.numero(produto.getPrecoCusto()));
        venda.setText(Formatos.numero(produto.getPrecoVenda()));
        estoqueMinimo.setText(produto.getEstoqueMinimo().map(Formatos::numero).orElse(""));
    }

    private void salvar() {
        Avisos.limpar(mensagem);
        if (!camposNumericosValidos()) {
            return;
        }
        BigDecimal precoCusto = Formatos.lerNumero(custo.getText()).orElseThrow();
        BigDecimal precoVenda = Formatos.lerNumero(venda.getText()).orElseThrow();
        BigDecimal inicial = Formatos.lerNumero(estoqueInicial.getText()).orElse(null);
        BigDecimal minimo = Formatos.lerNumero(estoqueMinimo.getText()).orElse(null);
        Tarefas.emSegundoPlano(dialogo.getDialogPane(), () -> {
            if (existente == null) {
                return casos.cadastrarProduto().executar(new CadastrarProduto.Comando(
                        unidade.getValue(), codigo.getText(), codigoBarras.getText(), nome.getText(),
                        precoCusto, precoVenda, inicial, minimo));
            }
            return casos.atualizarProduto().executar(new AtualizarProduto.Comando(
                    existente.getId().orElseThrow(), codigo.getText(), codigoBarras.getText(), nome.getText(),
                    precoCusto, precoVenda, minimo));
        }, produto -> {
            dialogo.setResult(produto);
            dialogo.close();
        }, erro -> Avisos.tratar(erro, mensagem));
    }

    /**
     * Confere só a digitação: os preços precisam ser números; os estoques podem ficar vazios, mas
     * se preenchidos também precisam ser números. Mostra a primeira falha e põe o cursor no campo.
     */
    private boolean camposNumericosValidos() {
        return numeroValido(custo, true, "Preencha o preço de custo com um número, por exemplo 12,50.")
                && numeroValido(venda, true, "Preencha o preço de venda com um número, por exemplo 19,90.")
                && numeroValido(estoqueInicial, false, "O estoque inicial precisa ser um número (ou ficar vazio).")
                && numeroValido(estoqueMinimo, false, "O estoque mínimo precisa ser um número (ou ficar vazio).");
    }

    private boolean numeroValido(TextField campo, boolean obrigatorio, String erro) {
        boolean vazio = campo.getText().isBlank();
        if ((vazio && !obrigatorio) || (!vazio && Formatos.lerNumero(campo.getText()).isPresent())) {
            return true;
        }
        Avisos.erro(mensagem, erro);
        campo.requestFocus();
        return false;
    }
}
