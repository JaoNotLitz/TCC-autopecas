# HU-B07.3 — Itens da nota

**Épico:** [EPB-07](README.md) · **Unidade:** `fiscal-service` · **Estimativa:** 2 pts · **Depende de:** HU-B07.1

## História

Como **vendedor**, quero **ver os itens de uma nota** para **escolher o que o cliente está devolvendo**.

## Requisitos funcionais

- RF01 — `GET /api/notas/{id}/itens` responde `NotaFiscalItem[]` (array puro) no formato do front: `produtoId`, `codigo`, `descricao`, `ncm`, `cfop`, `quantidade`, `valorUnitario`.
- RF02 — Inexistente → `404`.
- RF03 — Extra (uso interno e e2e): `GET /api/notas/{id}` responde a `NotaFiscal`; `GET /internal/notas/{id}` traz também `protocolo`, `codigoStatus`, `motivoRejeicao`, `xmlPath`, `pdfPath` e o saldo devolvível por produto.

## Critérios de aceite

```gherkin
Cenário: itens para devolução
  Dado uma nota autorizada com 2 itens
  Quando chamo GET /api/notas/{id}/itens
  Então recebo um array com os 2 itens, cada um com produtoId, quantidade e valorUnitario

Cenário: nota inexistente
  Quando chamo com um id que não existe
  Então recebo 404
```

## Subtasks

- [ ] ST-B07.3.1 Endpoint de itens
- [ ] ST-B07.3.2 Detalhe extra e rota interna

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-B07.3.1 | API | itens e 404 | itens |
| TC-B07.3.2 | Contrato | itens validam contra `NotaFiscalItem` do front | RF01 |
