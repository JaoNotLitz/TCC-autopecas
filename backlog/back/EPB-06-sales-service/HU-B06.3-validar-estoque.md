# HU-B06.3 — Validar estoque disponível

**Épico:** [EPB-06](README.md) · **Unidade:** `sales-service` · **Estimativa:** 3 pts · **Depende de:** HU-B06.1, HU-B04.3

## História

Como **vendedor**, quero **saber antes de finalizar se falta alguma peça** para **não vender o que não tenho**.

## Requisitos funcionais

- RF01 — Antes de gravar, chama `POST http://product-service:8082/api/produtos/disponibilidade` com todos os itens (somando quantidades do mesmo produto).
- RF02 — Algum item insuficiente → `409` "Estoque insuficiente" com `campos[{campo: "itens[i].quantidade", message: "Disponível: N"}]` para cada um.
- RF03 — Vale para `finalizada` e `orcamento` (orçamento também reserva).

## Regras e requisitos não funcionais

- A validação é otimista; a garantia final é a `ConditionExpression` do listener (HU-B05.1).

## Critérios de aceite

```gherkin
Cenário: falta de estoque
  Dado o produto PF-001 com disponivel 1
  Quando envio venda com 2 unidades
  Então recebo 409 com campos itens[0].quantidade e message "Disponível: 1"

Cenário: mesmo produto em duas linhas
  Dado disponivel 3
  Quando envio duas linhas do mesmo produto com 2 unidades cada
  Então recebo 409
```

## Subtasks

- [ ] ST-B06.3.1 `ProductClient`
- [ ] ST-B06.3.2 Agregação por produto e mapeamento do erro

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-B06.3.1 | Unitário (WireMock) | insuficiente → 409 com campos | falta |
| TC-B06.3.2 | Unitário | agregação de linhas repetidas | mesmo produto |
