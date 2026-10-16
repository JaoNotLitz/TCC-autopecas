# HU-F09.2 — Escolher itens e quantidades

**Épico:** [EPF-09](README.md) · **Estimativa:** 3 pts · **Depende de:** HU-F09.1, HU-B07.3

## História

Como **vendedor**, quero **indicar quantas unidades de cada item voltam** para **devolver só o que o cliente trouxe**.

## Requisitos funcionais

- RF01 — Itens de `GET /api/notas/{id}/itens`; cada linha: código, descrição, vendida, já devolvida, máximo (`quantidade − quantidadeDevolvida`), campo "Devolver" (0 a máximo) e valor unitário.
- RF02 — Itens com máximo 0 aparecem desabilitados com "Já devolvido".
- RF03 — Total da devolução `Σ qtdDevolver × valorUnitario` em `computed()`.
- RF04 — Remover o botão "Adicionar item" (decisão A6: só itens da nota de origem têm `itemId`).
- RF05 — Motivo opcional.

## Critérios de aceite

```gherkin
Cenário: limite por item
  Dado um item vendido 2 e devolvido 1
  Então o campo Devolver aceita no máximo 1

Cenário: nada selecionado
  Então o botão Emitir fica desabilitado
```

## Subtasks

- [ ] ST-F09.2.1 Tabela de itens com limites
- [ ] ST-F09.2.2 Remover "Adicionar item" e `itensMock`

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-F09.2.1 | Unitário | limites e total | limite |
| TC-F09.2.2 | Unitário | botão desabilitado sem itens | nada selecionado |
