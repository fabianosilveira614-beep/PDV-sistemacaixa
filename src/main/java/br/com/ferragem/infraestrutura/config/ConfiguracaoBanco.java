package br.com.ferragem.infraestrutura.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.Properties;

/**
 * Dados de conexão com o banco de dados (URL, usuário e senha).
 *
 * <p>Os valores vêm de duas fontes, nesta ordem de prioridade:</p>
 * <ol>
 *   <li>variáveis de ambiente {@code CAIXA_DB_URL}, {@code CAIXA_DB_USUARIO} e {@code CAIXA_DB_SENHA};</li>
 *   <li>arquivo {@code config.properties} no classpath ({@code db.url}, {@code db.usuario}, {@code db.senha}).</li>
 * </ol>
 *
 * <p>Assim, a senha real nunca precisa ficar no código-fonte: em produção basta definir as
 * variáveis de ambiente.</p>
 */
public final class ConfiguracaoBanco {

    static final String ARQUIVO_PADRAO = "config.properties";

    private final String url;
    private final String usuario;
    private final String senha;

    /**
     * Cria uma configuração já validada.
     *
     * @param url     URL JDBC do banco; não pode ser vazia
     * @param usuario usuário do banco; não pode ser vazio
     * @param senha   senha do banco; não pode ser nula (pode ser vazia)
     * @throws IllegalStateException se algum valor obrigatório estiver ausente
     */
    public ConfiguracaoBanco(String url, String usuario, String senha) {
        this.url = exigirPreenchido(url, "URL do banco (db.url / CAIXA_DB_URL)");
        this.usuario = exigirPreenchido(usuario, "usuário do banco (db.usuario / CAIXA_DB_USUARIO)");
        if (senha == null) {
            throw new IllegalStateException("Senha do banco não configurada (db.senha / CAIXA_DB_SENHA).");
        }
        this.senha = senha;
    }

    /**
     * Carrega a configuração do {@code config.properties} do classpath e das variáveis de ambiente do sistema.
     *
     * @return configuração pronta para uso
     * @throws IllegalStateException se o arquivo não existir ou faltar algum valor
     */
    public static ConfiguracaoBanco carregarPadrao() {
        return carregar(lerArquivoDoClasspath(ARQUIVO_PADRAO), System.getenv());
    }

    /**
     * Combina as propriedades do arquivo com as variáveis de ambiente; o ambiente tem prioridade.
     *
     * <p>Recebe o ambiente como parâmetro (em vez de ler {@code System.getenv()} direto) para que
     * a regra de prioridade possa ser testada sem alterar o sistema.</p>
     *
     * @param arquivo  propriedades lidas do arquivo de configuração
     * @param ambiente variáveis de ambiente
     * @return configuração resultante
     * @throws IllegalStateException se faltar algum valor obrigatório
     */
    public static ConfiguracaoBanco carregar(Properties arquivo, Map<String, String> ambiente) {
        return new ConfiguracaoBanco(
                valor(ambiente, "CAIXA_DB_URL", arquivo, "db.url"),
                valor(ambiente, "CAIXA_DB_USUARIO", arquivo, "db.usuario"),
                valor(ambiente, "CAIXA_DB_SENHA", arquivo, "db.senha"));
    }

    /** @return URL JDBC do banco */
    public String getUrl() {
        return url;
    }

    /** @return usuário do banco */
    public String getUsuario() {
        return usuario;
    }

    /** @return senha do banco */
    public String getSenha() {
        return senha;
    }

    /**
     * Representação textual sem a senha, segura para aparecer em logs.
     *
     * @return URL e usuário
     */
    @Override
    public String toString() {
        return "ConfiguracaoBanco{url='" + url + "', usuario='" + usuario + "', senha='***'}";
    }

    private static String valor(Map<String, String> ambiente, String chaveAmbiente,
                                Properties arquivo, String chaveArquivo) {
        String doAmbiente = ambiente.get(chaveAmbiente);
        if (doAmbiente != null && !doAmbiente.isBlank()) {
            return doAmbiente;
        }
        return arquivo.getProperty(chaveArquivo);
    }

    private static String exigirPreenchido(String valor, String descricao) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalStateException("Configuração ausente: " + descricao + ".");
        }
        return valor.trim();
    }

    private static Properties lerArquivoDoClasspath(String nomeArquivo) {
        try (InputStream entrada = ConfiguracaoBanco.class.getClassLoader().getResourceAsStream(nomeArquivo)) {
            if (entrada == null) {
                throw new IllegalStateException("Arquivo " + nomeArquivo + " não encontrado no classpath.");
            }
            Properties propriedades = new Properties();
            propriedades.load(entrada);
            return propriedades;
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao ler " + nomeArquivo, e);
        }
    }
}
