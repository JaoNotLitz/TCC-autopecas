# HU-B04.4 — Atualizar produto (inclusive a quantidade)

**Épico:** [EPB-04](README.md) · **Unidade:** `product-service` · **Estimativa:** 5 pts · **Depende de:** HU-B04.1, HU-B05.3

## História

Como **gerente**, quero **alterar preço, mínimo e a quantidade de um produto no mesmo formulário** para **manter o catálogo e o estoque corretos**, como a tela de Estoque do front já faz.

## Requisitos funcionais

- RF01 — `PUT /api/produtos/{id}` com o `ProdutoInput` do front; responde `200` com o `Produto` (com `status` recalculado).
- RF02 — Se `estoqueAtual` vier igual ao atual, só atualiza o cadastro.
- RF03 — Se `estoqueAtual` vier diferente: exige papel `ADMIN` (`403` para VENDEDOR), registra uma movimentação `ajuste` com motivo "Ajuste pela tela de estoque" e o `usuarioId` do token, publica `adjust` na `inventory-updates-queue` (mesma regra do HU-B05.3) e responde já com o valor novo.
- RF04 — Se `estoqueMinimo` mudar, recalcula `inventory.status` (e publica alerta se passar a crítico, regra do EPB-05).
- RF05 — Invalida cache.

## Regras e requisitos não funcionais

- `estoqueAtual` menor que o `reservado` → `422` "Quantidade menor que o reservado".
- Mesmas validações do POST.

## Critérios de aceite

```gherkin
Cenário: mudar preço
  Quando altero precoVenda de 89,90 para 99,90
  Então recebo 200 com o preço novo

Cenário: mudar mínimo muda status
  Dado um produto com estoqueAtual 10 e estoqueMinimo 5 (ok)
  Quando altero estoqueMinimo para 30
  Então status vira "critico"

Cenário: admin corrige a quantidade
  Dado um token ADMIN e um produto com estoqueAtual 10
  Quando envio PUT com estoqueAtual 25
  Então recebo 200 com estoqueAtual 25
  E existe uma movimentação "ajuste" com motivo "Ajuste pela tela de estoque"

Cenário: vendedor tenta mudar a quantidade
  Dado um token VENDEDOR
  Quando envio PUT com estoqueAtual diferente
  Então recebo 403
```

## Subtasks

- [ ] ST-B04.4.1 `ProdutoService.atualizar`
- [ ] ST-B04.4.2 Ajuste automático quando muda `estoqueAtual`
- [ ] ST-B04.4.3 Recalcular status ao mudar mínimo

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-B04.4.1 | Integração | preço | preço |
| TC-B04.4.2 | Integração | mínimo e status | mínimo |
| TC-B04.4.3 | Integração (com listener) | quantidade nova gera movimentação e reflete no estoque | admin |
| TC-B04.4.4 | API | VENDEDOR mudando quantidade → 403; mudando só preço → 200 | vendedor |
| TC-B04.4.5 | Unitário | abaixo do reservado → 422 | reservado |
