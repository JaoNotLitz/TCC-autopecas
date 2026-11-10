# EPB-04 — product-service (API)

**Unidade entregue:** microsserviço `product-service` (API REST, porta 8082). O listener de estoque do mesmo serviço é o épico [EPB-05](../EPB-05-inventory-listener/README.md).
**Tipo:** API · **Sprint sugerida:** 3 · **Arquitetura:** seções 4.3 e 5.3

## Objetivo

Catálogo de produtos e consulta de estoque, no formato do tipo `Produto` do front (plano, com `estoqueAtual` e `status`). Toda alteração de quantidade gera movimentação e evento na `inventory-updates-queue`, inclusive a que vem do `PUT` da tela de Estoque (o `ProdutoInput` do front traz `estoqueAtual`).

## Dependências de infraestrutura

DynamoDB `products` (GSI `codigo-index`), `inventory` (`status-index`), `inventory_movements` (`produto-data-index`) · SQS `inventory-updates-queue` (produtor) · Redis (cache).

## Critérios de aceite do épico

- Endpoints da seção 5.3 pelo gateway, no formato do tipo `Produto` do front (id numérico, plano, listagem em array).
- Tela de Estoque e busca de produtos do PDV do front funcionam contra o serviço.

## Histórias

| ID | Título | Pts |
|---|---|---|
| [HU-B04.1](HU-B04.1-cadastrar-produto.md) | Cadastrar produto com estoque inicial | 5 |
| [HU-B04.2](HU-B04.2-listar-buscar-produtos.md) | Listar e buscar produtos | 3 |
| [HU-B04.3](HU-B04.3-detalhar-produto.md) | Detalhar produto (extra, uso interno) | 2 |
| [HU-B04.4](HU-B04.4-atualizar-produto.md) | Atualizar produto (inclusive a quantidade) | 5 |
| [HU-B04.5](HU-B04.5-desativar-produto.md) | Desativar produto | 2 |
| [HU-B04.6](HU-B04.6-ajuste-manual.md) | Ajuste manual de estoque com motivo (extra) | 3 |
| [HU-B04.7](HU-B04.7-resumo-estoque.md) | Resumo de estoque | 3 |
| [HU-B04.8](HU-B04.8-historico-movimentacoes.md) | Histórico de movimentações (extra) | 3 |
