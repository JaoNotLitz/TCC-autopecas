# HU-B14.4 — Relatório fiscal

**Épico:** [EPB-14](README.md) · **Unidade:** `reports-service` · **Estimativa:** 2 pts · **Depende de:** HU-B14.2

## História

Como **responsável fiscal**, quero **exportar as notas de um período** para **conferir com o contador**.

## Requisitos funcionais

- RF01 — `GET /api/relatorios/fiscal?de&ate&tipo&status&formato`.
- RF02 — Colunas: `Numero, Serie, Modelo, Tipo, Chave, Emitida_Em, Autorizada_Em, Cliente, Valor_Total, Valor_ICMS, Status, Protocolo, Motivo_Rejeicao`.

## Critérios de aceite

```gherkin
Cenário: notas canceladas
  Quando peço status=cancelada no período
  Então recebo só as notas canceladas
```

## Subtasks

- [ ] ST-B14.4.1 Endpoint fiscal

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-B14.4.1 | Integração | filtros tipo e status | canceladas |
