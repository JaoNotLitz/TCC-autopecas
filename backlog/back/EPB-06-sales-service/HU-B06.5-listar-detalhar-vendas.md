# HU-B06.5 — Listar e detalhar vendas (extra)

> **Extra.** O front atual só cria vendas (`POST /api/vendas`). Estes endpoints ficam para a retomada de orçamentos e para o e2e.

**Épico:** [EPB-06](README.md) · **Unidade:** `sales-service` · **Estimativa:** 3 pts · **Depende de:** HU-B06.1

## História

Como **vendedor**, quero **consultar vendas e orçamentos** para **retomar um orçamento ou acompanhar a nota de uma venda**.

## Requisitos funcionais

- RF01 — `GET /api/vendas` com `status`, `clienteId`, `de`, `ate`; responde `Venda[]` (array puro, mais recente primeiro).
- RF02 — `status` → `status-index`; `clienteId` → `cliente-index`; período → `data-index` (PK `dataDia`, SK `criadaEm`).
- RF03 — `GET /api/vendas/{id}` responde a `Venda` do front + `notaFiscalId` (extra, `null` enquanto a nota não foi criada).
- RF04 — Inexistente → `404`.

## Critérios de aceite

```gherkin
Cenário: listar orçamentos
  Dado 3 orçamentos e 5 vendas finalizadas
  Quando chamo GET /api/vendas?status=orcamento
  Então recebo um array com 3 vendas

Cenário: detalhe com itens
  Quando chamo GET /api/vendas/{id}
  Então recebo a venda com itens {produtoId, quantidade, valorUnitario} e notaFiscalId
```

## Subtasks

- [ ] ST-B06.5.1 Consultas por índice
- [ ] ST-B06.5.2 Montagem do detalhe (venda + itens + nome do cliente)

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-B06.5.1 | Integração | filtros por status, cliente e período | listar |
| TC-B06.5.2 | API | detalhe e 404 | detalhe |
