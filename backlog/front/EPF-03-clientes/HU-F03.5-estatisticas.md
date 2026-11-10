# HU-F03.5 — Estatísticas de clientes

**Épico:** [EPF-03](README.md) · **Estimativa:** 1 pt · **Depende de:** HU-B14.1

## História

Como **dona da loja**, quero **ver total, ativos e novos no mês no topo da tela** para **acompanhar a base de clientes**.

## Requisitos funcionais

- RF01 — Cards de topo usam `dashboard.clientes` (`total`, `ativos`, `novosNoMes`); não calculam pela lista paginada.

## Critérios de aceite

```gherkin
Cenário: números do back
  Então os cards mostram os valores de GET /api/dashboard
```

## Subtasks

- [ ] ST-F03.5.1 Cards ligados ao dashboard

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-F03.5.1 | Unitário | fixture do dashboard | números |
