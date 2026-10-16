# HU-B02.2 — API REST com rotas para os 8 serviços

**Épico:** [EPB-02](README.md) · **Unidade:** API Gateway `wc-autopecas-api` · **Estimativa:** 5 pts · **Depende de:** HU-B02.1, HU-B00.2

## História

Como **front-end**, quero **um único endereço base para a API** para **não precisar conhecer a porta de cada microsserviço**.

## Requisitos funcionais

- RF01 — Recursos `{proxy+}` por prefixo, cada um com integração `HTTP_PROXY`:
  `/api/auth` → `auth-service:8080`, `/api/clientes` → `customer-service:8081`, `/api/produtos` → `product-service:8082`, `/api/vendas` → `sales-service:8083`, `/api/notas` → `fiscal-service:8084`, `/api/entrada` → `entry-service:8085`, `/api/documentos` → `document-service:8086`, `/api/dashboard` e `/api/relatorios` → `reports-service:8087`.
- RF02 — Métodos `POST /api/auth/login` e `POST /api/auth/refresh` sem authorizer; demais com `wc-jwt-authorizer`.
- RF03 — Stage `local`, deployment feito pelo script de init; o ID da API fica fixo via tag `_custom_id_` (`wcapi`) para o front ter uma URL estável.
- RF04 — Uploads `multipart/form-data` e respostas binárias (PDF, ZIP, XLSX) passam sem corrupção (`binaryMediaTypes`).

## Critérios de aceite

```gherkin
Cenário: roteamento por prefixo
  Dado todos os serviços no ar
  Quando chamo GET {gateway}/api/produtos com token
  Então a resposta vem do product-service

Cenário: rota pública
  Quando chamo POST {gateway}/api/auth/login sem token
  Então recebo a resposta do auth-service

Cenário: download binário
  Quando baixo um PDF por GET {gateway}/api/documentos/pdf/{id}
  Então o arquivo recebido é idêntico ao do S3

Cenário: URL estável
  Quando executo "make reset && make up"
  Então a URL base do gateway continua a mesma
```

## Subtasks

- [ ] ST-B02.2.1 Script `awslocal apigateway` criando recursos, métodos e integrações
- [ ] ST-B02.2.2 ID customizado e stage `local`
- [ ] ST-B02.2.3 `binaryMediaTypes`
- [ ] ST-B02.2.4 Documentar a URL base no README e no `environment.development.ts` do front

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-B02.2.1 | Smoke | `GET /actuator/health` de cada serviço via gateway (rota técnica) | roteamento |
| TC-B02.2.2 | API | login sem token → 200 | rota pública |
| TC-B02.2.3 | Integração | hash do PDF baixado = hash no S3 | binário |
| TC-B02.2.4 | Smoke | URL igual após reset | URL estável |
