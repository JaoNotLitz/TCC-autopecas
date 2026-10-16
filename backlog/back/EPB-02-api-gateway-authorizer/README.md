# EPB-02 — Lambda Authorizer e API Gateway

**Unidade entregue:** função Lambda `wc-jwt-authorizer` (Java 21) e a API REST `wc-autopecas-api` no API Gateway do LocalStack, roteando para cada microsserviço.
**Tipo:** Lambda + gateway · **Sprint sugerida:** 2 · **Arquitetura:** seção 3.3

## Objetivo

Dar ao front um **único endereço** (`http://localhost:4566/restapis/{id}/local/_user_request_/api/...` ou o alias `http://wc-api.localhost.localstack.cloud:4566/api`) que valida o JWT antes de chegar nos serviços, como o API Gateway faria na AWS.

## Decisão local (A1)

O ALB e o API Gateway HTTP API (v2) são recursos pagos do LocalStack. Localmente o gateway é **REST (v1)** com integração `HTTP_PROXY` direto para `http://{servico}:{porta}` na rede do Compose. O roteamento por prefixo é o mesmo da seção 3.2.

## Critérios de aceite do épico

- O front fala só com o gateway; nenhum serviço é chamado direto pela porta.
- Rotas públicas: `POST /api/auth/login` e `POST /api/auth/refresh`. Todo o resto exige token válido.
- A API é recriada automaticamente no init do LocalStack.

## Histórias

| ID | Título | Pts |
|---|---|---|
| [HU-B02.1](HU-B02.1-lambda-authorizer.md) | Lambda Authorizer que valida o JWT | 5 |
| [HU-B02.2](HU-B02.2-rotas-gateway.md) | API REST com rotas para os 8 serviços | 5 |
| [HU-B02.3](HU-B02.3-cors-limites.md) | CORS, limite de payload e throttling | 2 |
| [HU-B02.4](HU-B02.4-contexto-correlation.md) | Repasse de contexto e `correlationId` | 2 |
