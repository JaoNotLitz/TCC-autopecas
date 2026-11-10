# HU-B15.3 — Alertas de estoque, vendas e sistema

**Épico:** [EPB-15](README.md) · **Unidade:** `notification-service` · **Estimativa:** 3 pts · **Depende de:** HU-B15.1, HU-B05.5

## História

Como **gerente e operação**, quero **receber por e-mail os alertas importantes** para **agir sem ficar olhando o sistema**.

## Requisitos funcionais

- RF01 — As assinaturas SNS → `email-queue` (HU-B00.2) entregam mensagens no envelope SNS; o listener detecta o envelope (`Type: Notification`) e converte `MessageType` em pedido de e-mail:
  - `low_stock_alert` → template `low-stock-alert` para `ALERTA_ESTOQUE_DESTINATARIOS`;
  - `sale_completed` → `sale-confirmation` para `clienteEmail`;
  - `error`/`warning` do sistema → `fiscal-error` para `OPS_DESTINATARIOS` (só `critical` e `high`, pela filter policy).
- RF02 — Destinatários configurados por variável de ambiente.

## Critérios de aceite

```gherkin
Cenário: estoque crítico vira e-mail
  Quando um produto entra em crítico
  Então um e-mail low-stock-alert aparece no SES local para os destinatários configurados

Cenário: alerta de severidade baixa
  Quando o sistema publica um alerta "low"
  Então nenhum e-mail é enviado
```

## Subtasks

- [ ] ST-B15.3.1 Desembrulhar envelope SNS
- [ ] ST-B15.3.2 Mapeamento MessageType → template

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-B15.3.1 | Integração (LocalStack) | publica no tópico de estoque e confere SES | crítico |
| TC-B15.3.2 | Integração | severidade low filtrada | baixa |
