# EPB-07 — fiscal-service (API e criação de notas)

**Unidade entregue:** microsserviço `fiscal-service` (API REST, porta 8084) com o listener `SalesEventListener`. Os listeners de processamento (XML, SEFAZ, eventos) são os épicos EPB-08, EPB-09 e EPB-10.
**Tipo:** API + listener · **Sprint sugerida:** 4 · **Arquitetura:** seções 4.5, 5.5, 6.3 e 8.2

## Objetivo

Transformar vendas finalizadas em notas fiscais, expor consulta de notas e receber os pedidos de cancelamento, inutilização (`/api/inutilizacoes`), devolução e reenvio.

## Dependências de infraestrutura

DynamoDB `notas_fiscais`, `notas_items`, `fiscal_events`, `counters` · SQS `sales-events-queue` (consumidor), `fiscal-jobs-queue`, `sefaz-queue`, `fiscal-events-queue` (produtor) · HTTP interno para `sales-service`, `customer-service` e `product-service`.

## Critérios de aceite do épico

- Venda finalizada vira nota `processando` com itens e número sequencial por modelo e série.
- Endpoints das seções 5.5 e 5.6 respondem pelo gateway no formato dos tipos do front (`NotaFiscal`, `NotaFiscalItem`, `Inutilizacao`), com listagens em array puro.
- Telas Painel Fiscal, Cancelamento, Inutilização e Devolução do front funcionam contra o serviço.

## Histórias

| ID | Título | Pts |
|---|---|---|
| [HU-B07.1](HU-B07.1-criar-nota-de-venda.md) | Criar nota a partir da venda | 8 |
| [HU-B07.2](HU-B07.2-listar-notas.md) | Listar notas fiscais | 3 |
| [HU-B07.3](HU-B07.3-detalhar-nota-itens.md) | Itens da nota | 2 |
| [HU-B07.4](HU-B07.4-reenviar-nota.md) | Reenviar nota rejeitada (extra) | 2 |
| [HU-B07.5](HU-B07.5-solicitar-cancelamento.md) | Solicitar cancelamento | 3 |
| [HU-B07.6](HU-B07.6-solicitar-inutilizacao.md) | Inutilização de faixa (solicitar e listar) | 3 |
| [HU-B07.7](HU-B07.7-emitir-devolucao.md) | Emitir nota de devolução | 5 |
