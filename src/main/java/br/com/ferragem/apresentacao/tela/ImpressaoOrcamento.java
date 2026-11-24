package br.com.ferragem.apresentacao.tela;

import br.com.ferragem.apresentacao.comum.Formatos;
import br.com.ferragem.dominio.modelo.cliente.Cliente;
import br.com.ferragem.dominio.modelo.orcamento.Orcamento;
import javafx.geometry.Insets;
import javafx.print.PageLayout;
import javafx.print.PrinterJob;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.transform.Scale;
import javafx.stage.Window;

/**
 * Impressão do orçamento em formato de cupom, pela caixa de impressão do sistema. Para testar sem
 * impressora, escolha "Microsoft Print to PDF".
 *
 * <p>Fica na apresentação de propósito: imprimir é detalhe de tela. Os números (subtotais e total)
 * vêm prontos do {@link Orcamento}; aqui só se monta o papel.</p>
 */
final class ImpressaoOrcamento {

    /** Nome no topo do cupom. */
    static final String NOME_LOJA = "Caixa Ferragem";

    /** Largura do cupom em pontos (cerca de 10 cm). */
    private static final double LARGURA = 290;

    /** O que aconteceu com a impressão. */
    enum Resultado {
        /** Enviado para a impressora. */
        IMPRESSO,
        /** O operador fechou a caixa de impressão. */
        CANCELADO,
        /** Não há impressora instalada. */
        SEM_IMPRESSORA,
        /** A impressora recusou o trabalho. */
        FALHOU
    }

    private ImpressaoOrcamento() {
    }

    /**
     * @param orcamento orçamento já gravado (com número)
     * @param dono      janela sobre a qual a caixa de impressão abre
     * @return o que aconteceu
     */
    static Resultado imprimir(Orcamento orcamento, Window dono) {
        PrinterJob trabalho = PrinterJob.createPrinterJob();
        if (trabalho == null) {
            return Resultado.SEM_IMPRESSORA;
        }
        if (!trabalho.showPrintDialog(dono)) {
            trabalho.cancelJob();
            return Resultado.CANCELADO;
        }
        VBox cupom = montarCupom(orcamento);
        new Scene(new Group(cupom)); // a cena calcula o tamanho do cupom antes de imprimir
        cupom.applyCss();
        cupom.layout();
        PageLayout pagina = trabalho.getJobSettings().getPageLayout();
        double escala = Math.min(1, pagina.getPrintableWidth() / cupom.getBoundsInParent().getWidth());
        if (escala < 1) {
            cupom.getTransforms().add(new Scale(escala, escala));
        }
        if (!trabalho.printPage(pagina, cupom)) {
            trabalho.cancelJob();
            return Resultado.FALHOU;
        }
        return trabalho.endJob() ? Resultado.IMPRESSO : Resultado.FALHOU;
    }

    /**
     * @param resultado o que aconteceu
     * @return explicação para o operador quando não imprimiu (vazio se imprimiu)
     */
    static String descreverProblema(Resultado resultado) {
        return switch (resultado) {
            case IMPRESSO -> "";
            case CANCELADO -> "Impressão cancelada.";
            case SEM_IMPRESSORA -> "Nenhuma impressora encontrada: instale uma ou use \"Microsoft Print to PDF\".";
            case FALHOU -> "A impressora recusou o trabalho.";
        };
    }

    private static VBox montarCupom(Orcamento orcamento) {
        VBox cupom = new VBox(2);
        cupom.setPadding(new Insets(8));
        cupom.setPrefWidth(LARGURA);
        cupom.setStyle("-fx-background-color: white;");
        cupom.getChildren().addAll(
                linha(NOME_LOJA, true),
                linha("ORÇAMENTO Nº " + orcamento.getId().map(String::valueOf).orElse("?"), true),
                linha("*** SEM VALOR FISCAL ***", true),
                linha(Formatos.dataHora(orcamento.getCriadoEm()) + "   Atendente: " + orcamento.getAtendente(), false));
        orcamento.getCliente().map(Cliente::getNome).ifPresent(nome -> cupom.getChildren().add(linha("Cliente: " + nome, false)));
        cupom.getChildren().add(separador());
        for (Orcamento.Item item : orcamento.getItens()) {
            cupom.getChildren().add(linha(item.produto().getNome(), false));
            cupom.getChildren().add(linha("   " + Formatos.quantidade(item.quantidade(), item.produto().getUnidade())
                    + " × " + Formatos.moeda(item.precoUnitario()) + " = " + Formatos.moeda(item.calcularSubtotal()), false));
        }
        cupom.getChildren().addAll(separador(),
                linha("TOTAL  " + Formatos.moeda(orcamento.calcularTotal()), true),
                separador(),
                linha("Preços sujeitos a alteração.", false),
                linha("Este documento não é cupom fiscal.", false));
        return cupom;
    }

    private static Label linha(String texto, boolean destaque) {
        Label rotulo = new Label(texto);
        rotulo.setWrapText(true);
        rotulo.setMaxWidth(LARGURA - 16);
        rotulo.setStyle("-fx-font-family: 'Consolas', 'Courier New', monospace; -fx-text-fill: black; -fx-font-size: "
                + (destaque ? "12px; -fx-font-weight: bold;" : "10px;"));
        return rotulo;
    }

    private static Label separador() {
        return linha("-".repeat(44), false);
    }
}
