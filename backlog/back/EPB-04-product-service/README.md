# EPB-04 — product-service (API)

**Unidade entregue:** microsserviço `product-service` (API REST, porta 8082). O listener de estoque do mesmo serviço é o épico [EPB-05](../EPB-05-inventory-listener/README.md).
**Tipo:** API · **Sprint sugerida:** 3 · **Arquitetura:** seções 4.3 e 5.3

## Objetivo

Catálogo de produtos com dados fiscais e consulta de estoque. Toda alteração de quantidade passa por evento na `inventory-updates-queue`; a API nunca grava `estoqueAtual` direto, exceto o estoque inicial na criação.

## Dependências de infraestrutura

DynamoDB `products` (GSIs `codigo-index`, `categoria-index`), `inventory` (`status-index`), `inventory_movements` (`produto-data-index`) · SQS `inventory-updates-queue` (produtor) · Redis (cache).

## Critérios de aceite do épico

- Endpoints da seção 5.3 pelo gateway, sempre com o objeto `estoque` aninhado.
- Tela de Estoque (EPF-04) e busca de produtos do PDV (HU-F05.1) funcionam contra o serviço.

## Histórias

| ID | Título | Pts |
|---|---|---|
| [HU-B04.1](HU-B04.1-cadastrar-produto.md) | Cadastrar produto com estoque inicial | 5 |
| [HU-B04.2](HU-B04.2-listar-buscar-produtos.md) | Listar e buscar produtos | 5 |
| [HU-B04.3](HU-B04.3-detalhar-produto.md) | Detalhar produto | 2 |
| [HU-B04.4](HU-B04.4-atualizar-produto.md) | Atualizar produto | 3 |
| [HU-B04.5](HU-B04.5-desativar-produto.md) | Desativar produto | 2 |
| [HU-B04.6](HU-B04.6-ajuste-manual.md) | Ajuste manual de estoque | 3 |
| [HU-B04.7](HU-B04.7-resumo-estoque.md) | Resumo de estoque | 3 |
| [HU-B04.8](HU-B04.8-historico-movimentacoes.md) | Histórico de movimentações | 3 |
