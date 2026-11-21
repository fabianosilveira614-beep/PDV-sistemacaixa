package br.com.ferragem.aplicacao.memoria;

import br.com.ferragem.aplicacao.porta.ProdutoRepository;
import br.com.ferragem.dominio.modelo.produto.Produto;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/** Repositório de produtos em memória, só para testes. */
public class ProdutoRepositoryEmMemoria implements ProdutoRepository {

    private final List<Produto> produtos = new ArrayList<>();
    private long proximoId = 1;
    private int gravacoes;

    @Override
    public void salvar(Produto produto) {
        gravacoes++;
        if (produto.getId().isEmpty()) {
            produto.atribuirId(proximoId++);
            produtos.add(produto);
        }
    }

    @Override
    public boolean existeCodigo(String codigo, Long ignorarId) {
        return produtos.stream().anyMatch(p -> p.getCodigo().equals(codigo)
                && !Objects.equals(p.getId().orElse(null), ignorarId));
    }

    @Override
    public boolean existeCodigoBarras(String codigoBarras, Long ignorarId) {
        return produtos.stream().anyMatch(p -> p.getCodigoBarras().filter(codigoBarras::equals).isPresent()
                && !Objects.equals(p.getId().orElse(null), ignorarId));
    }

    @Override
    public Optional<Produto> buscarPorId(long id) {
        return produtos.stream().filter(p -> p.getId().orElse(-1L) == id).findFirst();
    }

    @Override
    public Optional<Produto> buscarPorCodigo(String codigo) {
        return produtos.stream().filter(Produto::estaAtivo).filter(p -> p.getCodigo().equals(codigo)).findFirst();
    }

    @Override
    public Optional<Produto> buscarPorCodigoBarras(String codigoBarras) {
        return produtos.stream().filter(Produto::estaAtivo)
                .filter(p -> p.getCodigoBarras().filter(codigoBarras::equals).isPresent()).findFirst();
    }

    @Override
    public List<Produto> buscarPorNome(String trecho) {
        String procurado = trecho.toLowerCase(Locale.ROOT);
        return produtos.stream().filter(Produto::estaAtivo)
                .filter(p -> p.getNome().toLowerCase(Locale.ROOT).contains(procurado))
                .sorted(Comparator.comparing(Produto::getNome)).toList();
    }

    @Override
    public List<Produto> listar(String trecho, boolean incluirInativos) {
        String procurado = trecho.toLowerCase(Locale.ROOT);
        return produtos.stream().filter(p -> incluirInativos || p.estaAtivo())
                .filter(p -> p.getNome().toLowerCase(Locale.ROOT).contains(procurado)
                        || p.getCodigo().toLowerCase(Locale.ROOT).contains(procurado))
                .sorted(Comparator.comparing(Produto::getNome)).toList();
    }

    @Override
    public void excluir(Produto produto) {
        produtos.remove(produto);
    }

    /** @return quantas vezes {@link #salvar} foi chamado (para conferir que nada foi gravado) */
    public int getGravacoes() {
        return gravacoes;
    }
}
