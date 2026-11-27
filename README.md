# Nova Ferragem — Sistema de Ponto de Venda (PDV)

Sistema de Ponto de Venda simples, feito para as ações diárias de lojas de pequeno a médio porte. Permite adicionar e administrar operadores, cadastrar produtos, lançar vendas, registrar clientes e realizar as operações gerais de um comércio. Conta com históricos e relatórios já implementados no programa.

## Sobre

Trabalho 2 da disciplina de Programação Orientada a Objetos (Java), orientado pelo professor Silvio Ereno Quincozes, com o objetivo de potencializar os aprendizados da disciplina.

## Integrantes

| Nome | Responsabilidade |
|---|---|
| Gustavo Almeida | Programação / Codificação |
| Fabiano Martins | Documentação / Polimento |

## Funcionalidades

- **PDV:** carrinho, busca de produtos e de clientes, cadastro rápido de cliente durante a venda.
- **Pagamentos:** dinheiro, cartão, Pix ou fiado.
- **Produtos e estoque:** venda por unidade, kg ou metro; ajuste de estoque; alerta de estoque baixo.
- **Clientes e fiado:** perfil do cliente com histórico de compras, conta de fiado e recebimento parcial ou total do pagamento.
- **Orçamentos:** salvar, imprimir, excluir e carregar no carrinho. O orçamento não mexe no estoque, pois serve para simular uma venda.
- **Relatórios:** vendas do dia ou de um período escolhido, compras em fiado ainda em aberto, informações de lucro e valores vendidos.
- **Operadores:** perfis de administrador e operador, senha provisória com troca obrigatória por uma senha pessoal no primeiro acesso e função para desativar operador.

## Tecnologias

- Java 21 (LTS)
- JavaFX 21
- PostgreSQL 16 ou 18
- Flyway
- Maven e JUnit 5

## Como executar

### Pré-requisitos

- JDK 21 ou superior
- Maven 3.9 ou superior
- Banco de dados PostgreSQL, por uma das opções abaixo

### 1. Banco de dados

**Docker **

```bash
docker compose up -d
```

Isso cria um PostgreSQL 16 com banco `caixa`, usuário `caixa` e senha `caixa`, na porta 5432. Os dados ficam no volume `dados-caixa` e mantêm-se mesmo após reinícios.

**PostgreSQL**

1. Instale o PostgreSQL 16 ou superior: <https://www.postgresql.org/download/>
2. Crie um Database com o nome exatamente igual ao de config.properties:
   ![pgAdmin conectado](docs/img/CreateDB.png)
   ![config.properties](docs/img/SenhaDB.png)
2. No pgAdmin (Query Tool), rode um comando de cada vez:

   ```sql
   CREATE USER caixa WITH PASSWORD 'caixa';
   ```

   ```sql
   CREATE DATABASE caixa OWNER caixa;
   ```

> [!NOTE]
> Executar os dois comandos juntos gera o erro `CREATE DATABASE cannot run inside a transaction block`.

Se for usar outro usuário, senha ou porta, ajuste o arquivo `src/main/resources/config.properties` ou defina as variáveis de ambiente `CAIXA_DB_URL`, `CAIXA_DB_USUARIO` e `CAIXA_DB_SENHA`.

### 2. Rodar a aplicação

```bash
mvn javafx:run
```

No IntelliJ: abra a pasta do projeto (o programa reconhece o `pom.xml`) e execute a classe `br.com.ferragem.Main`.

As tabelas são criadas automaticamente. Na inicialização, o Flyway aplica os "scripts" de `src/main/resources/db/migration/` que ainda não rodaram, então não precisa executar SQL manualmente.

**Primeiro acesso:** entre com o "login" `admin` e a senha `admin123`, criados pela migração V2. A senha é pública, o sistema obriga a trocar antes de liberar as outras telas.

### 3. Rodar os testes

```bash
mvn test
```

São dois tipos de teste:

- **Testes de regra** (domínio e casos de uso): usam repositórios em memória e não precisam do banco.
- **Testes de integração** (`infraestrutura.persistencia.jdbc`): rodam com um PostgreSQL, num banco separado (`caixa_teste`) que é apagado após cada teste.

Para criar o banco de teste, rode no pgAdmin:

```sql
CREATE DATABASE caixa_teste OWNER caixa;
```

Se esse banco não for criado, os testes de integração aparecem como *skipped* (pulados), não como reprovados.

## Arquitetura

```
br.com.ferragem
├── dominio                 entidades, enums e exceções de negócio
├── aplicacao               casos de uso e portas
├── infraestrutura          JDBC/PostgreSQL, Flyway, configuração, pagamentos simulados
├── apresentacao            telas JavaFX
├── ConfiguracaoAplicacao   composition root: cria e injeta as dependências
└── Main                    ponto de entrada do programa
```
