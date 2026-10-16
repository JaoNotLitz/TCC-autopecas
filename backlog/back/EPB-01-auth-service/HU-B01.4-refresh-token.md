# HU-B01.4 — Renovar token (refresh)

**Épico:** [EPB-01](README.md) · **Unidade:** `auth-service` · **Estimativa:** 3 pts · **Depende de:** HU-B01.1

## História

Como **usuário em um turno longo de PDV**, quero **que meu acesso seja renovado sem precisar logar de novo** para **não perder uma venda em andamento**.

## Requisitos funcionais

- RF01 — `POST /api/auth/refresh` recebe `{refreshToken}` (rota pública no gateway).
- RF02 — Valida assinatura, expiração, `typ = refresh`, que o `jti` não está na blacklist e que o usuário está ativo.
- RF03 — Gera novo access token e **novo** refreshToken (rotação); o `jti` do refresh antigo vai para a blacklist (`blacklist:{jti}`, TTL até a expiração dele).
- RF04 — Cria nova sessão no Redis e responde no mesmo formato do login.

## Critérios de aceite

```gherkin
Cenário: refresh válido
  Dado um refreshToken válido
  Quando faço POST /api/auth/refresh
  Então recebo 200 com novo token e novo refreshToken

Cenário: reutilização do refresh antigo
  Dado um refreshToken que já foi usado
  Quando faço POST /api/auth/refresh com ele de novo
  Então recebo 401

Cenário: access token enviado como refresh
  Quando envio um access token no campo refreshToken
  Então recebo 401
```

## Subtasks

- [ ] ST-B01.4.1 Endpoint `/refresh`
- [ ] ST-B01.4.2 Blacklist de `jti` no Redis
- [ ] ST-B01.4.3 Rotação do refresh

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-B01.4.1 | API | refresh válido → 200 com tokens diferentes | válido |
| TC-B01.4.2 | Integração (Redis) | segundo uso → 401 | reutilização |
| TC-B01.4.3 | Unitário | access como refresh → 401 | tipo errado |
