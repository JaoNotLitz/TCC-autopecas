# HU-F01.4 — Menu com usuário e permissões por papel

**Épico:** [EPF-01](README.md) · **Estimativa:** 2 pts · **Depende de:** HU-F01.2, HU-B01.6

## História

Como **vendedor**, quero **ver só as ações que posso executar** para **não tentar algo que vai ser negado**.

## Requisitos funcionais

- RF01 — Sidebar mostra nome e papel do usuário.
- RF02 — Diretiva `*appSePapel="'ADMIN'"` esconde ações restritas: Cancelamento e Inutilização no submenu Fiscal, botões Excluir de clientes e produtos, ajuste de estoque.
- RF03 — Rotas `/cancelamento` e `/inutilizacao` com `papelGuard('ADMIN')`; sem permissão, volta para `/` com toast.
- RF04 — O back continua sendo a garantia (403 tratado pelo interceptor).

## Critérios de aceite

```gherkin
Cenário: vendedor
  Dado que entrei como vendedor
  Então não vejo Cancelamento e Inutilização no menu
  E acessar /inutilizacao pela URL me leva ao início com aviso
```

## Subtasks

- [ ] ST-F01.4.1 Diretiva `appSePapel`
- [ ] ST-F01.4.2 `papelGuard`

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-F01.4.1 | Unitário | diretiva com ADMIN e VENDEDOR | vendedor |
| TC-F01.4.2 | E2E | login de vendedor e URL direta | vendedor |
