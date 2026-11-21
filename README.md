# Caixa Ferragem

Sistema de caixa para uma ferragem: PDV com carrinho, controle de estoque, venda fiado com quitação parcial e controle de abertura e fechamento de caixa.

Trabalho 2 da disciplina de Programação Orientada a Objetos (Java).

## Integrantes

- _nome — fatia/responsabilidade_
- _nome — fatia/responsabilidade_

## Tecnologias

| Item | Versão |
|---|---|
| Java | 21 (LTS) |
| Interface gráfica | JavaFX 21 |
| Banco de dados | PostgreSQL 16 ou superior (testado com 16 e 18) |
| Migração do esquema | Flyway |
| Build e testes | Maven, JUnit 5 |

## Como executar

### Pré-requisitos

- JDK 21 ou superior
- Maven 3.9+ (o IntelliJ IDEA já vem com Maven embutido)
- **Uma** das opções de banco abaixo

### 1. Subir o banco

**Opção A — com Docker (recomendado)**

```bash
docker compose up -d
```

Isso cria um PostgreSQL 16 com banco `caixa`, usuário `caixa` e senha `caixa`, na porta 5432. Os dados ficam no volume `dados-caixa` e sobrevivem a reinícios.

**Opção B — PostgreSQL instalado na máquina (sem Docker)**

1. Instale o PostgreSQL 16 ou superior: <https://www.postgresql.org/download/> (o instalador do Windows já traz o pgAdmin).
2. No `psql` ou no pgAdmin (Query Tool no banco `postgres`), conectado como superusuário, rode **um comando de cada vez**:

   ```sql
   CREATE USER caixa WITH PASSWORD 'caixa';
   ```
   ```sql
   CREATE DATABASE caixa OWNER caixa;
   ```

   > No pgAdmin, executar os dois juntos gera `CREATE DATABASE cannot run inside a transaction block`. Selecione uma linha por vez e aperte F5.

3. Se usar outro usuário, senha ou porta, ajuste `src/main/resources/config.properties` ou defina as variáveis de ambiente `CAIXA_DB_URL`, `CAIXA_DB_USUARIO` e `CAIXA_DB_SENHA`.

### 2. Rodar a aplicação

```bash
mvn javafx:run
```

No IntelliJ: abra a pasta do projeto (ele reconhece o `pom.xml`) e execute a classe `br.com.ferragem.Main`.

**As tabelas são criadas automaticamente.** Na inicialização, o Flyway aplica os scripts de `src/main/resources/db/migration/` que ainda não rodaram. Não é preciso executar SQL manualmente.

**Primeiro acesso:** entre com o login `admin` e a senha `admin123`, criados pela migração V2. Como essa senha é pública (está neste README), o sistema **obriga a trocá-la** antes de liberar qualquer tela. O mesmo vale para todo operador cadastrado pelo administrador: quem define a senha inicial é outra pessoa, então a troca é obrigatória no primeiro login.

**Dados de exemplo (para testar e apresentar):** com o programa já aberto uma vez (para o Flyway criar as tabelas), abra no pgAdmin o banco `caixa` → Query Tool → abra o arquivo `scripts/dados_exemplo.sql` → F5. Ele cria 8 produtos (por unidade, por kg e por metro, um deles com estoque baixo de propósito) e 2 clientes. Pode rodar de novo sem duplicar. Não é uma migração do Flyway de propósito: dado de demonstração não pode entrar sozinho no banco de uma loja de verdade.

### 3. Rodar os testes

```bash
mvn test
```

São dois tipos de teste:

- **De regra** (domínio e casos de uso): usam repositórios em memória e não precisam de banco.
- **De integração** (`infraestrutura.persistencia.jdbc`): rodam contra um PostgreSQL de verdade, num banco **separado**, `caixa_teste`, que é apagado a cada teste. Nunca aponte para o banco `caixa`.

Para criar o banco de teste, rode no pgAdmin (um comando de cada vez):

```sql
CREATE DATABASE caixa_teste OWNER caixa;
```

Sem esse banco, os testes de integração aparecem como **pulados** (skipped), não como reprovados. Outro endereço: variáveis `CAIXA_TESTE_DB_URL`, `CAIXA_TESTE_DB_USUARIO`, `CAIXA_TESTE_DB_SENHA`.

## Configuração

| Variável de ambiente | Propriedade (`config.properties`) | Padrão |
|---|---|---|
| `CAIXA_DB_URL` | `db.url` | `jdbc:postgresql://localhost:5432/caixa` |
| `CAIXA_DB_USUARIO` | `db.usuario` | `caixa` |
| `CAIXA_DB_SENHA` | `db.senha` | `caixa` |

A variável de ambiente tem prioridade sobre o arquivo.

## Arquitetura

```
br.com.ferragem
├── dominio            entidades, enums, exceções de negócio (sem dependências externas)
├── aplicacao          casos de uso e portas (interfaces de repositório e serviços)
├── infraestrutura     JDBC/PostgreSQL, Flyway, configuração, pagamentos simulados
├── apresentacao       telas JavaFX
├── ConfiguracaoAplicacao   composition root: cria e injeta as dependências
└── Main               ponto de entrada
```

As dependências apontam sempre para dentro: `apresentacao` e `infraestrutura` dependem de `aplicacao` e `dominio`, nunca o contrário. Detalhes no documento de projeto (`docs/`).

## Onde os requisitos foram atendidos

_Será preenchido conforme as fases forem concluídas (matriz de rastreabilidade, seção 12 do documento de projeto)._

## Andamento

- [x] Fase 0 — projeto Maven, banco via Docker, migração inicial, janela JavaFX
- [x] Fase 1 — domínio e testes de unidade
- [x] Fase 2 — casos de uso com repositórios em memória
- [x] Fase 3 — repositórios JDBC, transação com rollback, administrador inicial, testes de integração
- [ ] Fase 4 — telas (prontos: login, PDV com gaveta, barra de ferramentas e Produtos; faltam Clientes com fiado, Operadores, Relatórios e Orçamento)
- [ ] Fase 5 — acabamento visual, JavaDoc, capturas
