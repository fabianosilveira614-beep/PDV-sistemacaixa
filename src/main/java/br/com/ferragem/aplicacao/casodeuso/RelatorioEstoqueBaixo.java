package br.com.ferragem.aplicacao.casodeuso;

import br.com.ferragem.aplicacao.porta.ProdutoRepository;
import br.com.ferragem.dominio.modelo.produto.Produto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * Relatório "Estoque baixo": produtos ativos no estoque mínimo ou abaixo dele. O critério é o
 * mesmo que pinta a linha de vermelho na tela de Produtos: {@link Produto#precisaRepor()}.
 */
public class RelatorioEstoqueBaixo {

    /**
     * @param produto produto a repor
     * @param falta   quanto falta para voltar ao mínimo (zero se está exatamente no mínimo)
     */
    public record Linha(Produto produto, BigDecimal falta) {
    }

    private final ProdutoRepository produtos;

    /**
     * @param produtos produtos
     */
    public RelatorioEstoqueBaixo(ProdutoRepository produtos) {
        this.produtos = Objects.requireNonNull(produtos);
    }

    /** @return produtos a repor, em ordem de nome */
    public List<Linha> executar() {
        return produtos.listar("", false).stream()
                .filter(Produto::precisaRepor)
                .map(p -> new Linha(p, p.getEstoqueMinimo().orElseThrow().subtract(p.getQuantidadeEstoque())))
                .toList();
    }
}
