# HU-B05.2 — Baixar e dar entrada no estoque

**Épico:** [EPB-05](README.md) · **Unidade:** `InventoryUpdateListener` · **Estimativa:** 3 pts · **Depende de:** HU-B05.1

## História

Como **gerente**, quero **que a nota autorizada baixe o estoque e que entradas e devoluções somem** para **o estoque refletir o que realmente saiu e entrou**.

## Requisitos funcionais

- RF01 — `decrease` (nota de venda autorizada): `estoqueAtual −= quantidade` e `reservado −= quantidade`; movimentação `saida` com `referenciaType = sale`.
- RF02 — `increase` (entrada de mercadoria, cancelamento de nota, devolução autorizada): `estoqueAtual += quantidade`; movimentação `entrada` com `referenciaType` `entry`, `cancellation` ou `return`.
- RF03 — `decrease` sem reserva correspondente (ex.: venda antiga) baixa só o `estoqueAtual` e registra aviso no log.

## Regras e requisitos não funcionais

- `estoqueAtual` nunca fica negativo: `decrease` maior que o estoque → DLQ com motivo `ESTOQUE_NEGATIVO`.

## Critérios de aceite

```gherkin
Cenário: baixa após autorização
  Dado estoqueAtual 10 e reservado 3
  Quando chega decrease de 3
  Então estoqueAtual fica 7, reservado 0 e disponivel 7

Cenário: entrada de mercadoria
  Dado estoqueAtual 7
  Quando chega increase de 100 com referenciaType entry
  Então estoqueAtual fica 107 e há uma movimentação "entrada"
```

## Subtasks

- [ ] ST-B05.2.1 Operações decrease/increase
- [ ] ST-B05.2.2 Mapeamento de `referenciaType` para o tipo de movimentação

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-B05.2.1 | Unitário | decrease com e sem reserva | baixa |
| TC-B05.2.2 | Integração | increase de entrada | entrada |
| TC-B05.2.3 | Integração | decrease maior que o estoque → DLQ | RNF |
