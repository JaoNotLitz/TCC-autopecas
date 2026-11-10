# EPB-10 — FiscalEventsListener

**Unidade entregue:** listener `FiscalEventsListener` do `fiscal-service`, consumindo a `fiscal-events-queue`.
**Tipo:** listener SQS · **Sprint sugerida:** 6 · **Arquitetura:** seções 6.6 e 8.5

## Objetivo

Enviar à SEFAZ os eventos de cancelamento (110111) e os pedidos de inutilização, e aplicar o resultado na nota, no estoque e no registro do evento.

## Dependências de infraestrutura

SQS `fiscal-events-queue` (consumidor, DLQ), `inventory-updates-queue` (produtor) · DynamoDB `notas_fiscais`, `notas_items`, `fiscal_events` · S3 `wc-documents-bucket` · `sefaz-mock` · assinador da HU-B08.3.

## Critérios de aceite do épico

- Cancelamento pedido pela tela termina com nota `cancelada` e estoque devolvido.
- Inutilização termina com protocolo gravado.

## Histórias

| ID | Título | Pts |
|---|---|---|
| [HU-B10.1](HU-B10.1-processar-cancelamento.md) | Processar cancelamento | 5 |
| [HU-B10.2](HU-B10.2-processar-inutilizacao.md) | Processar inutilização | 3 |
| [HU-B10.3](HU-B10.3-rejeicao-erros-eventos.md) | Rejeição e erros de eventos | 2 |
