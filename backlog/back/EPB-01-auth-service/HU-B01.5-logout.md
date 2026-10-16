# HU-B01.5 — Logout

**Épico:** [EPB-01](README.md) · **Unidade:** `auth-service` · **Estimativa:** 2 pts · **Depende de:** HU-B01.3

## História

Como **usuário**, quero **sair do sistema** para **que ninguém use minha sessão no mesmo computador**.

## Requisitos funcionais

- RF01 — `POST /api/auth/logout` com `Authorization: Bearer` responde `204`.
- RF02 — Remove a sessão do Redis e coloca o `jti` do access na blacklist até a expiração.

## Critérios de aceite

```gherkin
Cenário: logout invalida o token
  Dado um usuário logado
  Quando faço POST /api/auth/logout
  Então recebo 204
  E GET /api/auth/me com o mesmo token responde 401
  E qualquer outro serviço com o mesmo token responde 401

Cenário: logout com token inválido
  Quando faço logout com token expirado
  Então recebo 401
```

## Subtasks

- [ ] ST-B01.5.1 Endpoint `/logout`
- [ ] ST-B01.5.2 Filtro do `wc-commons` consulta a blacklist

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-B01.5.1 | API | logout → 204 → `/me` 401 | invalida token |
| TC-B01.5.2 | Integração | token após logout em `customer-service` (stub) → 401 | outros serviços |
| TC-B01.5.3 | Unitário | token expirado → 401 | token inválido |
