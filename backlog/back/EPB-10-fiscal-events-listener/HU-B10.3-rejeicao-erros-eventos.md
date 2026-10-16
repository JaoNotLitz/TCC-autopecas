# HU-B10.3 — Rejeição e erros de eventos

**Épico:** [EPB-10](README.md) · **Unidade:** `FiscalEventsListener` · **Estimativa:** 2 pts · **Depende de:** HU-B10.1

## História

Como **administradora**, quero **saber por que um cancelamento ou inutilização foi recusado** para **decidir o que fazer em seguida**.

## Requisitos funcionais

- RF01 — Retorno diferente de 135/155/102: evento `status = rejeitado`, `codigoStatus`, `motivo`; a nota continua `autorizada`.
- RF02 — Erros de comunicação seguem a mesma política da HU-B09.5 (3 tentativas, DLQ, alerta).
- RF03 — `GET /api/notas/{id}` passa a incluir `eventos[]` (tipo, status, protocolo, motivo, registradoEm) para o front mostrar o resultado.

## Critérios de aceite

```gherkin
Cenário: cancelamento rejeitado
  Dado o mock configurado para responder cStat 218 (NF-e já cancelada)
  Então o evento fica rejeitado com o motivo
  E a nota continua autorizada

Cenário: resultado visível
  Então GET /api/notas/{id} mostra o evento rejeitado em eventos[]
```

## Subtasks

- [ ] ST-B10.3.1 Tratamento de rejeição
- [ ] ST-B10.3.2 `eventos[]` no detalhe da nota

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-B10.3.1 | Integração (WireMock admin) | 218 → rejeitado | rejeitado |
| TC-B10.3.2 | API | detalhe com eventos | visível |
