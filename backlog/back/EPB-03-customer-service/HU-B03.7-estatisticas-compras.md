# HU-B03.7 — Atualizar estatísticas de compras

**Épico:** [EPB-03](README.md) · **Unidade:** listener `SaleCompletedListener` no `customer-service` · **Estimativa:** 3 pts · **Depende de:** HU-B06.4

## História

Como **vendedor**, quero **ver quantas compras e quanto um cliente já gastou** para **reconhecer clientes frequentes**.

## Requisitos funcionais

- RF01 — Listener na `customer-sales-events-queue` (assinante do `sales-events-topic`, decisão A2) processa `sale.created` com `clienteId` não nulo.
- RF02 — Incrementa `totalCompras` em 1, soma `valorTotal` em `valorTotalCompras` e define `ultimaCompra = timestamp` com `UpdateItem` atômico (`ADD`).
- RF03 — Venda cancelada (`sale.cancelled`, se publicada) não altera estatísticas (fora do escopo; documentado).

## Regras e requisitos não funcionais

- Idempotência por `saleId` (`IdempotencyGuard`, TTL 7 dias): a mesma venda entregue duas vezes conta uma vez.
- Cliente inexistente: log de aviso e ACK (não manda para a DLQ).

## Critérios de aceite

```gherkin
Cenário: venda para cliente identificado
  Dado um cliente com totalCompras 2 e valorTotalCompras 100
  Quando chega sale.created com valorTotal 50 para ele
  Então o cliente fica com totalCompras 3 e valorTotalCompras 150

Cenário: mensagem duplicada
  Quando a mesma mensagem chega duas vezes
  Então as estatísticas mudam uma vez só

Cenário: consumidor final
  Quando chega sale.created com clienteId null
  Então nada é alterado
```

## Subtasks

- [ ] ST-B03.7.1 Listener com `@SqsListener`
- [ ] ST-B03.7.2 Update atômico
- [ ] ST-B03.7.3 Idempotência

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-B03.7.1 | Integração (LocalStack) | publica no tópico e confere o cliente | identificado |
| TC-B03.7.2 | Integração | duplicada | duplicada |
| TC-B03.7.3 | Unitário | `clienteId` null ignorado | consumidor final |
