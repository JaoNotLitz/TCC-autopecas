# HU-B07.7 — Emitir nota de devolução

**Épico:** [EPB-07](README.md) · **Unidade:** `fiscal-service` · **Estimativa:** 5 pts · **Depende de:** HU-B07.3

## História

Como **vendedor**, quero **emitir uma NF-e de devolução para itens que o cliente trouxe de volta** para **o estoque e o fiscal ficarem corretos**.

## Requisitos funcionais

- RF01 — `POST /api/notas/devolucao` com o `DevolucaoInput` do front: `{notaReferenciadaId, motivo, itens[{produtoId, quantidade, valorUnitario}]}`.
- RF02 — Cria nota `tipo = DEVOLUCAO`, modelo 55, `notaReferenciadaId`, `chaveReferenciada`, mesmo cliente, CFOP de devolução (`1202` dentro do estado; `2202` fora) e itens com as quantidades devolvidas. O `valorUnitario` usado é o da nota original; o enviado pelo front é ignorado.
- RF03 — Publica `generate_xml` na `fiscal-jobs-queue` (segue o fluxo normal de emissão).
- RF04 — O saldo é controlado **por produto** da nota original: `quantidadeDevolvida` só sobe **depois da autorização** (HU-B09.2); enquanto isso, a quantidade fica "em devolução" (`quantidadeEmDevolucao`) para não permitir devolver duas vezes em paralelo. Se o mesmo produto aparece em mais de uma linha da nota, as linhas são somadas.
- RF05 — Responde `201` com a `NotaFiscal` de devolução (`status: "processando"`).

## Regras e requisitos não funcionais

- Nota referenciada `autorizada` e de venda (`NFE`/`NFCE`) → senão `422`.
- `produtoId` precisa estar entre os itens da nota referenciada → senão `422` "Produto não consta na nota original" (não dá para adicionar item novo; resposta à Q4 do front).
- `quantidade ≤ vendida − devolvida − em devolução` daquele produto → senão `422` com o saldo na `message`.
- Pelo menos um item com quantidade > 0 → senão `400`.
- `motivo` opcional (como no front); vazio vira "Devolução de mercadoria".

## Critérios de aceite

```gherkin
Cenário: devolução parcial
  Dado uma nota autorizada com 2 unidades do produto 7
  Quando devolvo 1 unidade do produto 7
  Então recebo 201 com uma NotaFiscal DEVOLUCAO em "processando" e notaReferenciadaId preenchido

Cenário: devolver mais do que restou
  Dado o produto 7 com 2 vendidas e 1 já devolvida
  Quando peço para devolver 2
  Então recebo 422 com message citando o saldo 1

Cenário: produto fora da nota
  Quando envio um produtoId que não está na nota referenciada
  Então recebo 422

Cenário: nota referenciada cancelada
  Então a devolução é recusada com 422
```

## Subtasks

- [ ] ST-B07.7.1 Saldo por produto com `quantidadeEmDevolucao`
- [ ] ST-B07.7.2 Montagem da nota de devolução (CFOP por UF, preço da nota original)
- [ ] ST-B07.7.3 Endpoint e publicação

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-B07.7.1 | Unitário | limites de quantidade, produto repetido em duas linhas | mais do que restou |
| TC-B07.7.2 | Integração | devolução parcial cria nota e job | parcial |
| TC-B07.7.3 | Integração | duas devoluções paralelas do mesmo saldo → uma recusada | RF04 |
| TC-B07.7.4 | Unitário | CFOP 1202 × 2202 | RF02 |
| TC-B07.7.5 | Unitário | `valorUnitario` divergente do front é trocado pelo da nota | RF02 |
