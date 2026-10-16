# HU-B04.8 — Histórico de movimentações

**Épico:** [EPB-04](README.md) · **Unidade:** `product-service` · **Estimativa:** 3 pts · **Depende de:** HU-B05.4

## História

Como **gerente**, quero **ver todas as entradas, saídas, reservas e ajustes de um produto** para **entender por que o estoque mudou**.

## Requisitos funcionais

- RF01 — `GET /api/produtos/{id}/movimentos` com `de`, `ate`, `tipo` (`entrada|saida|ajuste|reserva|liberacao`), `limit` (default 50), `lastKey`; envelope padrão.
- RF02 — Query no `produto-data-index` (PK `produtoId`, SK `data`), ordem decrescente de data.
- RF03 — Cada item: `id, produtoId, tipo, quantidade (com sinal), estoqueAnterior, estoqueNovo, referenciaId, referenciaType, usuarioId, motivo, data`.

## Critérios de aceite

```gherkin
Cenário: histórico do produto
  Dado um produto com estoque inicial, uma reserva e uma baixa
  Quando chamo GET /api/produtos/{id}/movimentos
  Então recebo 3 movimentações da mais nova para a mais antiga

Cenário: filtro por tipo e período
  Quando filtro tipo=ajuste e um período sem ajustes
  Então recebo items vazio
```

## Subtasks

- [ ] ST-B04.8.1 Query com `KeyConditionExpression between`
- [ ] ST-B04.8.2 Filtro por tipo e paginação

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-B04.8.1 | Integração | ordem e conteúdo | histórico |
| TC-B04.8.2 | Integração | filtros | filtro |
| TC-B04.8.3 | API | produto inexistente → 404 | RF01 |
