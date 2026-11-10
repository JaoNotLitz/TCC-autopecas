# HU-F02.2 — Card de estoque crítico

**Épico:** [EPF-02](README.md) · **Estimativa:** 2 pts · **Depende de:** HU-B04.2

## História

Como **comprador**, quero **ver no início as peças em nível crítico** para **repor logo**.

## Requisitos funcionais

- RF01 — `EstoqueCard` usa `dashboard.estoque.criticos` quando presente; senão `GET /api/produtos?status=critico&limit=5`.
- RF02 — Cada linha: código, nome, NCM formatado, `estoqueAtual`/`estoqueMinimo` e selo do status vindo do back (sem recalcular).
- RF03 — Link "Ver todos" para `/estoque?status=critico`.
- RF04 — Lista vazia: "Nenhum produto em nível crítico".

## Critérios de aceite

```gherkin
Cenário: críticos
  Dado 2 produtos críticos
  Então o card lista os 2 com o selo "Crítico"
```

## Subtasks

- [ ] ST-F02.2.1 Integração do card

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-F02.2.1 | Unitário | com dados, vazio, com e sem `criticos` no dashboard | críticos |
