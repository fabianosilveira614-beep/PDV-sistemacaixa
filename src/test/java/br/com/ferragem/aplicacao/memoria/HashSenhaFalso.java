package br.com.ferragem.aplicacao.memoria;

import br.com.ferragem.aplicacao.porta.HashSenha;

/**
 * "Hash" trivial para testes de caso de uso: rápido e previsível. O hash de verdade
 * ({@code HashSenhaPbkdf2}) tem testes próprios.
 */
public class HashSenhaFalso implements HashSenha {

    @Override
    public String gerar(String senha) {
        return "falso:" + senha;
    }

    @Override
    public boolean confere(String senha, String hashGuardado) {
        return ("falso:" + senha).equals(hashGuardado);
    }
}
