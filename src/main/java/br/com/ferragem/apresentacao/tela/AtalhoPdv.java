package br.com.ferragem.apresentacao.tela;

import javafx.scene.input.KeyCode;

import java.util.Arrays;
import java.util.Optional;

/**
 * Atalhos de teclado do PDV. A tecla, o nome mostrado na legenda do rodapé e a ação ficam
 * amarrados aqui: <b>se amanhã o pagamento passar para F10</b>, muda só a constante
 * {@link #PAGAR}, e a legenda e a ajuda (F1) acompanham sozinhas.
 *
 * <p>Teclas que só valem dentro de diálogos (1–5 no pagamento, Enter, Esc) não estão aqui
 * porque não são atalhos da tela do carrinho.</p>
 */
public enum AtalhoPdv {

    AJUDA(KeyCode.F1, "F1", "Ajuda", "Mostra esta lista de atalhos"),
    QUANTIDADE_PROXIMA(KeyCode.F2, "F2", "Qtd.", "Informa a quantidade antes de passar o próximo produto"),
    BUSCAR_PRODUTO(KeyCode.F3, "F3", "Buscar", "Busca produto pelo nome ou pelo código"),
    CLIENTE(KeyCode.F4, "F4", "Cliente", "Identifica o cliente (obrigatório para fiado)"),
    PRECO_ITEM(KeyCode.F5, "F5", "Preço", "Altera o preço do item selecionado (desconto)"),
    QUANTIDADE_ITEM(KeyCode.F6, "F6", "Qtd. item", "Altera a quantidade do item selecionado"),
    REMOVER_ITEM(KeyCode.DELETE, "Del", "Remover", "Remove o item selecionado (com o campo de código vazio)"),
    CANCELAR_VENDA(KeyCode.F8, "F8", "Cancelar", "Cancela a venda inteira"),
    PAGAR(KeyCode.F9, "F9", "Pagamento", "Vai para o pagamento"),
    GAVETA(KeyCode.F10, "F10", "Gaveta", "Dinheiro na gaveta, suprimento, sangria e fechamento do caixa");

    private final KeyCode tecla;
    private final String rotuloTecla;
    private final String nomeCurto;
    private final String descricao;

    AtalhoPdv(KeyCode tecla, String rotuloTecla, String nomeCurto, String descricao) {
        this.tecla = tecla;
        this.rotuloTecla = rotuloTecla;
        this.nomeCurto = nomeCurto;
        this.descricao = descricao;
    }

    /**
     * @param tecla tecla pressionada
     * @return o atalho correspondente, se houver
     */
    public static Optional<AtalhoPdv> daTecla(KeyCode tecla) {
        return Arrays.stream(values()).filter(atalho -> atalho.tecla == tecla).findFirst();
    }

    /** @return texto da tecla, como "F9" */
    public String getRotuloTecla() {
        return rotuloTecla;
    }

    /** @return nome curto para a barra do rodapé */
    public String getNomeCurto() {
        return nomeCurto;
    }

    /** @return explicação para a ajuda (F1) */
    public String getDescricao() {
        return descricao;
    }
}
