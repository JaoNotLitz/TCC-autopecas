# HU-B04.5 — Desativar produto

**Épico:** [EPB-04](README.md) · **Unidade:** `product-service` · **Estimativa:** 2 pts · **Depende de:** HU-B04.1, HU-B01.6

## História

Como **administradora**, quero **tirar de linha um produto** para **que ele não apareça mais no PDV sem perder o histórico**.

## Requisitos funcionais

- RF01 — `DELETE /api/produtos/{id}` (ADMIN) define `ativo = false` e responde `204`.
- RF02 — Com `inventory.reservado > 0` → `409` "Produto com estoque reservado".
- RF03 — Produto inativo não é aceito em novas vendas (validado no EPB-06).

## Critérios de aceite

```gherkin
Cenário: desativar
  Dado um produto sem reserva
  Quando um ADMIN chama DELETE
  Então recebo 204 e o produto aparece com ativo false

Cenário: com reserva
  Dado um produto com reservado 2
  Então DELETE responde 409
```

## Subtasks

- [ ] ST-B04.5.1 Endpoint com checagem condicional (`ConditionExpression reservado = 0`)

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-B04.5.1 | Integração | sem reserva → 204 | desativar |
| TC-B04.5.2 | Integração | com reserva → 409 | reserva |
| TC-B04.5.3 | API | VENDEDOR → 403 | permissão |
