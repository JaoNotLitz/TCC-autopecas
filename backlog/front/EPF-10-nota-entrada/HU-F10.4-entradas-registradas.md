# HU-F10.4 — Entradas registradas

**Épico:** [EPF-10](README.md) · **Estimativa:** 2 pts · **Depende de:** HU-B11.4

## História

Como **estoquista**, quero **ver as entradas já lançadas e as que ficaram com pendência** para **resolver o que falta**.

## Requisitos funcionais

- RF01 — Aba "Entradas registradas" na mesma tela com `GET /api/entrada` paginado e filtros por fornecedor (CNPJ) e período.
- RF02 — Colunas: data de entrada, fornecedor, número, valor, origem (manual/XML), status (registrada, com pendências, erro, recebida).
- RF03 — Detalhe (`GET /api/entrada/{id}`) mostra itens; itens `vinculado: false` têm o atalho "Cadastrar produto".

## Critérios de aceite

```gherkin
Cenário: entrada com pendência
  Dado uma importação assíncrona com item desconhecido
  Então a lista mostra a entrada "Com pendências" e o detalhe aponta o item
```

## Subtasks

- [ ] ST-F10.4.1 Aba de lista e detalhe

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-F10.4.1 | Unitário | fixture com status variados | pendência |
