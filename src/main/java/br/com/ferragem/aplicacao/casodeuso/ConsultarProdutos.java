package br.com.ferragem.aplicacao.casodeuso;

import br.com.ferragem.aplicacao.porta.ConsultaProdutos;
import br.com.ferragem.dominio.modelo.produto.Produto;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Busca do PDV (tecla F3) e da tela de estoque: um único campo que aceita código de barras,
 * código interno ou parte do nome.
 *
 * <p>Depende só de {@link ConsultaProdutos}, a interface de leitura: esta classe não tem como
 * alterar produtos (segregação de interfaces).</p>
 */
public class ConsultarProdutos {

    private final ConsultaProdutos consulta;

    /**
     * @param consulta leitura de produtos
     */
    public ConsultarProdutos(ConsultaProdutos consulta) {
        this.consulta = Objects.requireNonNull(consulta);
    }

    /**
     * Procura primeiro um código de barras exato, depois um código interno exato e, por fim, o
     * trecho no nome. Sem termo, lista todos os produtos ativos (a janela de busca do PDV abre
     * já mostrando o catálogo).
     *
     * @param termo texto digitado ou lido pelo leitor; vazio ou nulo lista todos
     * @return produtos ativos encontrados, em ordem de nome (inativos nunca aparecem: não podem ser vendidos)
     */
    public List<Produto> executar(String termo) {
        if (termo == null || termo.isBlank()) {
            return consulta.buscarPorNome("");
        }
        String procurado = termo.trim();
        Optional<Produto> exato = consulta.buscarPorCodigoBarras(procurado)
                .or(() -> consulta.buscarPorCodigo(procurado));
        return exato.map(List::of).orElseGet(() -> consulta.buscarPorNome(procurado));
    }
}
