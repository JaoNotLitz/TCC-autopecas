# HU-B13.3 — Download do PDF

**Épico:** [EPB-13](README.md) · **Unidade:** `document-service` · **Estimativa:** 2 pts · **Depende de:** HU-B13.1

## História

Como **vendedor**, quero **baixar ou imprimir o DANFE** para **entregar ao cliente**.

## Requisitos funcionais

- RF01 — `GET /api/documentos/pdf/{notaId}` responde `application/pdf` com `filename="DANFE_{numero}.pdf"`; `?inline=true` usa `Content-Disposition: inline` (impressão no navegador).
- RF02 — PDF inexistente e nota autorizada → gera na hora (mesmo gerador da HU-B13.1), salva e devolve.
- RF03 — Nota cancelada → devolve o PDF com carimbo "CANCELADA" sobreposto (gerado em memória, sem gravar).
- RF04 — Nota não autorizada nem cancelada → `422`.

## Critérios de aceite

```gherkin
Cenário: PDF sob demanda
  Dado uma nota autorizada sem pdfPath
  Quando chamo GET /api/documentos/pdf/{id}
  Então recebo o PDF e a nota passa a ter pdfPath

Cenário: nota rejeitada
  Então GET /api/documentos/pdf/{id} responde 422
```

## Subtasks

- [ ] ST-B13.3.1 Endpoint com geração sob demanda
- [ ] ST-B13.3.2 Carimbo de cancelada

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-B13.3.1 | Integração | sob demanda | sob demanda |
| TC-B13.3.2 | Unitário | carimbo presente no texto do PDF | RF03 |
| TC-B13.3.3 | API | rejeitada → 422 | rejeitada |
