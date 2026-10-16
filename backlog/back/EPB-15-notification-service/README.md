# EPB-15 — notification-service

**Unidade entregue:** microsserviço `notification-service` (porta 8088, só actuator, sem API pública) com os listeners `EmailSenderListener` e `CustomerEventsListener`.
**Tipo:** listeners SQS/SNS · **Sprint sugerida:** 7 · **Arquitetura:** seções 4.9, 6.1, 6.8, 7, 8.7 e 8.8

## Objetivo

Centralizar o envio de e-mails (SES do LocalStack) e transformar eventos e alertas em mensagens para pessoas.

## Dependências de infraestrutura

SQS `email-queue`, `customer-events-queue` (consumidor) · SNS `low-stock-alerts-topic`, `sales-notifications-topic`, `system-notifications-topic` (via assinatura na `email-queue`) · SES.

## Como ver os e-mails localmente

O SES do LocalStack não entrega e-mails; eles ficam em `GET http://localhost:4566/_aws/ses`. O README do serviço explica como abrir.

## Histórias

| ID | Título | Pts |
|---|---|---|
| [HU-B15.1](HU-B15.1-envio-email-ses.md) | Enviar e-mail por template via SES | 5 |
| [HU-B15.2](HU-B15.2-eventos-cliente.md) | Boas-vindas e eventos de cliente | 2 |
| [HU-B15.3](HU-B15.3-alertas-sns.md) | Alertas de estoque, vendas e sistema | 3 |
