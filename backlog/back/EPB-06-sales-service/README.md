# EPB-06 — sales-service

**Unidade entregue:** microsserviço `sales-service` (API REST, porta 8083).
**Tipo:** API · **Sprint sugerida:** 4 · **Arquitetura:** seções 4.4 e 5.4

## Objetivo

Registrar vendas e orçamentos do PDV, validar cliente e estoque, reservar os itens e disparar a emissão fiscal.

## Dependências de infraestrutura

DynamoDB `sales`, `sale_items`, `counters` · SQS `inventory-updates-queue` (produtor) · SNS `sales-events-topic` (decisão A2) e `sales-notifications-topic` · HTTP interno para `customer-service` e `product-service` (decisão A4).

## Critérios de aceite do épico

- Uma venda NFC-e finalizada no PDV gera reserva de estoque e uma nota em `processando` no fiscal-service.
- Orçamentos podem ser finalizados ou cancelados depois.
- Erros `409` (estoque) e `422` (endereço para NF-e) chegam ao front com `campos` úteis.

## Histórias

| ID | Título | Pts |
|---|---|---|
| [HU-B06.1](HU-B06.1-criar-venda.md) | Criar venda ou orçamento | 5 |
| [HU-B06.2](HU-B06.2-validar-cliente-nfe.md) | Validar cliente para NF-e | 3 |
| [HU-B06.3](HU-B06.3-validar-estoque.md) | Validar estoque disponível | 3 |
| [HU-B06.4](HU-B06.4-eventos-venda.md) | Reservar estoque e disparar emissão | 3 |
| [HU-B06.5](HU-B06.5-listar-detalhar-vendas.md) | Listar e detalhar vendas | 3 |
| [HU-B06.6](HU-B06.6-alterar-status-cancelar.md) | Finalizar ou cancelar orçamento | 3 |
| [HU-B06.7](HU-B06.7-vincular-nota-notificar.md) | Vincular nota à venda e notificar | 2 |
