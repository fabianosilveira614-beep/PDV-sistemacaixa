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
     * trecho no nome.
     *
     * @param termo texto digitado ou lido pelo leitor
     * @return produtos ativos encontrados; vazio se o termo estiver em branco
     */
    public List<Produto> executar(String termo) {
        if (termo == null || termo.isBlank()) {
            return List.of();
        }
        String procurado = termo.trim();
        Optional<Produto> exato = consulta.buscarPorCodigoBarras(procurado)
                .or(() -> consulta.buscarPorCodigo(procurado));
        return exato.map(List::of).orElseGet(() -> consulta.buscarPorNome(procurado));
    }
}
