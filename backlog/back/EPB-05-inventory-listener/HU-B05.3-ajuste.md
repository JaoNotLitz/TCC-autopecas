# HU-B05.3 — Aplicar ajuste absoluto

**Épico:** [EPB-05](README.md) · **Unidade:** `InventoryUpdateListener` · **Estimativa:** 2 pts · **Depende de:** HU-B05.1

## História

Como **gerente**, quero **que o ajuste de inventário defina a quantidade exata** para **corrigir divergências de contagem**.

## Requisitos funcionais

- RF01 — `adjust`: `estoqueAtual = quantidade` (valor absoluto).
- RF02 — Movimentação `ajuste` com `quantidade = novo − anterior` (com sinal), `motivo` e `usuarioId` da mensagem.

## Critérios de aceite

```gherkin
Cenário: ajuste para menos
  Dado estoqueAtual 40
  Quando chega adjust com quantidade 35 e motivo "Inventário físico"
  Então estoqueAtual fica 35 e a movimentação tem quantidade -5 e o motivo
```

## Subtasks

- [ ] ST-B05.3.1 Operação adjust

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-B05.3.1 | Unitário | delta com sinal | ajuste |
| TC-B05.3.2 | Integração | movimentação com motivo e usuário | ajuste |
