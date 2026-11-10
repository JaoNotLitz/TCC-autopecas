# HU-F04.4 — Desativar produto

**Épico:** [EPF-04](README.md) · **Estimativa:** 1 pt · **Depende de:** HU-B04.5

## História

Como **administradora**, quero **tirar um produto de linha com confirmação** para **ele não aparecer mais no PDV**.

## Requisitos funcionais

- RF01 — "Desativar" (ADMIN) com confirmação; `DELETE /api/produtos/{id}`.
- RF02 — `409` mostra "Produto com estoque reservado. Finalize ou cancele as vendas pendentes antes".

## Critérios de aceite

```gherkin
Cenário: produto reservado
  Dado um produto com reserva
  Quando tento desativar
  Então vejo a mensagem de estoque reservado e ele continua ativo
```

## Subtasks

- [ ] ST-F04.4.1 Ação com confirmação e tratamento do 409

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-F04.4.1 | Unitário | 204 e 409 | reservado |
