# HU-B04.7 — Resumo de estoque

**Épico:** [EPB-04](README.md) · **Unidade:** `product-service` · **Estimativa:** 3 pts · **Depende de:** HU-B04.1

## História

Como **gerente**, quero **ver totais do estoque no topo da tela** para **saber quantos produtos estão críticos e quanto vale o estoque**.

## Requisitos funcionais

- RF01 — `GET /api/produtos/resumo` com `{totalProdutos, produtosBaixo, produtosCriticos, valorTotalEstoque, produtosAtivos, produtosInativos}`.
- RF02 — `valorTotalEstoque = Σ estoqueAtual × precoCusto` dos ativos.
- RF03 — Cache Redis de 5 min (`product:resumo`), invalidado em criação, desativação e quando o listener muda status.

## Regras e requisitos não funcionais

- Contagens por status via `status-index` (`Select=COUNT`); valor total via scan paginado com projeção, só quando o cache expirou.

## Critérios de aceite

```gherkin
Cenário: resumo bate com os dados
  Dado o seed com 20 produtos
  Quando chamo GET /api/produtos/resumo
  Então os números batem com uma contagem manual das tabelas

Cenário: cache
  Quando chamo duas vezes em menos de 5 minutos
  Então a segunda vem do cache
```

## Subtasks

- [ ] ST-B04.7.1 Cálculo do resumo
- [ ] ST-B04.7.2 Cache e invalidação

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-B04.7.1 | Integração | números com seed | bate |
| TC-B04.7.2 | Integração | cache e invalidação ao criar produto | cache |
