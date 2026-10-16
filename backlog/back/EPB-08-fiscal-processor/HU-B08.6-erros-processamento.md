# HU-B08.6 — Tratar erros do processamento

**Épico:** [EPB-08](README.md) · **Unidade:** `FiscalProcessorListener` · **Estimativa:** 3 pts · **Depende de:** HU-B08.3

## História

Como **operação**, quero **que falhas na geração do XML fiquem visíveis e não travem a fila** para **corrigir a causa e reprocessar**.

## Requisitos funcionais

- RF01 — Erros **definitivos** (certificado expirado, falha de assinatura, nota inexistente): log `ERROR` com `notaId` e motivo, publica `fiscal-error` no `system-notifications-topic` (severity `critical` para certificado) e deixa a mensagem ir para a `fiscal-dlq`.
- RF02 — Erros **transitórios** (S3, DynamoDB, Secrets Manager indisponíveis): exceção para a mensagem voltar à fila, até 3 recebimentos.
- RF03 — Erros de **dados** (XSD): nota `rejeitada` com motivo e ACK (HU-B08.4), sem DLQ.

## Critérios de aceite

```gherkin
Cenário: certificado expirado
  Então a mensagem termina na fiscal-dlq
  E chega uma notificação critical no system-notifications-topic

Cenário: S3 indisponível uma vez
  Dado o S3 falhando só na primeira tentativa
  Então a nota é processada na segunda entrega
```

## Subtasks

- [ ] ST-B08.6.1 Hierarquia de exceções (definitiva, transitória, dados)
- [ ] ST-B08.6.2 Publicação de alerta

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-B08.6.1 | Integração | certificado expirado → DLQ + alerta | expirado |
| TC-B08.6.2 | Integração (falha injetada) | S3 falha uma vez | transitório |
