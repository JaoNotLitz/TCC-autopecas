# HU-B07.3 — Detalhar nota e itens

**Épico:** [EPB-07](README.md) · **Unidade:** `fiscal-service` · **Estimativa:** 3 pts · **Depende de:** HU-B07.1

## História

Como **responsável fiscal**, quero **ver todos os dados e itens de uma nota** para **conferir valores e escolher o que devolver**.

## Requisitos funcionais

- RF01 — `GET /api/notas/{id}` no formato completo da seção 5.5 (inclui `protocolo`, `codigoStatus`, `motivoRejeicao`, `xmlPath`, `pdfPath`, `itens`).
- RF02 — `GET /api/notas/{id}/itens` no envelope padrão, com `itemId`, `quantidade` e `quantidadeDevolvida`.
- RF03 — Inexistente → `404`.

## Critérios de aceite

```gherkin
Cenário: itens para devolução
  Dado uma nota autorizada com 2 itens e 1 unidade já devolvida no primeiro
  Quando chamo GET /api/notas/{id}/itens
  Então o primeiro item vem com quantidadeDevolvida 1
```

## Subtasks

- [ ] ST-B07.3.1 Endpoints de detalhe e itens

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-B07.3.1 | API | detalhe, itens e 404 | itens |
| TC-B07.3.2 | Contrato | schemas `Nota` e `NotaItem` do front | RF01/RF02 |
