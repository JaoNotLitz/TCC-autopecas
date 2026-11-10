# EPB-14 — reports-service

**Unidade entregue:** microsserviço `reports-service` (API REST, porta 8087).
**Tipo:** API · **Sprint sugerida:** 7 · **Arquitetura:** seções 4.8 e 5.8

## Objetivo

Gerar o relatório de notas do Painel Fiscal (`GET /api/notas/relatorio`, usado pelo front) e, como extras, o dashboard agregado e os relatórios de vendas e estoque.

## Dependências de infraestrutura

DynamoDB (leitura de `sales`, `inventory`, `products`, `notas_fiscais`, `customers`) · Redis (cache 5 min) · S3 `wc-reports-bucket`.

> Leitura direta das tabelas de outros serviços é uma exceção consciente ao *database-per-service*, aceita na arquitetura (4.8) por ser só leitura.

## Histórias

| ID | Título | Pts |
|---|---|---|
| [HU-B14.1](HU-B14.1-dashboard.md) | Dashboard agregado (extra) | 5 |
| [HU-B14.2](HU-B14.2-relatorio-vendas.md) | Relatório de vendas (extra) | 3 |
| [HU-B14.3](HU-B14.3-relatorio-estoque.md) | Relatório de estoque (extra) | 2 |
| [HU-B14.4](HU-B14.4-relatorio-fiscal.md) | Relatório de notas (Painel Fiscal) | 2 |
