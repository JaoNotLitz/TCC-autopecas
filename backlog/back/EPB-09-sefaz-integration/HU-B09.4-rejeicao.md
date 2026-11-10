# HU-B09.4 — Tratar rejeição

**Épico:** [EPB-09](README.md) · **Unidade:** `SefazIntegrationListener` · **Estimativa:** 2 pts · **Depende de:** HU-B09.2

## História

Como **responsável fiscal**, quero **ver o motivo de uma rejeição** para **corrigir o dado e reenviar a nota**.

## Requisitos funcionais

- RF01 — `cStat` 2xx (ou outro diferente de 100/103/105): grava `status = rejeitada`, `codigoStatus` e `motivoRejeicao` (`xMotivo`), e dá ACK.
- RF02 — Publica `fiscal-error` no `system-notifications-topic` (severity `medium`) com número, código e motivo.
- RF03 — A reserva de estoque da venda **não** é liberada automaticamente (a nota pode ser reenviada — HU-B07.4).

## Critérios de aceite

```gherkin
Cenário: rejeição por duplicidade
  Dado uma nota com total terminado em ,04
  Quando o mock responde 204
  Então a nota fica "rejeitada" com codigoStatus "204" e o motivo do mock
  E o estoque reservado continua reservado
```

## Subtasks

- [ ] ST-B09.4.1 Tratamento de 2xx
- [ ] ST-B09.4.2 Alerta

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-B09.4.1 | Integração | 204 → rejeitada | rejeição |
| TC-B09.4.2 | Integração | reserva mantida | RF03 |
