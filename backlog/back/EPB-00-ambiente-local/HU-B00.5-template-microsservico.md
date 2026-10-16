# HU-B00.5 — Template de microsserviço

**Épico:** [EPB-00](README.md) · **Unidade:** módulo `service-template` · **Estimativa:** 3 pts · **Depende de:** HU-B00.3

## História

Como **desenvolvedora**, quero **um esqueleto de serviço pronto para copiar** para **começar cada épico seguinte já com Docker, health check e perfil local funcionando**.

## Requisitos funcionais

- RF01 — Módulo `service-template` com Spring Boot, dependência de `wc-commons`, perfis `local` e `test`.
- RF02 — `Dockerfile` multi-stage (build Maven → JRE 21 slim), usuário não root, `HEALTHCHECK` em `/actuator/health`.
- RF03 — Actuator com `health` (incluindo checagem do DynamoDB, SQS e Redis), `info` e `prometheus`.
- RF04 — Graceful shutdown (`server.shutdown=graceful`, 30 s).
- RF05 — Entrada pronta no `docker-compose.yml` como modelo (porta, variáveis, `depends_on`).
- RF06 — OpenAPI (springdoc) disponível em `/v3/api-docs` e Swagger UI em `/swagger-ui.html` no perfil `local`.

## Critérios de aceite

```gherkin
Cenário: serviço modelo sobe saudável
  Quando executo "docker compose up service-template"
  Então "GET /actuator/health" responde 200 com status "UP" e os componentes dynamodb, sqs e redis "UP"

Cenário: dependência fora do ar
  Dado o Redis parado
  Então "GET /actuator/health" responde 503 com redis "DOWN"
```

## Subtasks

- [ ] ST-B00.5.1 Criar módulo e perfis
- [ ] ST-B00.5.2 Dockerfile multi-stage
- [ ] ST-B00.5.3 Health indicators de DynamoDB, SQS e Redis
- [ ] ST-B00.5.4 Springdoc e graceful shutdown
- [ ] ST-B00.5.5 Guia "como criar um serviço novo" no README do módulo

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-B00.5.1 | Integração (Testcontainers) | health `UP` com tudo no ar | sobe saudável |
| TC-B00.5.2 | Integração | Redis parado → 503 | dependência fora |
