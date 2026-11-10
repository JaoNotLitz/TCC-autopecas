# HU-B16.1 — Lambda de monitoramento da DLQ

**Épico:** [EPB-16](README.md) · **Unidade:** Lambda `wc-dlq-monitor` · **Estimativa:** 3 pts · **Depende de:** HU-B00.2

## História

Como **operação**, quero **ser avisada quando mensagens começam a se acumular na DLQ** para **investigar antes de virar um problema para a loja**.

## Requisitos funcionais

- RF01 — Lambda Java 21 disparada por regra do EventBridge a cada 1 min (`rate(1 minute)`).
- RF02 — Lê `ApproximateNumberOfMessagesVisible` da `fiscal-dlq`; acima de 5 (configurável), publica no `system-notifications-topic` `{MessageType: "error", service: "fiscal-dlq", severity: "high", message, details:{quantidade, amostra:[origem, motivo]}}`.
- RF03 — Não repete o alerta enquanto a quantidade não mudar de faixa (guarda o último valor alertado num parâmetro/tabela `counters`).

## Regras e requisitos não funcionais

- Substitui o CloudWatch Alarm da seção 6.9, que o LocalStack gratuito não dispara para SNS de forma confiável.

## Critérios de aceite

```gherkin
Cenário: DLQ acima do limite
  Dado 6 mensagens na fiscal-dlq
  Quando a regra dispara
  Então um alerta high é publicado uma vez

Cenário: abaixo do limite
  Dado 2 mensagens
  Então nenhum alerta é publicado
```

## Subtasks

- [ ] ST-B16.1.1 Handler da Lambda
- [ ] ST-B16.1.2 Regra EventBridge e deploy no init

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-B16.1.1 | Unitário | limites e deduplicação do alerta | acima / abaixo |
| TC-B16.1.2 | Integração (LocalStack) | invoca a Lambda com DLQ cheia e lê fila assinante | acima |
