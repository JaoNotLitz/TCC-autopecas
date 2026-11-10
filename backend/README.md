# Backend — WC Autopeças

Microsserviços Spring Boot 3.3 / Java 21, um módulo Maven por serviço. A arquitetura está em
[`ARQUITETURA-BACKEND-DETALHADA.md`](../ARQUITETURA-BACKEND-DETALHADA.md) e o plano em [`backlog/`](../backlog/README.md).
O contrato da API é o do front React (`dinastiadoge/tcc`): ids numéricos, listas em array sem paginação, sem refresh token.

| Módulo | Porta | Épico | Situação |
|---|---|---|---|
| `wc-commons` | — | EPB-00 | pronto |
| `auth-service` | 8080 | EPB-01 | pronto (refresh token fora do escopo) |
| `customer-service` | 8081 | EPB-03 | pronto (CRUD, busca, eventos e estatísticas de compras) |
| `product-service` | 8082 | EPB-04 e EPB-05 | pronto (catálogo, resumo, ajustes, listener de estoque e alertas) |
| `wc-jwt-authorizer` | Lambda | EPB-02 | pronto (o gateway e as rotas estão em `infra/local/init/ready.d/50-gateway.sh`) |

## Rodar

```bash
cd backend
mvn verify            # unitários (*Test) + integração (*IT, Testcontainers: precisa de Docker)
mvn -DskipITs verify  # só unitários
```

Para subir a stack inteira, veja [`infra/local/README.md`](../infra/local/README.md) (`make up` na raiz).

## `wc-commons`

O que todo serviço ganha só por depender do módulo (auto-configuração do Spring Boot):

- **Erro padrão** (`erro/`): `GlobalExceptionHandler` devolve `{message, status, erro, campos}` (seção 5.0).
  Lance `NaoEncontradoException` (404), `ConflitoException` (409), `RegraNegocioException` (422),
  `ValidacaoException` (400), `NaoAutenticadoException` (401), `SemPermissaoException` (403) ou `ApiException`.
- **Ids numéricos** (`dynamo/IdGenerator`): `ids.proximo("clientes")` usa a tabela `counters` com `ADD valor :1`.
- **Listagem completa** (`dynamo/ListagemDynamo.todos(...)`): percorre todas as páginas do DynamoDB, porque o front não pagina.
- **Segurança** (`seguranca/`): filtro que valida o JWT HS512 e a sessão no Redis (401 se faltar), `@RequerPapel(Papel.ADMIN)`
  em endpoints (403), `UsuarioAtual.exigir(...)` para regras condicionais e `UsuarioAutenticado` como parâmetro de controller.
  Caminhos públicos em `wc.seguranca.caminhos-publicos`.
- **Clientes AWS** (`aws/`): DynamoDB (+ Enhanced), SQS, SNS, S3 e Secrets Manager, apontando para o LocalStack quando `wc.aws.endpoint` está preenchido.
- **Mensageria** (`mensageria/`): `EventPublisher` (SQS e SNS, com atributos `eventType` e `correlationId`) e `IdempotencyGuard` (Redis `SET NX`).
- **Validadores** (`validacao/`): `@CPF`, `@CNPJ`, `@CpfOuCnpj`, `@UF`, `@NCM`, `@ChaveAcesso`.
- **Logs**: `logback-spring.xml` do serviço inclui `wc-logback.xml` (JSON com `correlationId`; texto nos perfis `test` e `dev`).
- **Testes**: o test-jar traz `InfraTeste`, que sobe LocalStack (com os mesmos scripts de `infra/local/init`) e Redis uma vez por JVM.

## Novo serviço

1. Copie o `pom.xml` do `auth-service` e troque `artifactId`/descrição; adicione o módulo no `pom.xml` pai.
2. `application.yml` com `wc.aws.*`, `spring.data.redis.*` e os caminhos públicos.
3. Entrada no `infra/local/docker-compose.yml` usando a âncora `*servico` e `build.args.MODULE`.
4. Testes de integração com `@DynamicPropertySource` chamando `InfraTeste.registrar(r)`.
