# EPB-03 — customer-service

**Unidade entregue:** microsserviço `customer-service` (API REST, porta 8081) com o listener `SaleCompletedListener`.
**Tipo:** API + listener · **Sprint sugerida:** 2 · **Arquitetura:** seções 4.2, 5.2 e 6.1

## Objetivo

CRUD de clientes PF/PJ com validação de documento, unicidade de documento e e-mail, busca para o PDV e eventos de mudança.

## Dependências de infraestrutura

DynamoDB `customers` (GSIs `documento-index`, `email-index`, `status-index`) · SQS `customer-events-queue` (produtor) e `customer-sales-events-queue` (consumidor, decisão A2) · Redis (cache).

## Critérios de aceite do épico

- Endpoints da seção 5.2 funcionando pelo gateway, com o envelope de paginação.
- Tela de Clientes (EPF-03) e busca do PDV (HU-F05.3) funcionam contra este serviço.
- Eventos `customer.*` chegam na fila e são consumidos pelo notification-service (EPB-15).

## Histórias

| ID | Título | Pts |
|---|---|---|
| [HU-B03.1](HU-B03.1-cadastrar-cliente.md) | Cadastrar cliente | 5 |
| [HU-B03.2](HU-B03.2-listar-buscar-clientes.md) | Listar e buscar clientes | 5 |
| [HU-B03.3](HU-B03.3-detalhar-cliente.md) | Detalhar cliente com cache | 2 |
| [HU-B03.4](HU-B03.4-atualizar-cliente.md) | Atualizar cliente | 3 |
| [HU-B03.5](HU-B03.5-desativar-cliente.md) | Desativar cliente | 2 |
| [HU-B03.6](HU-B03.6-eventos-cliente.md) | Publicar eventos de cliente | 2 |
| [HU-B03.7](HU-B03.7-estatisticas-compras.md) | Atualizar estatísticas de compras | 3 |
