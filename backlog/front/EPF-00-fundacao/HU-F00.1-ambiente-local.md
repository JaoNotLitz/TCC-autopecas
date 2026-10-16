# HU-F00.1 — Apontar o front para a stack local

**Épico:** [EPF-00](README.md) · **Estimativa:** 2 pts · **Depende de:** HU-B02.2

## História

Como **desenvolvedora do front**, quero **rodar `ng serve` falando com o back do Docker/LocalStack** para **desenvolver as telas com dados reais**.

## Requisitos funcionais

- RF01 — `environment.development.ts` com `apiUrl` = URL estável do gateway (`http://wcapi.execute-api.localhost.localstack.cloud:4566/local` ou equivalente documentado na HU-B02.2) e `mockAuth: false`.
- RF02 — Alternativa com `proxy.conf.json` (`/api` → gateway) para quem preferir URL relativa; documentado no README do front.
- RF03 — `environment.ts` (produção) com `apiUrl: '/api'`.
- RF04 — Remover o login mock (`admin@autopecas.com`) do `AuthApiService` ou mantê-lo só atrás de `mockAuth: true` explicitamente.

## Critérios de aceite

```gherkin
Cenário: login contra a stack local
  Dado a stack do back no ar com seed
  Quando rodo ng serve e faço login com o admin do seed
  Então entro no sistema sem erro de CORS no console
```

## Subtasks

- [ ] ST-F00.1.1 Ajustar `environments/`
- [ ] ST-F00.1.2 `proxy.conf.json` e script `npm run start:local`
- [ ] ST-F00.1.3 Seção "Rodando com o back local" no README

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-F00.1.1 | E2E (Playwright, EPF-11) | login real contra a stack | login |
