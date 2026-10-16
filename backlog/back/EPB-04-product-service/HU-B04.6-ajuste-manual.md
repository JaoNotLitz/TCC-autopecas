# HU-B04.6 — Ajuste manual de estoque

**Épico:** [EPB-04](README.md) · **Unidade:** `product-service` · **Estimativa:** 3 pts · **Depende de:** HU-B04.1, HU-B05.3

## História

Como **gerente**, quero **corrigir a quantidade em estoque depois de um inventário físico, informando o motivo** para **o sistema bater com a prateleira e a correção ficar registrada**.

## Requisitos funcionais

- RF01 — `POST /api/produtos/{id}/ajuste` (ADMIN) com `{quantidadeNova, motivo}`.
- RF02 — Publica `{type: "adjust", produtoId, quantidade: quantidadeNova, referenciaType: "adjustment", usuarioId, motivo}` na `inventory-updates-queue`.
- RF03 — Responde `202` com `{produtoId, quantidadeNova, status: "processando"}`. *A arquitetura cita `200`; como o efeito é assíncrono, o backlog adota `202` (o front trata os dois).*

## Regras e requisitos não funcionais

- `motivo` obrigatório com no mínimo 10 caracteres após `trim`; `quantidadeNova ≥ 0` inteiro → `400`.
- `quantidadeNova < reservado` → `422` "Quantidade menor que o reservado".

## Critérios de aceite

```gherkin
Cenário: ajuste válido
  Dado um produto com estoqueAtual 40
  Quando um ADMIN ajusta para 35 com motivo "Inventário físico de outubro"
  Então recebo 202
  E em até 5 segundos o produto mostra estoqueAtual 35 e uma movimentação "ajuste" com o motivo

Cenário: motivo curto
  Quando envio motivo "erro"
  Então recebo 400 com campos "motivo"

Cenário: abaixo do reservado
  Dado reservado 5
  Quando ajusto para 3
  Então recebo 422
```

## Subtasks

- [ ] ST-B04.6.1 Endpoint e validações
- [ ] ST-B04.6.2 Publicação do evento `adjust`

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-B04.6.1 | Integração (com listener) | ajuste reflete no estoque e na movimentação | válido |
| TC-B04.6.2 | Unitário | motivo curto, quantidade negativa | motivo curto |
| TC-B04.6.3 | Unitário | abaixo do reservado → 422 | reservado |
