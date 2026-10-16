# EPB-05 — InventoryUpdateListener

**Unidade entregue:** listener `InventoryUpdateListener` do `product-service`, consumindo a `inventory-updates-queue` (pode rodar no mesmo container da API ou num container separado com o perfil `worker`).
**Tipo:** listener SQS · **Sprint sugerida:** 3 · **Arquitetura:** seções 4.3, 6.2 e 8.1

## Objetivo

Ser o **único ponto que altera quantidades de estoque**. Recebe eventos de vendas, entradas, notas e ajustes, aplica a operação de forma atômica, recalcula o status, registra a movimentação e dispara alerta de estoque crítico.

## Dependências de infraestrutura

SQS `inventory-updates-queue` (consumidor, DLQ `fiscal-dlq`) · DynamoDB `products`, `inventory`, `inventory_movements` · SNS `low-stock-alerts-topic` · Redis (invalidação de cache e idempotência).

## Critérios de aceite do épico

- Todos os tipos (`reserve`, `release`, `decrease`, `increase`, `adjust`) aplicados de forma atômica e idempotente.
- Mensagens inválidas vão para a `fiscal-dlq` após 3 tentativas.
- Concorrência de 10 mensagens simultâneas no mesmo produto não perde atualização.

## Histórias

| ID | Título | Pts |
|---|---|---|
| [HU-B05.1](HU-B05.1-reserva-liberacao.md) | Reservar e liberar estoque | 3 |
| [HU-B05.2](HU-B05.2-baixa-entrada.md) | Baixar e dar entrada no estoque | 3 |
| [HU-B05.3](HU-B05.3-ajuste.md) | Aplicar ajuste absoluto | 2 |
| [HU-B05.4](HU-B05.4-status-movimentacao.md) | Recalcular status e registrar movimentação | 3 |
| [HU-B05.5](HU-B05.5-alerta-estoque-critico.md) | Alerta de estoque crítico | 2 |
| [HU-B05.6](HU-B05.6-concorrencia-idempotencia-dlq.md) | Concorrência, idempotência e DLQ | 5 |
