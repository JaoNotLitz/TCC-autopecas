# HU-B06.5 — Listar e detalhar vendas

**Épico:** [EPB-06](README.md) · **Unidade:** `sales-service` · **Estimativa:** 3 pts · **Depende de:** HU-B06.1

## História

Como **vendedor**, quero **consultar vendas e orçamentos** para **retomar um orçamento ou acompanhar a nota de uma venda**.

## Requisitos funcionais

- RF01 — `GET /api/vendas` com `status`, `clienteId`, `vendedorId`, `de`, `ate`, `limit`, `lastKey`; envelope padrão com vendas resumidas (sem itens).
- RF02 — `status` → `status-index`; `clienteId` → `cliente-index`; período → `data-index` (PK `dataDia`, SK `criadaEm`).
- RF03 — `GET /api/vendas/{id}` no formato da seção 5.4: inclui `cliente {id, nome}`, `notaFiscalId`, `itens[]` com `itemId`, `codigo`, `nome`, `valorTotal`.
- RF04 — Inexistente → `404`.

## Critérios de aceite

```gherkin
Cenário: listar orçamentos
  Dado 3 orçamentos e 5 vendas finalizadas
  Quando chamo GET /api/vendas?status=orcamento
  Então recebo 3 itens

Cenário: detalhe com itens
  Quando chamo GET /api/vendas/{id}
  Então recebo os itens com itemId e valorTotal
```

## Subtasks

- [ ] ST-B06.5.1 Consultas por índice
- [ ] ST-B06.5.2 Montagem do detalhe (venda + itens + nome do cliente)

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-B06.5.1 | Integração | filtros por status, cliente e período | listar |
| TC-B06.5.2 | API | detalhe e 404 | detalhe |
