# HU-B00.3 — Projeto Maven multi-módulo e biblioteca `wc-commons`

**Épico:** [EPB-00](README.md) · **Unidade:** `backend/pom.xml` + módulo `wc-commons` · **Estimativa:** 8 pts · **Depende de:** HU-B00.1

## História

Como **desenvolvedora**, quero **um projeto base com o que todos os serviços repetem** (erro padrão, paginação, segurança, clientes AWS) para **não reescrever isso em cada microsserviço e manter o contrato com o front igual em todos**.

## Requisitos funcionais

- RF01 — `backend/pom.xml` pai com Java 21, Spring Boot 3.3.x, Spring Cloud AWS 3.1.x, AWS SDK 2.x, Lombok, MapStruct, e um módulo por serviço.
- RF02 — **Erro padrão**: `@RestControllerAdvice` que converte exceções no JSON `{status, erro, message, campos[]}` da seção 5.0. `campos` só em `400`/`422`. Exceções de domínio: `NaoEncontradoException` (404), `ConflitoException` (409), `RegraNegocioException` (422), `ValidacaoException` (400).
- RF03 — **Paginação**: `PaginaResponse<T>{items, lastKey, count}`, codificação do `LastEvaluatedKey` do DynamoDB em base64 URL-safe, `limit` default 20 e máximo 100 (acima disso, `400`).
- RF04 — **Segurança**: filtro que lê o JWT (`Authorization: Bearer`), valida assinatura HS512 com `wc-jwt-secret`, confere sessão no Redis (`session:{userId}:{tokenHash}`) e popula o `SecurityContext` com `usuarioId` e `papel`. Responde `401` sem token/sessão e `403` para papel insuficiente, no formato padrão.
- RF05 — **Clientes AWS** configurados por `AWS_ENDPOINT_URL` (LocalStack no perfil `local`): `DynamoDbEnhancedClient`, `SqsTemplate`, `SnsTemplate`, `S3Client`, `S3Presigner` (com host público configurável para o navegador), `SecretsManagerClient`.
- RF06 — **Mensageria**: `EventPublisher` que publica JSON com atributos `eventType` e `correlationId`; base `@SqsListener` com log estruturado e ACK manual.
- RF07 — **Idempotência**: utilitário `IdempotencyGuard` usando Redis `SET NX` com TTL (chave `lock:{recurso}`), usado pelos listeners.
- RF08 — **Validadores**: `@CPF`, `@CNPJ`, `@CpfOuCnpj`, `@UF`, `@NCM` (8 dígitos), `@ChaveAcesso` (44 dígitos com DV).
- RF09 — **Logs**: Logback JSON com `correlationId` (MDC) vindo do header `X-Correlation-Id` ou gerado; propagado nas mensagens SQS/SNS.

## Regras e requisitos não funcionais

- `wc-commons` não depende de nenhum serviço.
- Datas sempre ISO 8601 UTC (Jackson configurado), sem timestamps numéricos.

## Critérios de aceite

```gherkin
Cenário: erro de validação no formato padrão
  Dado um endpoint que recebe um DTO com "documento" anotado com @CpfOuCnpj
  Quando envio documento "11111111111"
  Então recebo 400 com erro "BAD_REQUEST", message preenchida e campos[0].campo = "documento"

Cenário: paginação
  Dado 45 registros numa tabela
  Quando peço limit=20 três vezes seguindo lastKey
  Então recebo 20, 20 e 5 itens e a última página vem com lastKey null

Cenário: token sem sessão
  Dado um JWT válido cuja sessão foi removida do Redis
  Quando chamo um endpoint protegido
  Então recebo 401 no formato padrão

Cenário: limit acima do máximo
  Quando peço limit=500
  Então recebo 400 com campos[0].campo = "limit"
```

## Subtasks

- [ ] ST-B00.3.1 POM pai e estrutura de módulos
- [ ] ST-B00.3.2 Handler global de erros e exceções de domínio
- [ ] ST-B00.3.3 `PaginaResponse` e codec de `lastKey`
- [ ] ST-B00.3.4 Filtro JWT + checagem de sessão no Redis
- [ ] ST-B00.3.5 Configuração dos clientes AWS por perfil
- [ ] ST-B00.3.6 `EventPublisher` e base de listener
- [ ] ST-B00.3.7 `IdempotencyGuard`
- [ ] ST-B00.3.8 Validadores de documento, UF, NCM e chave de acesso
- [ ] ST-B00.3.9 Logback JSON e propagação de `correlationId`

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-B00.3.1 | Unitário | handler converte cada exceção no status e JSON corretos | erro padrão |
| TC-B00.3.2 | Unitário | CPF/CNPJ válidos, inválidos, repetidos (`111...`), com máscara | validação |
| TC-B00.3.3 | Integração (LocalStack) | 45 itens paginados de 20 em 20 | paginação |
| TC-B00.3.4 | Integração (Redis) | token válido sem sessão → 401; com sessão → 200; papel errado → 403 | segurança |
| TC-B00.3.5 | Unitário | `limit=500` → 400 | limite |
| TC-B00.3.6 | Unitário | `IdempotencyGuard` bloqueia a segunda chamada com a mesma chave | idempotência |
