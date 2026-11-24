package br.com.ferragem;

import br.com.ferragem.aplicacao.casodeuso.AbrirCaixa;
import br.com.ferragem.aplicacao.casodeuso.AjustarEstoque;
import br.com.ferragem.aplicacao.casodeuso.AtualizarCliente;
import br.com.ferragem.aplicacao.casodeuso.AtualizarProduto;
import br.com.ferragem.aplicacao.casodeuso.AutenticarOperador;
import br.com.ferragem.aplicacao.casodeuso.CadastrarCliente;
import br.com.ferragem.aplicacao.casodeuso.AtualizarOperador;
import br.com.ferragem.aplicacao.casodeuso.CadastrarOperador;
import br.com.ferragem.aplicacao.casodeuso.DesativarOperador;
import br.com.ferragem.aplicacao.casodeuso.ListarOperadores;
import br.com.ferragem.aplicacao.casodeuso.CarregarOrcamento;
import br.com.ferragem.aplicacao.casodeuso.ExcluirOrcamento;
import br.com.ferragem.aplicacao.casodeuso.ListarOrcamentos;
import br.com.ferragem.aplicacao.casodeuso.SalvarOrcamento;
import br.com.ferragem.aplicacao.casodeuso.ReativarOperador;
import br.com.ferragem.aplicacao.casodeuso.RelatorioEstoqueBaixo;
import br.com.ferragem.aplicacao.casodeuso.RelatorioFiadoEmAberto;
import br.com.ferragem.aplicacao.casodeuso.RelatorioVendas;
import br.com.ferragem.aplicacao.casodeuso.RedefinirSenha;
import br.com.ferragem.aplicacao.casodeuso.CadastrarProduto;
import br.com.ferragem.aplicacao.casodeuso.ConsultarCaixaAberto;
import br.com.ferragem.aplicacao.casodeuso.ConsultarClientes;
import br.com.ferragem.aplicacao.casodeuso.ConsultarContaFiado;
import br.com.ferragem.aplicacao.casodeuso.ConsultarHistoricoCliente;
import br.com.ferragem.aplicacao.casodeuso.ConsultarProdutos;
import br.com.ferragem.aplicacao.casodeuso.ConsultarVenda;
import br.com.ferragem.aplicacao.casodeuso.ExcluirCliente;
import br.com.ferragem.aplicacao.casodeuso.ExcluirProduto;
import br.com.ferragem.aplicacao.casodeuso.FecharCaixa;
import br.com.ferragem.aplicacao.casodeuso.InativarProduto;
import br.com.ferragem.aplicacao.casodeuso.ListarClientes;
import br.com.ferragem.aplicacao.casodeuso.ListarProdutos;
import br.com.ferragem.aplicacao.casodeuso.PagarDebito;
import br.com.ferragem.aplicacao.casodeuso.RegistrarSangria;
import br.com.ferragem.aplicacao.casodeuso.RegistrarSuprimento;
import br.com.ferragem.aplicacao.casodeuso.ReativarProduto;
import br.com.ferragem.aplicacao.casodeuso.RegistrarVenda;
import br.com.ferragem.aplicacao.casodeuso.TrocarSenha;

/**
 * Todos os casos de uso do sistema, já montados com suas dependências. É o que as telas
 * (fase 4) recebem: cada tela pega só os casos de uso de que precisa e nunca conhece JDBC.
 *
 * <p>É um {@code record} porque é só um "pacote" imutável de referências, sem comportamento.</p>
 */
public record CasosDeUso(
        AutenticarOperador autenticarOperador,
        CadastrarOperador cadastrarOperador,
        TrocarSenha trocarSenha,
        ListarOperadores listarOperadores,
        AtualizarOperador atualizarOperador,
        RedefinirSenha redefinirSenha,
        DesativarOperador desativarOperador,
        ReativarOperador reativarOperador,
        ConsultarCaixaAberto consultarCaixaAberto,
        AbrirCaixa abrirCaixa,
        RegistrarSuprimento registrarSuprimento,
        RegistrarSangria registrarSangria,
        FecharCaixa fecharCaixa,
        ConsultarProdutos consultarProdutos,
        ListarProdutos listarProdutos,
        CadastrarProduto cadastrarProduto,
        AtualizarProduto atualizarProduto,
        InativarProduto inativarProduto,
        ReativarProduto reativarProduto,
        ExcluirProduto excluirProduto,
        AjustarEstoque ajustarEstoque,
        RegistrarVenda registrarVenda,
        CadastrarCliente cadastrarCliente,
        AtualizarCliente atualizarCliente,
        ExcluirCliente excluirCliente,
        ConsultarClientes consultarClientes,
        ListarClientes listarClientes,
        ConsultarHistoricoCliente consultarHistoricoCliente,
        ConsultarContaFiado consultarContaFiado,
        ConsultarVenda consultarVenda,
        PagarDebito pagarDebito,
        RelatorioVendas relatorioVendas,
        RelatorioFiadoEmAberto relatorioFiadoEmAberto,
        RelatorioEstoqueBaixo relatorioEstoqueBaixo,
        SalvarOrcamento salvarOrcamento,
        ListarOrcamentos listarOrcamentos,
        ExcluirOrcamento excluirOrcamento,
        CarregarOrcamento carregarOrcamento) {
}
