# EPF-01 — Autenticação e navegação

**Área:** `pages/login`, `core/auth.service.ts`, `core/auth.guard.ts`, `layout/` · **Rota:** `/login` e casca autenticada
**Sprint sugerida:** 2 · **Back:** EPB-01, EPB-02

## Objetivo

Login real contra o auth-service, sessão validada em `/auth/me`, logout, e menu mostrando o usuário e escondendo o que o papel dele não pode fazer.

## Histórias

| ID | Título | Pts |
|---|---|---|
| [HU-F01.1](HU-F01.1-login.md) | Tela de login | 3 |
| [HU-F01.2](HU-F01.2-sessao-guard.md) | Sessão ao abrir a página e proteção de rotas | 3 |
| [HU-F01.3](HU-F01.3-logout.md) | Sair do sistema | 1 |
| [HU-F01.4](HU-F01.4-menu-papel.md) | Menu com usuário e permissões por papel | 2 |
