# HU-B04.1 — Cadastrar produto com estoque inicial

**Épico:** [EPB-04](README.md) · **Unidade:** `product-service` · **Estimativa:** 5 pts · **Depende de:** HU-B00.3

## História

Como **gerente de estoque**, quero **cadastrar uma peça com seus dados fiscais e a quantidade que já tenho** para **vendê-la e emitir nota corretamente**.

## Requisitos funcionais

- RF01 — `POST /api/produtos` com `codigo`, `nome`, `descricao`, `ncm`, `cest`, `categoria`, `fabricante`, `codigoFabricante`, `precoCusto`, `precoVenda`, `estoqueMinimo`, `cfop`, `origem`, `cstIcms`, `aliquotaIcms` e `estoqueInicial` (opcional).
- RF02 — Responde `201` no formato de `GET /api/produtos/{id}`, com `margemLucro = (precoVenda − precoCusto) / precoCusto × 100` (2 casas).
- RF03 — Na mesma transação (`TransactWriteItems`) grava `products`, `inventory` (`estoqueAtual = estoqueInicial` ou 0, `reservado = 0`, `disponivel`, `status` pela regra da seção 4.3) e, se `estoqueInicial > 0`, uma movimentação `ajuste` com motivo "Estoque inicial".

## Regras e requisitos não funcionais

- `codigo` único (item-guarda `COD#{codigo}` com `attribute_not_exists`) → `409`.
- NCM 8 dígitos; CEST 7 dígitos quando informado; CFOP 4 dígitos; `origem` 0–8; `precoVenda > precoCusto`; `estoqueMinimo ≥ 0`; `estoqueInicial ≥ 0`; `aliquotaIcms` entre 0 e 100 → `400` com `campos`.
- Obrigatórios: `codigo`, `nome`, `ncm`, `categoria`, `precoCusto`, `precoVenda`, `estoqueMinimo`, `cfop`, `origem`, `cstIcms`.
- Valores monetários como `BigDecimal` com 2 casas.

## Critérios de aceite

```gherkin
Cenário: cadastro com estoque inicial
  Quando cadastro "PF-001" com estoqueMinimo 15 e estoqueInicial 30
  Então recebo 201 com estoque.estoqueAtual 30, estoque.status "ok" e margemLucro calculada
  E existe uma movimentação "ajuste" com motivo "Estoque inicial"

Cenário: sem estoque inicial
  Quando cadastro sem estoqueInicial e com estoqueMinimo 10
  Então estoque.estoqueAtual é 0 e estoque.status é "critico"

Cenário: código repetido
  Dado o produto "PF-001"
  Quando cadastro outro "PF-001"
  Então recebo 409 com campos[0].campo = "codigo"

Cenário: preço de venda menor que custo
  Quando envio precoCusto 50 e precoVenda 40
  Então recebo 400 com campos "precoVenda"
```

## Subtasks

- [ ] ST-B04.1.1 Entidades `Produto`, `Inventario`, `Movimentacao`
- [ ] ST-B04.1.2 `StatusEstoqueCalculator` (regra única, reaproveitada no EPB-05)
- [ ] ST-B04.1.3 `ProdutoService.criar` com transação
- [ ] ST-B04.1.4 DTOs, validações e controller
- [ ] ST-B04.1.5 Compose e rota no gateway

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-B04.1.1 | Unitário (parametrizado) | `StatusEstoqueCalculator` nos limites 0,4 / 0,7 / 1,0 do mínimo e com mínimo 0 | status |
| TC-B04.1.2 | Unitário | margem de lucro e arredondamento | RF02 |
| TC-B04.1.3 | Integração (LocalStack) | cria e confere as 3 tabelas | com estoque inicial |
| TC-B04.1.4 | Integração | código repetido → 409 | repetido |
| TC-B04.1.5 | Unitário | validações de NCM, CFOP, preços | inválidos |
