# HU-F05.6 — Salvar e retomar orçamento

**Épico:** [EPF-05](README.md) · **Estimativa:** 3 pts · **Depende de:** HU-F05.5, HU-B06.5, HU-B06.6

## História

Como **vendedor**, quero **salvar um orçamento e retomá-lo depois** para **o cliente decidir com calma sem perder as peças reservadas**.

## Requisitos funcionais

- RF01 — "Salvar orçamento" envia o mesmo payload com `status: "orcamento"`; mostra o número.
- RF02 — Botão "Orçamentos" abre lista (`GET /api/vendas?status=orcamento`, paginada) com número, cliente, total e data.
- RF03 — Abrir um orçamento carrega o detalhe (`GET /api/vendas/{id}`) em modo leitura com as ações "Finalizar" (`PUT /api/vendas/{id}/status {finalizada}`) e "Cancelar orçamento" (com confirmação, `DELETE /api/vendas/{id}`).
- RF04 — Editar itens de um orçamento salvo fica fora do escopo (cancelar e criar outro).

## Critérios de aceite

```gherkin
Cenário: finalizar orçamento
  Dado um orçamento salvo
  Quando o abro e clico em Finalizar
  Então ele vira venda e começa o acompanhamento da nota

Cenário: cancelar orçamento
  Quando cancelo e confirmo
  Então ele some da lista
```

## Subtasks

- [ ] ST-F05.6.1 Salvar orçamento
- [ ] ST-F05.6.2 Lista e detalhe de orçamentos
- [ ] ST-F05.6.3 Finalizar e cancelar

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-F05.6.1 | Unitário | status orcamento no payload | RF01 |
| TC-F05.6.2 | Unitário | finalizar chama PUT; cancelar chama DELETE após confirmação | cenários |
