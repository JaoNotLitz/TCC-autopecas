# HU-B07.7 — Emitir nota de devolução

**Épico:** [EPB-07](README.md) · **Unidade:** `fiscal-service` · **Estimativa:** 5 pts · **Depende de:** HU-B07.3

## História

Como **vendedor**, quero **emitir uma NF-e de devolução para itens que o cliente trouxe de volta** para **o estoque e o fiscal ficarem corretos**.

## Requisitos funcionais

- RF01 — `POST /api/notas/devolucao` com `{notaReferenciadaId, motivo, itens[{itemId, quantidadeDevolver}]}`.
- RF02 — Cria nota `tipo = DEVOLUCAO`, modelo 55, `notaReferenciadaId`, `chaveReferenciada`, mesmo cliente, CFOP de devolução (`1202` dentro do estado; `2202` fora) e itens com as quantidades devolvidas.
- RF03 — Publica `generate_xml` na `fiscal-jobs-queue` (segue o fluxo normal de emissão).
- RF04 — `quantidadeDevolvida` dos itens da nota original só é incrementada **depois da autorização** (HU-B09.2); enquanto isso, a quantidade fica "em devolução" (`quantidadeEmDevolucao`) para não permitir devolver duas vezes em paralelo.
- RF05 — Responde `201` com a nota de devolução.

## Regras e requisitos não funcionais

- Nota referenciada `autorizada` → senão `422`.
- `itemId` precisa pertencer à nota referenciada → senão `422` com `campos itens[i].itemId`.
- `quantidadeDevolver ≤ quantidade − quantidadeDevolvida − quantidadeEmDevolucao` → senão `422`.
- Pelo menos um item com quantidade > 0 → senão `400`.
- `motivo` opcional (como no front).

## Critérios de aceite

```gherkin
Cenário: devolução parcial
  Dado uma nota autorizada com 2 unidades do item A
  Quando devolvo 1 unidade do item A
  Então recebo 201 com uma nota DEVOLUCAO em "processando"

Cenário: devolver mais do que restou
  Dado o item A com quantidade 2 e quantidadeDevolvida 1
  Quando peço para devolver 2
  Então recebo 422 com campos "itens[0].quantidadeDevolver"

Cenário: item de outra nota
  Quando envio um itemId que não pertence à nota referenciada
  Então recebo 422

Cenário: nota referenciada cancelada
  Então a devolução é recusada com 422
```

## Subtasks

- [ ] ST-B07.7.1 Regras de quantidade com `quantidadeEmDevolucao`
- [ ] ST-B07.7.2 Montagem da nota de devolução (CFOP por UF)
- [ ] ST-B07.7.3 Endpoint e publicação

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-B07.7.1 | Unitário | limites de quantidade | mais do que restou |
| TC-B07.7.2 | Integração | devolução parcial cria nota e job | parcial |
| TC-B07.7.3 | Integração | duas devoluções paralelas do mesmo saldo → uma recusada | RF04 |
| TC-B07.7.4 | Unitário | CFOP 1202 × 2202 | RF02 |
