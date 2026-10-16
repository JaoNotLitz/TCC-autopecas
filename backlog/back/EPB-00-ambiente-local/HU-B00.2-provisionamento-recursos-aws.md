# HU-B00.2 — Provisionar recursos AWS no LocalStack

**Épico:** [EPB-00](README.md) · **Unidade:** `infra/local/init` · **Estimativa:** 5 pts · **Depende de:** HU-B00.1

## História

Como **desenvolvedora**, quero **que tabelas, filas, tópicos, buckets e secrets sejam criados automaticamente** quando o LocalStack sobe, para **que os serviços encontrem tudo pronto**.

## Requisitos funcionais

- RF01 — Scripts em `infra/local/init/ready.d/` (hooks do LocalStack) usando `awslocal`, executados em ordem numérica.
- RF02 — **DynamoDB (11 tabelas)** com chaves, GSIs e streams conforme a seção 3.4 da arquitetura: `users` (email-index), `customers` (documento-index, email-index, status-index), `products` (codigo-index, categoria-index), `inventory` (status-index), `inventory_movements` (produto-data-index), `sales` (cliente-index, status-index, data-index), `sale_items` (PK vendaId, SK itemId), `notas_fiscais` (status-index, venda-index, chave-index, emitidaEm-index), `notas_items` (PK notaId, SK itemId), `fiscal_events` (nota-index, tipo-index), `entry_notes` (fornecedor-index, data-index, chave-index). Modo on-demand, TTL no atributo `ttl`.
- RF03 — Tabela auxiliar `counters` (PK `nome`) para numeração sequencial de vendas e notas.
- RF04 — **SQS**: `customer-events-queue`, `inventory-updates-queue`, `sales-events-queue`, `customer-sales-events-queue`, `fiscal-jobs-queue`, `sefaz-queue`, `fiscal-events-queue`, `entry-events-queue`, `document-events-queue`, `email-queue` e `fiscal-dlq`, com visibility timeout, retenção e `RedrivePolicy` (maxReceiveCount 3 → `fiscal-dlq`) conforme a seção 6.
- RF05 — **SNS**: `low-stock-alerts-topic`, `sales-notifications-topic`, `system-notifications-topic` e `sales-events-topic` (decisão A2), com as assinaturas SQS e as filter policies da seção 7 (ex.: `email-queue` só recebe `status = critico` do tópico de estoque).
- RF06 — **S3**: `wc-xml-storage`, `wc-documents-bucket` (versionado), `wc-reports-bucket`, `wc-backups`, com CORS liberado para `http://localhost:4200` (download pelo front via URL pré-assinada).
- RF07 — **Secrets Manager**: `wc-jwt-secret` (chave aleatória de 64 bytes gerada no init), `wc-redis-auth-token`, `wc-sefaz-certificate` (certificado de teste da HU-B00.4).
- RF08 — **SES**: identidade `noreply@wcautopecas.com.br` verificada.
- RF09 — Scripts idempotentes: rodar duas vezes não gera erro nem duplica recurso.

## Regras e requisitos não funcionais

- Nomes de recursos centralizados em `infra/local/init/resources.env`, lidos também pelos serviços.
- Tempo total do init ≤ 60 s.

## Critérios de aceite

```gherkin
Cenário: recursos criados na subida
  Dado a stack recém-criada com "make reset && make up"
  Quando listo tabelas, filas, tópicos, buckets e secrets com awslocal
  Então encontro exatamente os recursos dos RF02 a RF07

Cenário: DLQ configurada
  Dado a fila "sefaz-queue"
  Quando consulto o atributo RedrivePolicy
  Então ele aponta para "fiscal-dlq" com maxReceiveCount 3

Cenário: filtro de assinatura
  Dado uma mensagem publicada em "low-stock-alerts-topic" com status "baixo"
  Então ela não chega na "email-queue"
  E uma mensagem com status "critico" chega

Cenário: idempotência
  Quando executo os scripts de init duas vezes
  Então a segunda execução termina com código 0 e sem recursos duplicados
```

## Subtasks

- [ ] ST-B00.2.1 Script de tabelas DynamoDB com GSIs e streams
- [ ] ST-B00.2.2 Script de filas SQS com DLQ e redrive
- [ ] ST-B00.2.3 Script de tópicos SNS, assinaturas e filter policies
- [ ] ST-B00.2.4 Script de buckets S3 com versionamento e CORS
- [ ] ST-B00.2.5 Script de secrets e identidade SES
- [ ] ST-B00.2.6 `resources.env` com os nomes
- [ ] ST-B00.2.7 Script `verify.sh` que confere a lista esperada (usado no CI)

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-B00.2.1 | Smoke (`verify.sh`) | lista de recursos bate com a esperada | recursos criados |
| TC-B00.2.2 | Smoke | `get-queue-attributes` da `sefaz-queue` | DLQ |
| TC-B00.2.3 | Integração (Testcontainers) | publica no tópico de estoque com `baixo` e `critico`, confere `email-queue` | filtro |
| TC-B00.2.4 | Smoke | roda o init duas vezes | idempotência |
