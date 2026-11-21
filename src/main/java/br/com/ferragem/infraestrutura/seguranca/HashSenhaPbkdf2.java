package br.com.ferragem.infraestrutura.seguranca;

import br.com.ferragem.aplicacao.porta.HashSenha;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Objects;

/**
 * Hash de senha com PBKDF2-HMAC-SHA256, que já vem no JDK (sem biblioteca extra).
 *
 * <p>Como funciona:</p>
 * <ul>
 *   <li><b>Sal:</b> cada senha recebe 16 bytes aleatórios antes do cálculo. Duas pessoas com a mesma
 *       senha ficam com hashes diferentes, e tabelas prontas de senhas ("rainbow tables") não servem.</li>
 *   <li><b>Iterações:</b> o cálculo é repetido muitas vezes para ficar lento de propósito: imperceptível
 *       num login, mas caríssimo para quem tenta adivinhar milhões de senhas.</li>
 *   <li><b>Formato guardado:</b> {@code pbkdf2-sha256$iteracoes$sal$hash} (sal e hash em Base64), para
 *       conferir depois com os mesmos parâmetros.</li>
 * </ul>
 */
public class HashSenhaPbkdf2 implements HashSenha {

    /** Quantidade de iterações recomendada para PBKDF2-HMAC-SHA256 (referência: OWASP). */
    public static final int ITERACOES_PADRAO = 600_000;

    private static final String ALGORITMO = "PBKDF2WithHmacSHA256";
    private static final String PREFIXO = "pbkdf2-sha256";
    private static final int TAMANHO_SAL_BYTES = 16;
    private static final int TAMANHO_HASH_BITS = 256;

    private final int iteracoes;
    private final SecureRandom aleatorio = new SecureRandom();

    /** Usa {@value #ITERACOES_PADRAO} iterações. */
    public HashSenhaPbkdf2() {
        this(ITERACOES_PADRAO);
    }

    /**
     * @param iteracoes quantidade de iterações (menor só em testes, para rodarem rápido)
     * @throws IllegalArgumentException se não for positiva
     */
    public HashSenhaPbkdf2(int iteracoes) {
        if (iteracoes <= 0) {
            throw new IllegalArgumentException("Iterações devem ser positivas: " + iteracoes);
        }
        this.iteracoes = iteracoes;
    }

    @Override
    public String gerar(String senha) {
        Objects.requireNonNull(senha, "senha");
        byte[] sal = new byte[TAMANHO_SAL_BYTES];
        aleatorio.nextBytes(sal);
        byte[] hash = derivar(senha, sal, iteracoes);
        Base64.Encoder base64 = Base64.getEncoder();
        return PREFIXO + "$" + iteracoes + "$" + base64.encodeToString(sal) + "$" + base64.encodeToString(hash);
    }

    @Override
    public boolean confere(String senha, String hashGuardado) {
        if (senha == null || hashGuardado == null) {
            return false;
        }
        String[] partes = hashGuardado.split("\\$");
        if (partes.length != 4 || !PREFIXO.equals(partes[0])) {
            return false;
        }
        try {
            int iteracoesGuardadas = Integer.parseInt(partes[1]);
            byte[] sal = Base64.getDecoder().decode(partes[2]);
            byte[] esperado = Base64.getDecoder().decode(partes[3]);
            // comparação em tempo constante: não revela, pelo tempo de resposta, quantos bytes acertaram
            return MessageDigest.isEqual(esperado, derivar(senha, sal, iteracoesGuardadas));
        } catch (IllegalArgumentException formatoInvalido) {
            return false;
        }
    }

    private static byte[] derivar(String senha, byte[] sal, int iteracoes) {
        PBEKeySpec especificacao = new PBEKeySpec(senha.toCharArray(), sal, iteracoes, TAMANHO_HASH_BITS);
        try {
            return SecretKeyFactory.getInstance(ALGORITMO).generateSecret(especificacao).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Algoritmo " + ALGORITMO + " indisponível nesta JVM.", e);
        } finally {
            especificacao.clearPassword();
        }
    }
}
