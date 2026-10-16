# HU-B04.2 — Listar e buscar produtos

**Épico:** [EPB-04](README.md) · **Unidade:** `product-service` · **Estimativa:** 5 pts · **Depende de:** HU-B04.1

## História

Como **vendedor ou gerente**, quero **buscar peças por nome, código ou NCM e filtrar por categoria e situação do estoque** para **achar o item certo e ver o que precisa repor**.

## Requisitos funcionais

- RF01 — `GET /api/produtos` com `busca`, `categoria`, `status`, `ativo`, `limit`, `lastKey`; envelope padrão; cada item com o objeto `estoque {estoqueAtual, reservado, disponivel, status}`.
- RF02 — `status` → query no `status-index` da `inventory` e `BatchGetItem` dos produtos.
- RF03 — `categoria` → query no `categoria-index`.
- RF04 — `busca` igual a um código existente → query no `codigo-index`; 8 dígitos → filtro por NCM; texto → scan com `contains` em `nomeBusca` normalizado.
- RF05 — Junta `products` e `inventory` com `BatchGetItem` (no máximo 100 chaves por chamada).
- RF06 — Cache Redis `product:{id}` (1 h) reaproveitado pela montagem da lista.

## Regras e requisitos não funcionais

- `status` fora de `ok|atencao|baixo|critico` → `400`.
- Página de 20 itens responde em p95 ≤ 400 ms local com 1.000 produtos.

## Critérios de aceite

```gherkin
Cenário: produtos críticos para o card do início
  Dado 3 produtos com status "critico"
  Quando chamo GET /api/produtos?status=critico&limit=5
  Então recebo os 3 com estoque.status "critico"

Cenário: busca por código
  Quando busco "PF-001"
  Então o primeiro item é o produto PF-001

Cenário: estoque aninhado
  Então todo item da lista tem o objeto estoque com estoqueAtual, reservado, disponivel e status
```

## Subtasks

- [ ] ST-B04.2.1 Estratégias de consulta por filtro
- [ ] ST-B04.2.2 Junção products + inventory
- [ ] ST-B04.2.3 Paginação e preenchimento de página

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-B04.2.1 | Integração | filtro por status | críticos |
| TC-B04.2.2 | Integração | busca por código, NCM e nome | busca |
| TC-B04.2.3 | Contrato | item valida contra schema `Produto` do front | aninhado |
| TC-B04.2.4 | Integração | 1.000 produtos, tempo da página | desempenho |
