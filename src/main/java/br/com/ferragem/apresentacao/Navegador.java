package br.com.ferragem.apresentacao;

import br.com.ferragem.CasosDeUso;
import br.com.ferragem.apresentacao.comum.Formatos;
import br.com.ferragem.apresentacao.comum.Icones;
import br.com.ferragem.apresentacao.comum.Sessao;
import br.com.ferragem.apresentacao.comum.Tarefas;
import br.com.ferragem.apresentacao.tela.TelaClientes;
import br.com.ferragem.apresentacao.tela.TelaLogin;
import br.com.ferragem.apresentacao.tela.TelaOperadores;
import br.com.ferragem.apresentacao.tela.TelaPdv;
import br.com.ferragem.apresentacao.tela.TelaPerfilCliente;
import br.com.ferragem.apresentacao.tela.TelaProdutos;
import br.com.ferragem.apresentacao.tela.TelaRelatorios;
import br.com.ferragem.apresentacao.tela.TelaTrocaSenha;
import br.com.ferragem.dominio.modelo.caixa.Operador;
import br.com.ferragem.dominio.modelo.cliente.Cliente;
import br.com.ferragem.dominio.modelo.caixa.SessaoCaixa;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Decide qual tela aparece e monta a moldura comum: barra de ferramentas com as áreas do
 * sistema, menu do usuário e faixa com a situação do caixa.
 *
 * <p>As telas não criam umas às outras: pedem ao navegador ("terminei o login, vá para o caixa").
 * Assim cada tela conhece só os casos de uso que usa e o fluxo entre telas fica num lugar só.
 * <b>Se amanhã o fluxo mudar</b> (por exemplo, abrir em Produtos depois do login), muda só
 * esta classe.</p>
 */
public final class Navegador {

    /** Áreas da barra de ferramentas, na ordem em que aparecem. */
    public enum Area {
        CAIXA("Caixa", Icones::caixa),
        PRODUTOS("Produtos", Icones::produtos),
        CLIENTES("Clientes", Icones::clientes),
        RELATORIOS("Relatórios", Icones::relatorios),
        OPERADORES("Operadores", Icones::operadores);

        private final String titulo;
        private final Supplier<Node> icone;

        Area(String titulo, Supplier<Node> icone) {
            this.titulo = titulo;
            this.icone = icone;
        }
    }

    private final CasosDeUso casos;
    private final Sessao sessao = new Sessao();
    private final BorderPane moldura = new BorderPane();
    private final HBox areasDaBarra = new HBox(2);
    private final Map<Area, Button> botoesArea = new EnumMap<>(Area.class);
    private final MenuButton menuUsuario = new MenuButton();
    private final Label situacaoCaixa = new Label();
    private final HBox faixaSituacao = new HBox(16);
    /** PDV guardado durante a sessão: ir a Produtos e voltar não perde o carrinho. */
    private TelaPdv pdv;

    /**
     * @param casos casos de uso do sistema
     */
    public Navegador(CasosDeUso casos) {
        this.casos = Objects.requireNonNull(casos);
        moldura.setTop(new VBox(criarBarra(), criarFaixaSituacao()));
    }

    /** @return o nó raiz da janela */
    public Parent raiz() {
        return moldura;
    }

    /**
     * Troca o conteúdo central.
     *
     * @param conteudo tela a mostrar
     */
    public void mostrar(Node conteudo) {
        moldura.setCenter(conteudo);
    }

    // ------------------------------------------------------------------ fluxo entre telas

    /** Tela de login, sem barra de áreas nem menu do usuário. */
    public void irParaLogin() {
        sessao.sair();
        pdv = null; // o carrinho em andamento é de quem estava logado
        mostrarMoldura(false);
        mostrar(new TelaLogin(casos.autenticarOperador(), this).raiz());
    }

    /**
     * Chamado pela tela de login.
     *
     * @param operador quem acabou de se autenticar
     */
    public void aoEntrar(Operador operador) {
        sessao.entrar(operador);
        menuUsuario.setText(operador.getNome());
        botoesArea.get(Area.OPERADORES).setVisible(operador.ehAdministrador());
        botoesArea.get(Area.OPERADORES).setManaged(operador.ehAdministrador());
        irParaCaixa();
    }

    /**
     * Caixa: o PDV. Com o caixa fechado, a própria tela pede o troco e abre o caixa; a gaveta
     * (suprimento, sangria, fechamento) fica no F1. O carrinho em andamento é mantido entre idas
     * e vindas a outras áreas.
     */
    public void irParaCaixa() {
        if (exigirSenhaPropria()) {
            return;
        }
        if (pdv == null) {
            pdv = new TelaPdv(casos, sessao, this);
        }
        entrarNaArea(Area.CAIXA, pdv.raiz());
        pdv.aoMostrar();
    }

    /** Cadastro de produtos: lista, novo, edição, ajuste de estoque, inativação. */
    public void irParaProdutos() {
        if (exigirSenhaPropria()) {
            return;
        }
        entrarNaArea(Area.PRODUTOS, new TelaProdutos(casos).raiz());
    }

    /** Lista de clientes: busca, cadastro, exclusão e entrada para o perfil. */
    public void irParaClientes() {
        if (exigirSenhaPropria()) {
            return;
        }
        entrarNaArea(Area.CLIENTES, new TelaClientes(casos, this).raiz());
    }

    /**
     * Perfil de um cliente (dados, conta de fiado, compras). Fica dentro da área Clientes:
     * Esc ou o botão "← Clientes" voltam para a lista.
     *
     * @param cliente cliente escolhido na lista
     * @param aba     aba que abre selecionada
     */
    public void abrirPerfilCliente(Cliente cliente, TelaPerfilCliente.Aba aba) {
        if (exigirSenhaPropria()) {
            return;
        }
        entrarNaArea(Area.CLIENTES, new TelaPerfilCliente(casos, this, cliente, aba).raiz());
    }

    /** Relatórios: vendas do período, fiado em aberto e estoque baixo. */
    public void irParaRelatorios() {
        if (exigirSenhaPropria()) {
            return;
        }
        entrarNaArea(Area.RELATORIOS, new TelaRelatorios(casos, this).raiz());
    }

    /**
     * Operadores: só administrador. O botão nem aparece para os outros, e os casos de uso
     * conferem de novo (a tela não é a única barreira).
     */
    public void irParaOperadores() {
        if (exigirSenhaPropria() || !sessao.operador().ehAdministrador()) {
            return;
        }
        entrarNaArea(Area.OPERADORES, new TelaOperadores(casos, sessao, this).raiz());
    }

    /**
     * O administrador corrigiu o próprio nome na tela de operadores: atualiza a sessão e o menu.
     *
     * @param atualizado o operador logado, já gravado
     */
    public void aoAlterarOperadorLogado(Operador atualizado) {
        sessao.entrar(atualizado);
        menuUsuario.setText(atualizado.getNome());
    }

    /** Troca de senha voluntária (menu do usuário). */
    public void irParaTrocaSenha() {
        if (!exigirSenhaPropria()) {
            marcarAreaAtiva(null);
            mostrar(new TelaTrocaSenha(casos.trocarSenha(), sessao, this, false).raiz());
        }
    }

    /**
     * Atualiza a faixa "Caixa aberto por… · Gaveta R$…". As telas chamam depois de qualquer
     * operação que mexe no caixa (abrir, vender, sangria, fechar).
     */
    public void atualizarSituacaoCaixa() {
        Tarefas.emSegundoPlano(null, casos.consultarCaixaAberto()::executar, this::exibirSituacao,
                erro -> situacaoCaixa.setText("Situação do caixa indisponível: " + erro.getMessage()));
    }

    // ------------------------------------------------------------------ apoio ao fluxo

    /**
     * Se o operador ainda usa uma senha definida por outra pessoa, mostra a troca obrigatória
     * (sem barra de áreas e sem "Cancelar") no lugar da tela pedida. Toda navegação para dentro
     * do sistema passa por aqui: não há botão nem atalho que contorne a troca.
     *
     * @return {@code true} se a troca obrigatória foi mostrada
     */
    private boolean exigirSenhaPropria() {
        if (!sessao.operador().deveTrocarSenha()) {
            return false;
        }
        mostrarMoldura(false);
        menuUsuario.setVisible(true); // "Sair" continua disponível
        mostrar(new TelaTrocaSenha(casos.trocarSenha(), sessao, this, true).raiz());
        return true;
    }

    private void entrarNaArea(Area area, Node conteudo) {
        mostrarMoldura(true);
        marcarAreaAtiva(area);
        mostrar(conteudo);
        atualizarSituacaoCaixa();
    }

    private void mostrarMoldura(boolean visivel) {
        areasDaBarra.setVisible(visivel);
        menuUsuario.setVisible(visivel);
        faixaSituacao.setVisible(visivel);
        faixaSituacao.setManaged(visivel);
    }

    private void marcarAreaAtiva(Area ativa) {
        botoesArea.forEach((area, botao) -> {
            botao.getStyleClass().remove("area-ativa");
            if (area == ativa) {
                botao.getStyleClass().add("area-ativa");
            }
        });
    }

    private void exibirSituacao(Optional<SessaoCaixa> caixa) {
        situacaoCaixa.getStyleClass().removeAll("caixa-aberto", "caixa-fechado");
        if (caixa.isPresent()) {
            SessaoCaixa aberto = caixa.get();
            situacaoCaixa.getStyleClass().add("caixa-aberto");
            situacaoCaixa.setText("Caixa aberto por " + aberto.getOperador().getNome() + " em "
                    + Formatos.dataHora(aberto.getAbertoEm()) + "   ·   Gaveta: "
                    + Formatos.moeda(aberto.calcularSaldoEsperado()));
        } else {
            situacaoCaixa.getStyleClass().add("caixa-fechado");
            situacaoCaixa.setText("Caixa fechado");
        }
    }

    // ------------------------------------------------------------------ moldura

    private HBox criarBarra() {
        Label titulo = new Label("Nova Ferragem");
        titulo.getStyleClass().add("titulo-app");
        titulo.setPadding(new Insets(0, 16, 0, 0));

        for (Area area : Area.values()) {
            Button botao = new Button(area.titulo, area.icone.get());
            botao.setContentDisplay(ContentDisplay.TOP);
            botao.getStyleClass().add("botao-area");
            botao.setFocusTraversable(false);
            botao.setOnAction(evento -> irPara(area));
            botoesArea.put(area, botao);
            areasDaBarra.getChildren().add(botao);
        }

        MenuItem trocarSenha = new MenuItem("Trocar senha");
        trocarSenha.setOnAction(evento -> irParaTrocaSenha());
        MenuItem sair = new MenuItem("Sair");
        sair.setOnAction(evento -> irParaLogin());
        menuUsuario.getItems().addAll(trocarSenha, sair);
        menuUsuario.setGraphic(Icones.usuario());
        menuUsuario.getStyleClass().add("menu-usuario");
        menuUsuario.setFocusTraversable(false);

        Region espaco = new Region();
        HBox.setHgrow(espaco, Priority.ALWAYS);
        HBox barra = new HBox(8, titulo, areasDaBarra, espaco, menuUsuario);
        barra.getStyleClass().add("cabecalho");
        barra.setAlignment(Pos.CENTER_LEFT);
        barra.setPadding(new Insets(4, 16, 4, 16));
        barra.setMinHeight(64);
        mostrarMoldura(false);
        return barra;
    }

    private HBox criarFaixaSituacao() {
        faixaSituacao.getChildren().add(situacaoCaixa);
        faixaSituacao.getStyleClass().add("faixa-situacao");
        faixaSituacao.setPadding(new Insets(6, 16, 6, 16));
        faixaSituacao.setAlignment(Pos.CENTER_LEFT);
        return faixaSituacao;
    }

    private void irPara(Area area) {
        switch (area) {
            case CAIXA -> irParaCaixa();
            case PRODUTOS -> irParaProdutos();
            case CLIENTES -> irParaClientes();
            case RELATORIOS -> irParaRelatorios();
            case OPERADORES -> irParaOperadores();
        }
    }
}
