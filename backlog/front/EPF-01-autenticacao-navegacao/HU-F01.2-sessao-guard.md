# HU-F01.2 — Sessão ao abrir a página e proteção de rotas

**Épico:** [EPF-01](README.md) · **Estimativa:** 3 pts · **Depende de:** HU-F01.1, HU-B01.3

## História

Como **usuário**, quero **continuar logado ao recarregar a página na mesma aba** e **não ver telas internas sem estar logado**.

## Requisitos funcionais

- RF01 — Ao iniciar o app (`provideAppInitializer`), se houver token, chama `GET /auth/me`; enquanto isso `carregando = true` e o layout mostra um carregamento de tela cheia.
- RF02 — `401` em `/me` (depois da tentativa de refresh do interceptor) limpa a sessão.
- RF03 — `authGuard` (`CanMatchFn`) redireciona para `/login?returnUrl=…` quando não autenticado; `/login` redireciona para `/` quando já autenticado.
- RF04 — O usuário nunca é persistido no navegador (só o token), como diz o mapeamento.

## Critérios de aceite

```gherkin
Cenário: recarregar a página
  Dado que estou logado
  Quando recarrego /clientes
  Então continuo em /clientes sem passar pelo login

Cenário: nova aba sem sessão
  Quando abro /clientes numa aba nova (sessionStorage vazio)
  Então sou levado ao login
```

## Subtasks

- [ ] ST-F01.2.1 Inicializador da sessão
- [ ] ST-F01.2.2 Guard e redirecionamentos

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-F01.2.1 | Unitário | guard com e sem sessão | RF03 |
| TC-F01.2.2 | Unitário | `/me` 401 limpa sessão | RF02 |
| TC-F01.2.3 | E2E | recarregar mantém a tela | recarregar |
