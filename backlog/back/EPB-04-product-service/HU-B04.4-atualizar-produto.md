# HU-B04.4 — Atualizar produto

**Épico:** [EPB-04](README.md) · **Unidade:** `product-service` · **Estimativa:** 3 pts · **Depende de:** HU-B04.1

## História

Como **gerente**, quero **alterar preço, mínimo e dados fiscais de um produto** para **manter o catálogo correto**.

## Requisitos funcionais

- RF01 — `PUT /api/produtos/{id}` com o corpo do POST sem `estoqueInicial`; responde `200` no formato do detalhe.
- RF02 — **Não altera quantidade**: se vier `estoqueInicial` ou `estoqueAtual`, responde `400` orientando a usar `/ajuste`.
- RF03 — Recalcula `margemLucro`. Se `estoqueMinimo` mudar, recalcula `inventory.status` (e publica alerta crítico se passar a crítico, via mesma regra do EPB-05).
- RF04 — Invalida cache.

## Critérios de aceite

```gherkin
Cenário: mudar preço
  Quando altero precoVenda de 89,90 para 99,90
  Então margemLucro é recalculada

Cenário: mudar mínimo muda status
  Dado um produto com estoqueAtual 10 e estoqueMinimo 5 (ok)
  Quando altero estoqueMinimo para 30
  Então estoque.status vira "critico"

Cenário: tentar mudar quantidade
  Quando envio estoqueAtual no PUT
  Então recebo 400 com message citando /ajuste
```

## Subtasks

- [ ] ST-B04.4.1 `ProdutoService.atualizar`
- [ ] ST-B04.4.2 Recalcular status ao mudar mínimo

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-B04.4.1 | Integração | preço e margem | preço |
| TC-B04.4.2 | Integração | mínimo e status | mínimo |
| TC-B04.4.3 | API | `estoqueAtual` no corpo → 400 | quantidade |
