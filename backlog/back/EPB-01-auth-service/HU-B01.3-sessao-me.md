# HU-B01.3 — Sessão no Redis e `GET /api/auth/me`

**Épico:** [EPB-01](README.md) · **Unidade:** `auth-service` · **Estimativa:** 3 pts · **Depende de:** HU-B01.1

## História

Como **front-end**, quero **validar o token salvo ao abrir a página** para **saber quem está logado sem guardar o usuário no navegador**.

## Requisitos funcionais

- RF01 — `GET /api/auth/me` com `Authorization: Bearer {token}` responde `200` com `{id, nome, email, papel}`.
- RF02 — Valida assinatura e expiração do token, existência da sessão no Redis e `ativo = true` do usuário.
- RF03 — Qualquer falha responde `401` no formato padrão.

## Critérios de aceite

```gherkin
Cenário: token válido
  Dado um token emitido pelo login e a sessão no Redis
  Quando chamo GET /api/auth/me
  Então recebo 200 com os dados do usuário

Cenário: sessão expirada
  Dado um token válido cuja sessão não existe mais no Redis
  Quando chamo GET /api/auth/me
  Então recebo 401

Cenário: usuário desativado depois do login
  Dado um token válido de um usuário que foi desativado
  Então GET /api/auth/me responde 401

Cenário: refresh token usado como access
  Quando chamo GET /api/auth/me com um refreshToken
  Então recebo 401
```

## Subtasks

- [ ] ST-B01.3.1 Endpoint `/me`
- [ ] ST-B01.3.2 Rejeitar `typ = refresh` em rotas protegidas

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-B01.3.1 | API | token válido → 200 | token válido |
| TC-B01.3.2 | Integração (Redis) | apaga sessão → 401 | sessão expirada |
| TC-B01.3.3 | Integração | desativa usuário → 401 | desativado |
| TC-B01.3.4 | Unitário | `typ=refresh` → 401 | refresh como access |
