# HU-B06.4 — Reservar estoque e disparar emissão

**Épico:** [EPB-06](README.md) · **Unidade:** `sales-service` · **Estimativa:** 3 pts · **Depende de:** HU-B06.1, HU-B05.1

## História

Como **vendedor**, quero **que ao finalizar a venda o estoque seja reservado e a nota comece a ser emitida sozinha** para **não ter que fazer isso em outra tela**.

## Requisitos funcionais

- RF01 — Após gravar a venda (qualquer status), publica um `reserve` por item na `inventory-updates-queue` com `referenciaId = vendaId`, `referenciaType = sale`, `usuarioId`.
- RF02 — Se `status = finalizada`, publica no `sales-events-topic` (decisão A2): `{eventType: "sale.created", saleId, tipoDocumento, clienteId, valorTotal, timestamp}` com atributo `eventType`.
- RF03 — Publicação com outbox simples (mesmo padrão da HU-B03.6): falha no SNS/SQS não perde o evento.

## Critérios de aceite

```gherkin
Cenário: venda finalizada
  Quando crio uma venda finalizada com 2 itens
  Então 2 mensagens reserve chegam na inventory-updates-queue
  E 1 mensagem sale.created chega na sales-events-queue e na customer-sales-events-queue

Cenário: orçamento
  Quando crio um orçamento
  Então só as mensagens reserve são publicadas
```

## Subtasks

- [ ] ST-B06.4.1 `VendaEventPublisher`
- [ ] ST-B06.4.2 Outbox e job de reenvio

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-B06.4.1 | Integração (LocalStack) | lê as 3 filas após venda finalizada | finalizada |
| TC-B06.4.2 | Integração | orçamento não gera sale.created | orçamento |
| TC-B06.4.3 | Contrato | mensagem valida contra o schema `sale-event` usado pelo fiscal | RF02 |
