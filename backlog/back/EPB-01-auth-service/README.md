# EPB-01 — auth-service

**Unidade entregue:** microsserviço `auth-service` (API REST, porta 8080), container no Compose.
**Tipo:** API · **Sprint sugerida:** 1 · **Arquitetura:** seções 4.1 e 5.1

## Objetivo

Autenticar usuários por e-mail e senha, emitir JWT HS512 (access + refresh), manter a sessão no Redis e controlar bloqueio por tentativas. O token emitido aqui é o mesmo que o Lambda Authorizer (EPB-02) e o filtro do `wc-commons` validam.

## Dependências de infraestrutura

DynamoDB `users` · Redis (sessões e contadores) · Secrets Manager `wc-jwt-secret`.

## Critérios de aceite do épico

- Os 4 endpoints de `/api/auth` respondem conforme a seção 5.1 da arquitetura.
- Login do front (EPF-01) funciona contra o serviço rodando no Compose.
- Senhas nunca aparecem em log nem em resposta.

## Histórias

| ID | Título | Pts |
|---|---|---|
| [HU-B01.1](HU-B01.1-login.md) | Login com e-mail e senha | 5 |
| [HU-B01.2](HU-B01.2-bloqueio-tentativas.md) | Bloqueio após 5 tentativas falhas | 3 |
| [HU-B01.3](HU-B01.3-sessao-me.md) | Sessão no Redis e `GET /me` | 3 |
| [HU-B01.4](HU-B01.4-refresh-token.md) | Renovar token (refresh) | 3 |
| [HU-B01.5](HU-B01.5-logout.md) | Logout | 2 |
| [HU-B01.6](HU-B01.6-papeis-autorizacao.md) | Papéis e autorização por operação | 3 |
