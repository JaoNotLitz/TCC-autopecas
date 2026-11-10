# HU-B04.2 — Listar e buscar produtos

**Épico:** [EPB-04](README.md) · **Unidade:** `product-service` · **Estimativa:** 3 pts · **Depende de:** HU-B04.1

## História

Como **vendedor ou gerente**, quero **buscar peças por nome, código ou NCM e filtrar pela situação do estoque** para **achar o item certo e ver o que precisa repor**.

## Requisitos funcionais

- RF01 — `GET /api/produtos` com os filtros do front (`busca`, `status`), opcionais; responde `Produto[]` (array puro, plano: `estoqueAtual` e `status` no próprio item), só produtos ativos, em ordem alfabética.
- RF02 — `status` → query no `status-index` da `inventory` e `BatchGetItem` dos produtos.
- RF03 — `busca` igual a um código existente → query no `codigo-index`; 8 dígitos → filtro por NCM; texto → filtro `contains` em `nomeBusca` normalizado.
- RF04 — Junta `products` e `inventory` com `BatchGetItem` (no máximo 100 chaves por chamada).
- RF05 — Cache Redis `product:{id}` (1 h) reaproveitado pela montagem da lista.

## Regras e requisitos não funcionais

- `status` fora de `ok|atencao|baixo|critico` → `400`.
- Lista de 1.000 produtos responde em p95 ≤ 800 ms local.

## Critérios de aceite

```gherkin
Cenário: produtos críticos para o card do início
  Dado 3 produtos com status "critico"
  Quando chamo GET /api/produtos?status=critico
  Então recebo um array com os 3, cada um com status "critico"

Cenário: busca por código
  Quando busco "PF-001"
  Então o primeiro item é o produto PF-001

Cenário: formato plano
  Então todo item tem id numérico, estoqueAtual e status no próprio objeto, sem objeto estoque aninhado
```

## Subtasks

- [ ] ST-B04.2.1 Estratégias de consulta por filtro
- [ ] ST-B04.2.2 Junção products + inventory no formato plano

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-B04.2.1 | Integração | filtro por status | críticos |
| TC-B04.2.2 | Integração | busca por código, NCM e nome | busca |
| TC-B04.2.3 | Contrato | item valida contra o tipo `Produto` do front | formato plano |
| TC-B04.2.4 | Integração | 1.000 produtos, tempo da resposta | desempenho |
