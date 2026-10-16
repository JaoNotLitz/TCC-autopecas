# HU-B04.3 — Detalhar produto

**Épico:** [EPB-04](README.md) · **Unidade:** `product-service` · **Estimativa:** 2 pts · **Depende de:** HU-B04.1

## História

Como **sales-service e front**, quero **os dados completos de um produto com o estoque disponível** para **validar uma venda e montar o formulário de edição**.

## Requisitos funcionais

- RF01 — `GET /api/produtos/{id}` no formato da seção 5.3, incluindo `estoque.ultimaMovimentacao` e os campos fiscais (`cfop`, `origem`, `cstIcms`, `aliquotaIcms`).
- RF02 — Inexistente → `404`.
- RF03 — Endpoint interno `POST /api/produtos/disponibilidade` (não exposto no gateway) recebe `[{produtoId, quantidade}]` e devolve, por item, `disponivel` e `suficiente` — usado pelo sales-service (decisão A4).

## Critérios de aceite

```gherkin
Cenário: detalhe
  Quando chamo GET /api/produtos/{id} de um produto existente
  Então recebo 200 com os campos fiscais e o objeto estoque

Cenário: disponibilidade
  Dado um produto com disponivel 3
  Quando consulto disponibilidade de 5 unidades
  Então o item volta com suficiente false e disponivel 3
```

## Subtasks

- [ ] ST-B04.3.1 Endpoint de detalhe
- [ ] ST-B04.3.2 Endpoint interno de disponibilidade

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-B04.3.1 | API | detalhe e 404 | detalhe |
| TC-B04.3.2 | Integração | disponibilidade suficiente e insuficiente | disponibilidade |
| TC-B04.3.3 | Smoke | rota interna não responde pelo gateway | RF03 |
