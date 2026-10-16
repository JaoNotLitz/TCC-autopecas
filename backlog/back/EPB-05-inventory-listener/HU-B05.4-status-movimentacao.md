# HU-B05.4 — Recalcular status e registrar movimentação

**Épico:** [EPB-05](README.md) · **Unidade:** `InventoryUpdateListener` · **Estimativa:** 3 pts · **Depende de:** HU-B05.1, HU-B05.2, HU-B05.3

## História

Como **gerente**, quero **que o status do estoque e o histórico sejam atualizados a cada mudança** para **confiar nos alertas e na auditoria**.

## Requisitos funcionais

- RF01 — Após qualquer operação, recalcula `status` com o `StatusEstoqueCalculator` (crítico ≤ 40% do mínimo; baixo < 70%; atenção < 100%; ok ≥ mínimo).
- RF02 — Atualiza `inventory` e grava em `inventory_movements` **na mesma transação** (`TransactWriteItems`), com `estoqueAnterior`, `estoqueNovo` e `data`.
- RF03 — Atualiza `ultimaMovimentacao` e invalida `product:{id}` e `product:resumo` no Redis.

## Critérios de aceite

```gherkin
Cenário: transação atômica
  Dado uma falha simulada na gravação da movimentação
  Então o inventory não é alterado e a mensagem volta para a fila

Cenário: status muda com a baixa
  Dado estoqueMinimo 10 e estoqueAtual 8 (atencao)
  Quando chega decrease de 5
  Então o status vira "critico"
```

## Subtasks

- [ ] ST-B05.4.1 Transação inventory + movimentação
- [ ] ST-B05.4.2 Invalidação de cache

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-B05.4.1 | Integração | falha simulada não deixa estado parcial | atômica |
| TC-B05.4.2 | Unitário | transições de status | status |
| TC-B05.4.3 | Integração (Redis) | cache invalidado | RF03 |
