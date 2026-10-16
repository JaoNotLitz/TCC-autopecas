# EPB-12 — XmlProcessorListener

**Unidade entregue:** listener `XmlProcessorListener` do `entry-service`, consumindo a `entry-events-queue`.
**Tipo:** listener SQS · **Sprint sugerida:** 6 · **Arquitetura:** seções 6.7 e 8.6

## Objetivo

Processar XMLs enviados pela importação assíncrona: ler do S3, extrair os dados, registrar a entrada e somar ao estoque.

## Dependências de infraestrutura

SQS `entry-events-queue` (consumidor, DLQ), `inventory-updates-queue` (produtor) · S3 `wc-xml-storage` · DynamoDB `entry_notes` · HTTP interno para `product-service`.

## Histórias

| ID | Título | Pts |
|---|---|---|
| [HU-B12.1](HU-B12.1-processar-xml.md) | Processar XML e registrar entrada | 5 |
| [HU-B12.2](HU-B12.2-atualizar-estoque.md) | Atualizar estoque dos itens vinculados | 2 |
| [HU-B12.3](HU-B12.3-erros-idempotencia.md) | Erros e idempotência | 3 |
