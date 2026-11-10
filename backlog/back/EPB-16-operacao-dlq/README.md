# EPB-16 — Operação: DLQ, reprocessamento e observabilidade

**Unidade entregue:** Lambdas `wc-dlq-monitor` (agendada pelo EventBridge) e `wc-dlq-redrive` (invocada sob demanda), mais o painel de saúde local.
**Tipo:** Lambda · **Sprint sugerida:** 8 · **Arquitetura:** seções 6.9, 7.3 e 13

## Objetivo

Dar visibilidade e ferramenta para o que falhou: alertar quando a DLQ acumula, reprocessar mensagens com segurança e mostrar a saúde da stack num lugar só.

## Dependências de infraestrutura

SQS `fiscal-dlq` e filas de origem · SNS `system-notifications-topic` · EventBridge (regra agendada) · Lambda · CloudWatch Logs do LocalStack.

## Histórias

| ID | Título | Pts |
|---|---|---|
| [HU-B16.1](HU-B16.1-lambda-dlq-monitor.md) | Lambda de monitoramento da DLQ | 3 |
| [HU-B16.2](HU-B16.2-lambda-dlq-redrive.md) | Lambda de reprocessamento da DLQ | 5 |
| [HU-B16.3](HU-B16.3-logs-metricas.md) | Logs estruturados e métricas | 3 |
| [HU-B16.4](HU-B16.4-painel-saude.md) | Painel de saúde da stack | 2 |
