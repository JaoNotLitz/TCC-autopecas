# HU-B02.1 — Lambda Authorizer que valida o JWT

**Épico:** [EPB-02](README.md) · **Unidade:** Lambda `wc-jwt-authorizer` · **Estimativa:** 5 pts · **Depende de:** HU-B01.1

## História

Como **arquiteta do sistema**, quero **barrar tokens inválidos no gateway** para **que requisições sem autenticação não cheguem aos microsserviços**.

## Requisitos funcionais

- RF01 — Lambda Java 21 (`RequestHandler<APIGatewayCustomAuthorizerEvent, IamPolicyResponse>`), tipo `REQUEST`, identity source `Authorization`.
- RF02 — Valida assinatura HS512 com a chave de `wc-jwt-secret` (lida uma vez por container e mantida em cache) e a expiração. Rejeita `typ = refresh`.
- RF03 — Token válido → policy `Allow` para `execute-api:Invoke` e `context` com `usuarioId` e `papel`.
- RF04 — Token ausente, malformado, expirado ou com assinatura inválida → `Unauthorized` (o gateway responde `401`).
- RF05 — Resposta do gateway para `401`/`403` customizada (Gateway Responses) no formato de erro da seção 5.0.
- RF06 — Script de build gera o zip e o init do LocalStack cria a função e o authorizer.

## Regras e requisitos não funcionais

- O authorizer **não** consulta o Redis (sessão e blacklist ficam no filtro do serviço, como diz a arquitetura).
- Cache de autorização do gateway desligado localmente (TTL 0) para facilitar testes de logout.
- Cold start local tolerado; tempo de execução quente ≤ 50 ms.

## Critérios de aceite

```gherkin
Cenário: token válido
  Dado um token emitido pelo auth-service
  Quando chamo GET /api/clientes pelo gateway
  Então a requisição chega ao customer-service

Cenário: sem token
  Quando chamo GET /api/clientes pelo gateway sem Authorization
  Então recebo 401 com {status: 401, erro: "UNAUTHORIZED", message}

Cenário: token adulterado
  Dado um token com o payload alterado
  Então o gateway responde 401

Cenário: token expirado
  Dado um token com exp no passado
  Então o gateway responde 401
```

## Subtasks

- [ ] ST-B02.1.1 Módulo Maven `wc-jwt-authorizer` (aws-lambda-java-core/events)
- [ ] ST-B02.1.2 Validação do JWT reaproveitando o `JwtService` do `wc-commons`
- [ ] ST-B02.1.3 Montagem da policy e do context
- [ ] ST-B02.1.4 Empacotamento (shade) e script de deploy no LocalStack
- [ ] ST-B02.1.5 Gateway Responses no formato padrão

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-B02.1.1 | Unitário | handler com token válido → `Allow` + context | válido |
| TC-B02.1.2 | Unitário | sem token, adulterado, expirado, refresh → `Unauthorized` | inválidos |
| TC-B02.1.3 | Integração (LocalStack) | invoca a Lambda publicada com evento real | válido |
| TC-B02.1.4 | API pelo gateway | sem token → 401 no formato padrão | sem token |
