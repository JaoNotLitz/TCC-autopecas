# HU-B04.1 — Cadastrar produto com estoque inicial

**Épico:** [EPB-04](README.md) · **Unidade:** `product-service` · **Estimativa:** 5 pts · **Depende de:** HU-B00.3

## História

Como **gerente de estoque**, quero **cadastrar uma peça com a quantidade que já tenho** para **vendê-la e emitir nota corretamente**.

## Requisitos funcionais

- RF01 — `POST /api/produtos` recebe o `ProdutoInput` do front: `codigo`, `nome`, `ncm`, `estoqueAtual`, `estoqueMinimo`, `precoCusto`, `precoVenda`.
- RF02 — Responde `201` com o `Produto` do front: os mesmos campos + `id` (numérico, `IdGenerator` recurso `produtos`) + `status` calculado (regra da seção 4.3).
- RF03 — Dados fiscais que a tela não tem (`cfop`, `origem`, `cstIcms`, `aliquotaIcms`) recebem os padrões de configuração do serviço (`5102`, `0`, `00`, 18) e ficam só internos.
- RF04 — Na mesma transação (`TransactWriteItems`) grava `products`, `inventory` (`estoqueAtual` do corpo, `reservado = 0`, `status`) e, se `estoqueAtual > 0`, uma movimentação `ajuste` com motivo "Estoque inicial".

## Regras e requisitos não funcionais

- `codigo` único (item-guarda `COD#{codigo}` com `attribute_not_exists`) → `409`.
- Obrigatórios: todos os campos do `ProdutoInput`. NCM 8 dígitos; `precoVenda > precoCusto`; `estoqueMinimo ≥ 0`; `estoqueAtual ≥ 0` → `400` com `campos`.
- Valores monetários como `BigDecimal` com 2 casas internamente; serializados como `number`.

## Critérios de aceite

```gherkin
Cenário: cadastro com estoque inicial
  Quando cadastro "PF-001" com estoqueMinimo 15 e estoqueAtual 30
  Então recebo 201 com id numérico, estoqueAtual 30 e status "ok"
  E existe uma movimentação "ajuste" com motivo "Estoque inicial"

Cenário: sem estoque
  Quando cadastro com estoqueAtual 0 e estoqueMinimo 10
  Então status é "critico"

Cenário: código repetido
  Dado o produto "PF-001"
  Quando cadastro outro "PF-001"
  Então recebo 409

Cenário: preço de venda menor que custo
  Quando envio precoCusto 50 e precoVenda 40
  Então recebo 400 com campos "precoVenda"
```

## Subtasks

- [ ] ST-B04.1.1 Entidades `Produto`, `Inventario`, `Movimentacao` (ids `long`)
- [ ] ST-B04.1.2 `StatusEstoqueCalculator` (regra única, reaproveitada no EPB-05)
- [ ] ST-B04.1.3 `ProdutoService.criar` com transação e padrões fiscais
- [ ] ST-B04.1.4 DTOs, validações e controller
- [ ] ST-B04.1.5 Compose e rota no gateway

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-B04.1.1 | Unitário (parametrizado) | `StatusEstoqueCalculator` nos limites 0,4 / 0,7 / 1,0 do mínimo e com mínimo 0 | status |
| TC-B04.1.2 | Integração (LocalStack) | cria e confere as 3 tabelas | com estoque inicial |
| TC-B04.1.3 | Integração | código repetido → 409 | repetido |
| TC-B04.1.4 | Unitário | validações de NCM e preços | inválidos |
| TC-B04.1.5 | Contrato | resposta valida contra o tipo `Produto` do front (plano, sem campos a mais) | RF02 |
