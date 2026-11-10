# HU-F04.1 — Listar produtos e resumo do estoque

**Épico:** [EPF-04](README.md) · **Estimativa:** 3 pts · **Depende de:** HU-F00.5, HU-B04.2, HU-B04.7

## História

Como **gerente**, quero **ver todos os produtos com a situação do estoque e os totais no topo** para **saber o que precisa de reposição**.

## Requisitos funcionais

- RF01 — Cards do topo com `GET /api/produtos/resumo` (total, baixo, crítico, valor total) no lugar das estatísticas fixas (847, 23, R$ 145.890, 8).
- RF02 — Lista com `GET /api/produtos` paginada; filtros `busca` (nome, código ou NCM), `categoria`, `status` (Todos/OK/Atenção/Baixo/Crítico), `ativo`.
- RF03 — Colunas: código, nome, NCM formatado, categoria, estoque atual, reservado, disponível, mínimo, custo, venda, selo de status (do back, sem recalcular), ações.
- RF04 — Aceita `?status=critico` vindo do início.
- RF05 — Remover `produtosIniciais` e o cálculo de status em `salvarProduto`.

## Critérios de aceite

```gherkin
Cenário: filtro por situação
  Quando escolho "Crítico"
  Então só vejo produtos com selo Crítico, carregados do back

Cenário: estoque reservado
  Dado um produto com 45 em estoque e 5 reservados
  Então vejo atual 45, reservado 5 e disponível 40
```

## Subtasks

- [ ] ST-F04.1.1 Cards do resumo
- [ ] ST-F04.1.2 Lista paginada com filtros
- [ ] ST-F04.1.3 Remover mocks e regra local de status

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-F04.1.1 | Unitário | parâmetros por filtro | filtro |
| TC-F04.1.2 | Unitário | renderização do objeto `estoque` | reservado |
| TC-F04.1.3 | E2E | filtro Crítico com seed | filtro |
