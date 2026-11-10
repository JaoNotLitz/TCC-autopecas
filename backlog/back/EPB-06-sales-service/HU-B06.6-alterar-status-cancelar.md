# HU-B06.6 — Finalizar ou cancelar orçamento

**Épico:** [EPB-06](README.md) · **Unidade:** `sales-service` · **Estimativa:** 3 pts · **Depende de:** HU-B06.4

## História

Como **vendedor**, quero **transformar um orçamento em venda ou cancelá-lo** para **liberar as peças reservadas quando o cliente desiste**.

## Requisitos funcionais

- RF01 — `PUT /api/vendas/{id}/status` com `{status}`; transições permitidas: `orcamento → finalizada` e `orcamento → cancelada`.
- RF02 — `→ finalizada`: grava `finalizadaEm`, revalida cliente para NF-e (HU-B06.2) e publica `sale.created`.
- RF03 — `→ cancelada`: publica `release` por item.
- RF04 — `DELETE /api/vendas/{id}` equivale a `→ cancelada`; responde `204`.
- RF05 — Transição inválida (ex.: finalizada → cancelada) → `422` "Vendas finalizadas devem ser canceladas pela nota fiscal".

## Regras e requisitos não funcionais

- Atualização condicional (`status = orcamento`) para evitar dupla finalização concorrente → `409`.

## Critérios de aceite

```gherkin
Cenário: finalizar orçamento
  Dado um orçamento
  Quando envio status finalizada
  Então recebo 200 e um sale.created é publicado

Cenário: cancelar orçamento
  Dado um orçamento com 2 itens
  Quando chamo DELETE /api/vendas/{id}
  Então recebo 204 e 2 mensagens release são publicadas

Cenário: cancelar venda finalizada
  Dado uma venda finalizada
  Quando chamo DELETE
  Então recebo 422
```

## Subtasks

- [ ] ST-B06.6.1 Máquina de estados da venda
- [ ] ST-B06.6.2 Endpoints PUT status e DELETE

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-B06.6.1 | Unitário | tabela de transições | transições |
| TC-B06.6.2 | Integração | finalizar publica sale.created | finalizar |
| TC-B06.6.3 | Integração | cancelar publica release | cancelar |
| TC-B06.6.4 | Integração | duas finalizações simultâneas → uma 200, outra 409 | concorrência |
