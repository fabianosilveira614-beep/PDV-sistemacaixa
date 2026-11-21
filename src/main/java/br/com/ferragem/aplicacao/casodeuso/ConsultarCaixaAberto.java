package br.com.ferragem.aplicacao.casodeuso;

import br.com.ferragem.aplicacao.porta.CaixaRepository;
import br.com.ferragem.dominio.modelo.caixa.SessaoCaixa;

import java.util.Objects;
import java.util.Optional;

/**
 * Consulta o caixa aberto no momento (saldo da gaveta, recebido por forma de pagamento).
 *
 * <p>Parece simples demais para um caso de uso, mas existe para que as telas nunca falem
 * direto com um repositório: a camada de apresentação só conhece casos de uso.</p>
 */
public class ConsultarCaixaAberto {

    private final CaixaRepository caixas;

    /**
     * @param caixas onde as sessões de caixa ficam
     */
    public ConsultarCaixaAberto(CaixaRepository caixas) {
        this.caixas = Objects.requireNonNull(caixas);
    }

    /** @return o caixa aberto, ou vazio se ninguém abriu o caixa ainda */
    public Optional<SessaoCaixa> executar() {
        return caixas.buscarAberta();
    }
}
